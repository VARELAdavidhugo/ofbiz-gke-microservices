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
package org.apache.ofbiz.widget.renderer.html;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.widget.model.MenuFactory;
import org.apache.ofbiz.widget.model.ModelMenu;
import org.apache.ofbiz.widget.renderer.MenuStringRenderer;
import org.xml.sax.SAXException;


/**
 * Widget Library - HTML Menu Wrapper class - makes it easy to do the setup and render of a menu
 */
public class HtmlMenuWrapper {

    private static final String MODULE = HtmlMenuWrapper.class.getName();

    private String resourceName;
    private String menuName;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private ModelMenu modelMenu;
    private MenuStringRenderer renderer;
    private Map<String, Object> context;

    protected HtmlMenuWrapper() { }

    public HtmlMenuWrapper(String resourceName, String menuName, HttpServletRequest request, HttpServletResponse response)
            throws IOException, SAXException, ParserConfigurationException {
        init(resourceName, menuName, request, response);
    }

    /**
     * Init.
     * @param resourceName the resource name
     * @param menuName the menu name
     * @param request the request
     * @param response the response
     * @throws IOException the io exception
     * @throws SAXException the sax exception
     * @throws ParserConfigurationException the parser configuration exception
     */
    public void init(String resourceName, String menuName, HttpServletRequest request, HttpServletResponse response)
            throws IOException, SAXException, ParserConfigurationException {
        this.resourceName = resourceName;
        this.menuName = menuName;
        this.request = request;
        this.response = response;

        this.modelMenu = MenuFactory.getMenuFromWebappContext(resourceName, menuName, request);

        this.renderer = getMenuRenderer();

        this.context = new HashMap<>();
        Map<String, Object> parameterMap = UtilHttp.getParameterMap(request);
        context.put("parameters", parameterMap);

        HttpSession session = request.getSession();
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        context.put("userLogin", userLogin);

        //make sure the locale is in the context
        context.put("locale", UtilHttp.getLocale(request));

        // if there was an error message, this is an error
        if (UtilValidate.isNotEmpty(request.getAttribute("_ERROR_MESSAGE_"))) {
            context.put("isError", Boolean.TRUE);
        } else {
            context.put("isError", Boolean.FALSE);
        }

        // if a parameter was passed saying this is an error, it is an error
        if ("true".equals(parameterMap.get("isError"))) {
            context.put("isError", Boolean.TRUE);
        }
    }

    /**
     * Gets menu renderer.
     * @return the menu renderer
     */
    public MenuStringRenderer getMenuRenderer() {
        return new HtmlMenuRenderer(request, response);
    }

    /**
     * Render menu string string.
     * @return the string
     * @throws IOException the io exception
     */
    public String renderMenuString() throws IOException {
        HttpServletRequest req = ((HtmlMenuRenderer) renderer).getRequest();
        if (req.getServletContext() == null) {
            if (Debug.infoOn()) {
                Debug.logInfo("in renderMenuString, ctx is null(0)", "");
            }
        }

        Writer writer = new StringWriter();
        modelMenu.renderMenuString(writer, context, renderer);

        HttpServletRequest req2 = ((HtmlMenuRenderer) renderer).getRequest();
        if (req2.getServletContext() == null) {
            if (Debug.infoOn()) {
                Debug.logInfo("in renderMenuString, ctx is null(2)", "");
            }
        }

        return writer.toString();
    }

    /**
     * Tells the menu library whether this is a response to an error or not.
     * Defaults on initialization according to the presense of an errorMessage
     * in the request or if an isError parameter was passed to the page with
     * the value "true". If true then the prefilled values will come from the
     * parameters Map instead of the value Map.
     */
    public void setIsError(boolean isError) {
        this.context.put("isError", isError);
    }

    /**
     * Gets is error.
     * @return the is error
     */
    public boolean getIsError() {
        Boolean isErrorBoolean = (Boolean) this.context.get("isError");
        if (isErrorBoolean == null) {
            return false;
        } else {
            return isErrorBoolean;
        }
    }

    /**
     * Sets menu override name.
     * @param menuName the menu name
     */
    public void setMenuOverrideName(String menuName) {
        this.context.put("menuName", menuName);
    }

    /**
     * Put in context.
     * @param name  the name
     * @param value the value
     */
    public void putInContext(String name, Object value) {
        this.context.put(name, value);
    }

    /**
     * Put in context.
     * @param menuItemName the menu item name
     * @param valueName    the value name
     * @param value        the value
     */
    public void putInContext(String menuItemName, String valueName, Object value) {
        Object obj = context.get(menuItemName);
        Map<String, Object> valueMap = (obj instanceof Map) ? UtilGenerics.cast(obj) : null;
        if (valueMap == null) {
            valueMap = new HashMap<>();
            context.put(menuItemName, valueMap);
        }
        valueMap.put(valueName, value);
    }

    /**
     * Gets from context.
     * @param name the name
     * @return the from context
     */
    public Object getFromContext(String name) {
        return this.context.get(name);
    }

    /**
     * Gets from context.
     * @param menuItemName the menu item name
     * @param valueName    the value name
     * @return the from context
     */
    public Object getFromContext(String menuItemName, String valueName) {
        Object obj = context.get(menuItemName);
        Map<String, Object> valueMap = (obj instanceof Map) ? UtilGenerics.cast(obj) : null;
        if (valueMap == null) {
            valueMap = new HashMap<>();
            context.put(menuItemName, valueMap);
        }
        return valueMap.get(valueName);
    }

    /**
     * Gets model menu.
     * @return the model menu
     */
    public ModelMenu getModelMenu() {
        return modelMenu;
    }

    /**
     * Gets renderer.
     * @return the renderer
     */
    public MenuStringRenderer getRenderer() {
        return renderer;
    }

    /**
     * Sets renderer.
     * @param renderer the renderer
     */
    public void setRenderer(MenuStringRenderer renderer) {
        this.renderer = renderer;
    }

    /**
     * Sets request.
     * @param request the request
     */
    public void setRequest(HttpServletRequest request) {
        this.request = request;
        // CHECKSTYLE_OFF: ALMOST_ALL
        ((HtmlMenuRenderer) renderer).setRequest(request);
        // CHECKSTYLE_ON: ALMOST_ALL
    }

    /**
     * Sets response.
     * @param response the response
     */
    public void setResponse(HttpServletResponse response) {
        this.response = response;
        // CHECKSTYLE_OFF: ALMOST_ALL
        ((HtmlMenuRenderer) renderer).setResponse(response);
        // CHECKSTYLE_ON: ALMOST_ALL
    }

    /**
     * Gets request.
     * @return the request
     */
    public HttpServletRequest getRequest() {
        return ((HtmlMenuRenderer) renderer).getRequest();
    }

    /**
     * Gets response.
     * @return the response
     */
    public HttpServletResponse getResponse() {
        return ((HtmlMenuRenderer) renderer).getResponse();
    }

    public static HtmlMenuWrapper getMenuWrapper(HttpServletRequest request, HttpServletResponse response, HttpSession session,
                                                 String menuDefFile, String menuName, String menuWrapperClassName) {

        HtmlMenuWrapper menuWrapper = null;

        String menuSig = menuDefFile + "__" + menuName;
        if (session != null) {
            menuWrapper = (HtmlMenuWrapper) session.getAttribute(menuSig);
        }

        if (menuWrapper == null) {
            try {
                Class<?> cls = Class.forName("org.apache.ofbiz.widget.html." + menuWrapperClassName);
                menuWrapper = (HtmlMenuWrapper) cls.getDeclaredConstructor().newInstance();
                menuWrapper.init(menuDefFile, menuName, request, response);
            } catch (InstantiationException | IllegalAccessException | IOException | SAXException | ParserConfigurationException e) {
                throw new RuntimeException(e.getMessage());
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Class not found:" + e.getMessage());
            }
        } else {
            menuWrapper.setRequest(request);
            menuWrapper.setResponse(response);
            Map<String, Object> parameterMap = UtilHttp.getParameterMap(request);
            menuWrapper.setParameters(parameterMap);

            GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
            menuWrapper.putInContext("userLogin", userLogin);

        }

        if (session != null) {
            session.setAttribute(menuSig, menuWrapper);
        }
        return menuWrapper;
    }

    /**
     * Sets parameters.
     * @param paramMap the param map
     */
    public void setParameters(Map<String, Object> paramMap) {
        context.put("parameters", paramMap);
    }

}
