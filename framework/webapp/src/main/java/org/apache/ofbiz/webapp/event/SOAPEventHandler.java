/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.apache.ofbiz.webapp.event;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.wsdl.WSDLException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPModelBuilder;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.engine.SoapSerializer;
import org.apache.ofbiz.webapp.control.ConfigXMLReader.Event;
import org.apache.ofbiz.webapp.control.ConfigXMLReader.RequestMap;
import org.apache.ofbiz.webapp.control.RequestHandler;
import org.w3c.dom.Document;

/**
 * SOAPEventHandler - SOAP Event Handler implementation
 */
public class SOAPEventHandler implements EventHandler {

    private static final String MODULE = SOAPEventHandler.class.getName();

    @Override
    public void init(ServletContext context) throws EventHandlerException {
    }

    @Override
    public String invoke(Event event, RequestMap requestMap, HttpServletRequest request, HttpServletResponse response) throws EventHandlerException {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        // first check for WSDL request
        String wsdlReq = request.getParameter("wsdl");
        if (wsdlReq == null) {
            wsdlReq = request.getParameter("WSDL");
        }
        if (wsdlReq != null) {
            String serviceName = RequestHandler.getOverrideViewUri(request.getPathInfo());
            DispatchContext dctx = dispatcher.getDispatchContext();
            String locationUri = getLocationURI(request);

            if (serviceName != null) {
                Document wsdl = null;
                try {
                    wsdl = dctx.getWSDL(serviceName, locationUri);
                } catch (GenericServiceException e) {
                    serviceName = null;
                } catch (WSDLException e) {
                    sendError(response, "Unable to obtain WSDL", serviceName);
                    throw new EventHandlerException("Unable to obtain WSDL", e);
                }

                if (wsdl != null) {
                    try (OutputStream os = response.getOutputStream()) {
                        response.setContentType("text/xml");
                        UtilXml.writeXmlDocument(os, wsdl);
                        response.flushBuffer();
                    } catch (IOException e) {
                        throw new EventHandlerException(e);
                    }
                    return null;
                } else {
                    sendError(response, "Unable to obtain WSDL", serviceName);
                    throw new EventHandlerException("Unable to obtain WSDL");
                }
            } else {
                try (Writer writer = response.getWriter()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("<html><head><title>OFBiz SOAP/1.1 Services</title></head>");
                    sb.append("<body>No such service.").append("<p>Services:<ul>");

                    for (String scvName: dctx.getAllServiceNames()) {
                        ModelService model = dctx.getModelService(scvName);
                        if (model.isExport()) {
                            sb.append("<li><a href=\"").append(locationUri).append("/").append(model.getName()).append("?wsdl\">");
                            sb.append(model.getName()).append("</a></li>");
                        }
                    }
                    sb.append("</ul></p></body></html>");

                    writer.write(sb.toString());
                    writer.flush();
                    return null;
                } catch (Exception e) {
                    sendError(response, "Unable to obtain WSDL", null);
                    throw new EventHandlerException("Unable to obtain WSDL");
                }
            }
        }

        // not a wsdl request; invoke the service
        response.setContentType("text/xml");

        // request envelope
        SOAPEnvelope reqEnv = null;

        // get the service name and parameters
        try {
            InputStream inputStream = request.getInputStream();
            SOAPModelBuilder builder = OMXMLBuilderFactory.createSOAPModelBuilder(inputStream, "UTF-8");
            reqEnv = (SOAPEnvelope) builder.getDocumentElement();

            // log the request message
            if (Debug.verboseOn()) {
                try {
                    Debug.logInfo("Request Message:\n" + reqEnv + "\n", MODULE);
                } catch (Throwable t) {
                }
            }
        } catch (Exception e) {
            sendError(response, "Problem processing the service", null);
            throw new EventHandlerException("Cannot get the envelope", e);
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose("[Processing]: SOAP Event", MODULE);
        }

        String serviceName = null;
        try {
            SOAPBody reqBody = reqEnv.getBody();
            validateSOAPBody(reqBody);
            OMElement serviceElement = reqBody.getFirstElement();
            serviceName = serviceElement.getLocalName();
            Map<String, Object> parameters = UtilGenerics.cast(SoapSerializer.deserialize(serviceElement.toString(), delegator));
            try {
                // verify the service is exported for remote execution and invoke it
                ModelService model = dispatcher.getDispatchContext().getModelService(serviceName);

                if (model == null) {
                    sendError(response, "Problem processing the service", serviceName);
                    Debug.logError("Could not find Service [" + serviceName + "].", MODULE);
                    return null;
                }

                if (!model.isExport()) {
                    sendError(response, "Problem processing the service", serviceName);
                    Debug.logError("Trying to call Service [" + serviceName + "] that is not exported.", MODULE);
                    return null;
                }

                Map<String, Object> serviceResults = dispatcher.runSync(serviceName, parameters);
                if (Debug.verboseOn()) {
                    Debug.logVerbose("[EventHandler] : Service invoked", MODULE);
                }

                createAndSendSOAPResponse(serviceResults, serviceName, response);

            } catch (GenericServiceException e) {
                Debug.logError(e, MODULE);
                if (UtilProperties.getPropertyAsBoolean("service", "secureSoapAnswer", true)) {
                    sendError(response, "Problem processing the service, check your parameters.", serviceName);
                } else {
                    if (e.getMessageList() == null) {
                        sendError(response, e.getMessage(), serviceName);
                    } else {
                        sendError(response, e.getMessageList(), serviceName);
                    }
                    return null;
                }
            }
        } catch (Exception e) {
            sendError(response, e.getMessage(), serviceName);
            Debug.logError(e, MODULE);
            return null;
        }

        return null;
    }

    private static void validateSOAPBody(SOAPBody reqBody) throws EventHandlerException {
        // ensure the SOAPBody contains only one service call request
        Integer numServiceCallRequests = 0;
        Iterator<Object> serviceIter = UtilGenerics.cast(reqBody.getChildElements());
        while (serviceIter.hasNext()) {
            numServiceCallRequests++;
            serviceIter.next();
        }
        if (numServiceCallRequests != 1) {
            throw new EventHandlerException("One service call expected, but received: " + numServiceCallRequests.toString());
        }
    }

    private static void createAndSendSOAPResponse(Map<String, Object> serviceResults, String serviceName,
            HttpServletResponse response) throws EventHandlerException {
        try {
        // setup the response
            if (Debug.verboseOn()) {
                Debug.logVerbose("[EventHandler] : Setting up response message", MODULE);
            }
            String xmlResults = SoapSerializer.serialize(serviceResults);
            //Debug.logInfo("xmlResults ==================" + xmlResults, MODULE);
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(xmlResults));
            OMXMLParserWrapper resultsBuilder = OMXMLBuilderFactory.createStAXOMBuilder(OMAbstractFactory.getOMFactory(),
                    reader);
            OMElement resultSer = resultsBuilder.getDocumentElement();

            // create the response soap
            SOAPFactory factory = OMAbstractFactory.getSOAP11Factory();
            SOAPEnvelope resEnv = factory.createSOAPEnvelope();
            SOAPBody resBody = factory.createSOAPBody();
            OMElement resService = factory.createOMElement(new QName(serviceName + "Response"));
            resService.addChild(resultSer.getFirstElement());
            resBody.addChild(resService);
            resEnv.addChild(resBody);

            // The declareDefaultNamespace method doesn't work see (https://issues.apache.org/jira/browse/AXIS2-3156)
            // so the following doesn't work:
            // resService.declareDefaultNamespace(ModelService.TNS);
            // instead, create the xmlns attribute directly:
            OMAttribute defaultNS = factory.createOMAttribute("xmlns", null, ModelService.TNS);
            resService.addAttribute(defaultNS);

            // log the response message
            if (Debug.verboseOn()) {
                try {
                    Debug.logInfo("Response Message:\n" + resEnv + "\n", MODULE);
                } catch (Throwable t) {
                }
            }

            resEnv.serialize(response.getOutputStream(), false);
            response.getOutputStream().flush();
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            throw new EventHandlerException(e.getMessage(), e);
        }
    }

    private static void sendError(HttpServletResponse res, String errorMessage, String serviceName)
            throws EventHandlerException {
        // setup the response
        sendError(res, ServiceUtil.returnError(errorMessage), serviceName);
    }

    private static void sendError(HttpServletResponse res, List<String> errorMessages, String serviceName)
            throws EventHandlerException {
        sendError(res, ServiceUtil.returnError(errorMessages.toString()), serviceName);
    }
    private static void sendError(HttpServletResponse res, Object object, String serviceName)
            throws EventHandlerException {
        try {
            // setup the response
            res.setContentType("text/xml");
            String xmlResults = SoapSerializer.serialize(object);
            XMLStreamReader xmlReader = XMLInputFactory.newInstance().createXMLStreamReader(new StringReader(xmlResults));
            OMXMLParserWrapper resultsBuilder = OMXMLBuilderFactory.createStAXOMBuilder(OMAbstractFactory.getOMFactory(), xmlReader);
            OMElement resultSer = resultsBuilder.getDocumentElement();

            // create the response soap
            SOAPFactory factory = OMAbstractFactory.getSOAP11Factory();
            SOAPEnvelope resEnv = factory.createSOAPEnvelope();
            SOAPBody resBody = factory.createSOAPBody();
            OMElement errMsg = factory.createOMElement(new QName((serviceName != null ? serviceName : "") + "Response"));
            errMsg.addChild(resultSer.getFirstElement());
            resBody.addChild(errMsg);
            resEnv.addChild(resBody);

            // The declareDefaultNamespace method doesn't work see (https://issues.apache.org/jira/browse/AXIS2-3156)
            // so the following doesn't work:
            // resService.declareDefaultNamespace(ModelService.TNS);
            // instead, create the xmlns attribute directly:
            OMAttribute defaultNS = factory.createOMAttribute("xmlns", null, ModelService.TNS);
            errMsg.addAttribute(defaultNS);

            // log the response message
            if (Debug.verboseOn()) {
                try {
                    Debug.logInfo("Response Message:\n" + resEnv + "\n", MODULE);
                } catch (Throwable t) {
                }
            }

            resEnv.serialize(res.getOutputStream(), false);
            res.getOutputStream().flush();
        } catch (Exception e) {
            throw new EventHandlerException(e.getMessage(), e);
        }
    }

    private static String getLocationURI(HttpServletRequest request) {
        StringBuilder uri = new StringBuilder();
        uri.append(request.getScheme());
        uri.append("://");
        uri.append(request.getServerName());
        if (request.getServerPort() != 80 && request.getServerPort() != 443) {
            uri.append(":");
            uri.append(request.getServerPort());
        }
        uri.append(request.getContextPath());
        uri.append(request.getServletPath());

        String reqInfo = RequestHandler.getRequestUri(request.getPathInfo());
        if (!reqInfo.startsWith("/")) {
            reqInfo = "/" + reqInfo;
        }

        uri.append(reqInfo);
        return uri.toString();
    }
}
