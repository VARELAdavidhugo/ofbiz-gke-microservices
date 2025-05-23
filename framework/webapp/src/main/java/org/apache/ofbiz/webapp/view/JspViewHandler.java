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
package org.apache.ofbiz.webapp.view;

import java.io.IOException;

import java.util.Map;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.jsp.JspException;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.webapp.control.ConfigXMLReader;
import org.apache.ofbiz.webapp.control.ControlFilter;

/**
 * JspViewHandler - Java Server Pages View Handler
 */
public class JspViewHandler extends AbstractViewHandler {

    private static final String MODULE = JspViewHandler.class.getName();
    private ServletContext servletContext;

    @Override
    public void init(ServletContext servletContext) throws ViewHandlerException {
        this.servletContext = servletContext;
    }

    @Override
    public Map<String, Object> prepareViewContext(HttpServletRequest request, HttpServletResponse response, ConfigXMLReader.ViewMap viewMap) {
        return Map.of();
    }

    @Override
    public void render(String name, String page, String contentType, String encoding, String info, HttpServletRequest request, HttpServletResponse
            response, Map<String, Object> context) throws ViewHandlerException {
        // some containers call filters on EVERY request, even forwarded ones,
        // so let it know that it came from the control servlet

        if (request == null) {
            throw new ViewHandlerException("Null HttpServletRequest object");
        }
        if (UtilValidate.isEmpty(page)) {
            throw new ViewHandlerException("Null or empty source");
        }

        //Debug.logInfo("Requested Page : " + page, MODULE);
        //Debug.logInfo("Physical Path  : " + context.getRealPath(page));

        // tell the ControlFilter we are forwarding
        request.setAttribute(ControlFilter.FORWARDED_FROM_SERVLET, Boolean.TRUE);
        RequestDispatcher rd = request.getRequestDispatcher(page);

        if (rd == null) {
            Debug.logInfo("HttpServletRequest.getRequestDispatcher() failed; trying ServletContext", MODULE);
            rd = this.servletContext.getRequestDispatcher(page);
            if (rd == null) {
                Debug.logInfo("ServletContext.getRequestDispatcher() failed; trying ServletContext.getNamedDispatcher(\"jsp\")", MODULE);
                rd = this.servletContext.getNamedDispatcher("jsp");
                if (rd == null) {
                    throw new ViewHandlerException("Source returned a null dispatcher (" + page + ")");
                }
            }
        }

        try {
            rd.include(request, response);
        } catch (IOException ie) {
            throw new ViewHandlerException("IO Error in view", ie);
        } catch (ServletException e) {
            Throwable throwable = e.getRootCause() != null ? e.getRootCause() : e;

            if (throwable instanceof JspException) {
                JspException jspe = (JspException) throwable;

                throwable = jspe.getCause() != null ? jspe.getCause() : jspe;
            }
            Debug.logError(throwable, "ServletException rendering JSP view", MODULE);
            throw new ViewHandlerException(e.getMessage(), throwable);
        }
    }
}
