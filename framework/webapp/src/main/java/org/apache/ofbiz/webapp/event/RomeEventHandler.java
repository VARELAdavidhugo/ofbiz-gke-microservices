/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

package org.apache.ofbiz.webapp.event;

import java.io.IOException;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.ofbiz.webapp.control.ConfigXMLReader.Event;
import org.apache.ofbiz.webapp.control.ConfigXMLReader.RequestMap;
import org.apache.ofbiz.webapp.control.RequestHandler;

import com.rometools.rome.feed.WireFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.WireFeedOutput;


/**
 * RomeEventHandler
 */
public class RomeEventHandler implements EventHandler {

    private static final String MODULE = RomeEventHandler.class.getName();
    private static final String MIME = "application/xml; charset=UTF-8";
    private static final String DEFAULT_FEED_TYPE = "rss_2.0";

    private EventHandler service;
    private WireFeedOutput out;

    @Override
    public void init(ServletContext context) throws EventHandlerException {
        // get the service event handler
        this.service = new ServiceEventHandler();
        this.service.init(context);
        this.out = new WireFeedOutput();
    }

    @Override
    public String invoke(Event event, RequestMap requestMap, HttpServletRequest request, HttpServletResponse response) throws EventHandlerException {
        RequestHandler handler = (RequestHandler) request.getSession().getServletContext().getAttribute("_REQUEST_HANDLER_");
        if (handler == null) {
            throw new EventHandlerException("No request handler found in servlet context!");
        }
        // generate the main and entry links
        String entryLinkReq = request.getParameter("entryLinkReq");
        String mainLinkReq = request.getParameter("mainLinkReq");

        // create the links; but the query string must be created by the service
        String entryLink = handler.makeLink(request, response, entryLinkReq, true, false, false);
        String mainLink = handler.makeLink(request, response, mainLinkReq, true, false, false);
        request.setAttribute("entryLink", entryLink);
        request.setAttribute("mainLink", mainLink);

        String feedType = request.getParameter("feedType");
        if (feedType == null) {
            request.setAttribute("feedType", DEFAULT_FEED_TYPE);
        }

        // invoke the feed generator service (implements rssFeedInterface)
        String respCode = service.invoke(event, requestMap, request, response);

        // pull out the RSS feed from the request attributes
        WireFeed wireFeed = (WireFeed) request.getAttribute("wireFeed");
        response.setContentType(MIME);
        try {
            out.output(wireFeed, response.getWriter());
        } catch (IOException e) {
            throw new EventHandlerException("Unable to get response writer", e);
        } catch (FeedException e) {
            throw new EventHandlerException("Unable to write RSS feed", e);
        }

        return respCode;
    }
}
