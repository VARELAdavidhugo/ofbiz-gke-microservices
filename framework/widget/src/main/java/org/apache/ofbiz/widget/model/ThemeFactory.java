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
package org.apache.ofbiz.widget.model;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.ofbiz.base.location.FlexibleLocation;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.FileUtil;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.widget.renderer.VisualTheme;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;


/**
 * Widget Theme Library - Theme factory class
 */
public final class ThemeFactory {

    private static final String MODULE = ThemeFactory.class.getName();

    private static final UtilCache<String, ModelTheme> THEME_LOCATION_CACHE =
            UtilCache.createUtilCache("widget.theme.locationResource", 0, 0, false);
    private static final UtilCache<String, VisualTheme> THEME_VISUAL_THEME_ID_CACHE =
            UtilCache.createUtilCache("widget.theme.idAndVisualTheme", 0, 0, false);

    private ThemeFactory() { }
    /**
     * From a w3c Document return the modelTheme instantiated
     * @param themeFileDoc
     * @return
     */
    private static ModelTheme readThemeDocument(Document themeFileDoc) {
        if (themeFileDoc != null) {
            // read document and construct ModelScreen for each screen element
            ModelTheme modelTheme = new ModelTheme(themeFileDoc.getDocumentElement());
            return modelTheme;
        }
        return null;
    }

    /**
     * Reload all VisualTheme in cache
     */
    private static void pullModelThemesFromXmlToCache() {
        try {
            List<File> xmlThemes = getThemeXmlFiles();
            for (File xmlTheme : xmlThemes) {
                ModelTheme modelTheme = getModelThemeFromLocation(xmlTheme.toURI().toURL().toString());
                if (modelTheme != null) {
                    for (String containsVisualThemeId : modelTheme.getVisualThemeIds()) {
                        THEME_VISUAL_THEME_ID_CACHE.put(containsVisualThemeId, modelTheme.getVisualTheme(containsVisualThemeId));
                    }
                }
            }
        } catch (IOException e) {
            Debug.logError("Impossible to initialize models themes in cache throw: " + e, MODULE);
        }
    }

    /**
     * Helper method for getThemeXmlFiles
     * @return
     * @throws IOException
     */
    private static List<File> checkForWidgetFolder(String folderPath) throws IOException {
        File folder = new File(folderPath);
        List<File> xmlThemes = new ArrayList<File>();
        if (folder.exists() && folder.isDirectory()) {
            File[] subFolders = folder.listFiles(File::isDirectory);

            if (subFolders != null) {
                for (File subFolder : subFolders) {
                    File widgetFolder = new File(subFolder, "widget");
                    if (widgetFolder.exists() && widgetFolder.isDirectory()) {
                        List<File> xmlPluginThemes = FileUtil.findXmlFiles(widgetFolder.getPath(), null, "theme", "widget-theme.xsd");
                        if (UtilValidate.isNotEmpty(xmlPluginThemes)) {
                            xmlThemes.addAll(xmlPluginThemes);
                        }
                    }
                }
            }
        }
        return xmlThemes;
    }

    /**
     * Scan all Theme.xml definition
     * @return
     * @throws IOException
     */
    public static List<File> getThemeXmlFiles() throws IOException {
        String ofbizHome = System.getProperty("ofbiz.home");
        String themeFolderPath = ofbizHome + "/themes";
        String pluginsFolderPath = ofbizHome + "/plugins";

        List<File> xmlThemes = checkForWidgetFolder(themeFolderPath);
        xmlThemes.addAll(checkForWidgetFolder(pluginsFolderPath));

        return xmlThemes;
    }

    /**
     * From a visualThemeId return the VisualTheme object corresponding in cache
     * If it's empty, reload the cache from all Theme definition
     * @param visualThemeId
     * @return
     */
    public static VisualTheme getVisualThemeFromId(String visualThemeId) {
        if (visualThemeId == null) {
            return null;
        }
        VisualTheme visualTheme = THEME_VISUAL_THEME_ID_CACHE.get(visualThemeId);
        if (visualTheme == null) {
            synchronized (ThemeFactory.class) {
                visualTheme = THEME_VISUAL_THEME_ID_CACHE.get(visualThemeId);
                if (visualTheme == null) {
                    pullModelThemesFromXmlToCache();
                }
                visualTheme = THEME_VISUAL_THEME_ID_CACHE.get(visualThemeId);
                if (visualTheme == null) {
                    Debug.logError("Impossible to resolve the modelTheme for the visualThemeId " + visualThemeId + ", Common is returned", MODULE);
                    return THEME_VISUAL_THEME_ID_CACHE.get("COMMON");
                }

            }
        }
        return visualTheme;
    }

    /**
     * From a theme file location, resolve the modelTheme related from the cache.
     * If empty, load the modeTheme definition and put it in cache
     * @param resourceName
     * @return
     */
    public static ModelTheme getModelThemeFromLocation(String resourceName) {
        ModelTheme modelTheme = THEME_LOCATION_CACHE.get(resourceName);
        if (modelTheme == null) {
            synchronized (ThemeFactory.class) {
                try {
                    modelTheme = THEME_LOCATION_CACHE.get(resourceName);
                    if (modelTheme == null) {
                        URL themeFileUrl = null;
                        themeFileUrl = FlexibleLocation.resolveLocation(resourceName);
                        if (themeFileUrl == null) {
                            throw new IllegalArgumentException("Could not resolve location to URL: " + resourceName);
                        }
                        Document themeFileDoc = UtilXml.readXmlDocument(themeFileUrl, true, true);
                        modelTheme = readThemeDocument(themeFileDoc);
                        THEME_LOCATION_CACHE.put(resourceName, modelTheme);
                    }
                } catch (IOException | ParserConfigurationException | SAXException e) {
                    Debug.logError("Impossible to resolve the theme from the resourceName " + resourceName, MODULE);
                }
            }
        }
        return modelTheme;
    }

    /**
     * Return all visual theme available corresponding to all entries on the entity VisualTheme who have linked to a modelTheme
     * @param delegator
     * @param visualThemeSetId
     * @return
     * @throws GenericEntityException
     */
    public static List<VisualTheme> getAvailableThemes(Delegator delegator, String visualThemeSetId) throws GenericEntityException {
        if (THEME_VISUAL_THEME_ID_CACHE.isEmpty()) {
            synchronized (ThemeFactory.class) {
                if (THEME_VISUAL_THEME_ID_CACHE.isEmpty()) {
                    pullModelThemesFromXmlToCache();
                }
            }
        }
        LinkedHashMap<String, VisualTheme> visualThemesMap = new LinkedHashMap<>();
        List<GenericValue> visualThemesInDataBase = delegator.findList("VisualTheme",
                EntityCondition.makeCondition("visualThemeSetId", visualThemeSetId), null, UtilMisc.toList("visualThemeId"), null, true);
        List<String> visualThemeIds = EntityUtil.getFieldListFromEntityList(visualThemesInDataBase, "visualThemeId", true);
        for (String visualThemeId : visualThemeIds) {
            visualThemesMap.put(visualThemeId, THEME_VISUAL_THEME_ID_CACHE.get(visualThemeId));
        }
        return new ArrayList<>(visualThemesMap.values());
    }

    /**
     * Resolve the enabled VisualTheme with this find order
     * If a user is logged
     * 1. Check if present en session with key "visualTheme"
     * 2. Check from user preference
     * If user isn't logged or visualTheme not find with logged user
     * 3. Check if visualThemeId has been set on the webapp attribute
     * 4. Check the general.properties VISUAL_THEME
     * 5. return COMMON
     * @param request
     * @return
     */
    public static VisualTheme resolveVisualTheme(HttpServletRequest request) {
        String visualThemeId = null;
        if (request != null) {
            HttpSession session = request.getSession();
            GenericValue userLogin = session != null
                    ? (GenericValue) session.getAttribute("userLogin")
                    : null;
            //search on request only if a userLogin is present on session (otherwise this implied that the user isn't identify so wait
            if (userLogin != null) {
                VisualTheme visualTheme = (VisualTheme) session.getAttribute("visualTheme");
                if (visualTheme != null) {
                    return visualTheme;
                }

                //resolve on user pref
                LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
                if (dispatcher != null) {
                    try {
                        Map<String, Object> userPreferencesResult = dispatcher.runSync("getUserPreference",
                                UtilMisc.toMap("userLogin", userLogin, "userPrefTypeId", "VISUAL_THEME"));
                        visualThemeId = (String) userPreferencesResult.get("userPrefValue");
                    } catch (GenericServiceException e) {
                        Debug.logError("Impossible to resolve the theme from user prefrence for " + userLogin.get("userLoginId"), MODULE);
                    }
                }
            }

            //resolve from webapp
            if (visualThemeId == null && session != null) {
                ServletContext servletContext = request.getServletContext();
                visualThemeId = servletContext.getInitParameter("visualThemeId");
            }
        }

        //resolve from general properties
        if (visualThemeId == null) {
            visualThemeId = UtilProperties.getPropertyValue("general", "VISUAL_THEME", "COMMON");
        }
        return getVisualThemeFromId(visualThemeId);
    }
}
