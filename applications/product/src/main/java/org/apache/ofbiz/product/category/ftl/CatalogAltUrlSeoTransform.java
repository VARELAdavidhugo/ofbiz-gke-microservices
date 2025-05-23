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
package org.apache.ofbiz.product.category.ftl;

import java.io.IOException;
import java.io.Writer;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.template.FreeMarkerWorker;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.product.category.CatalogUrlFilter;
import org.apache.ofbiz.product.category.CategoryContentWrapper;
import org.apache.ofbiz.product.product.ProductContentWrapper;
import org.apache.ofbiz.security.CsrfUtil;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.webapp.OfbizUrlBuilder;
import org.apache.ofbiz.webapp.control.WebAppConfigurationException;
import org.apache.ofbiz.webapp.SeoConfigUtil;

import freemarker.core.Environment;
import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans.NumberModel;
import freemarker.ext.beans.GenericObjectModel;
import freemarker.template.SimpleNumber;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTransformModel;

public class CatalogAltUrlSeoTransform implements TemplateTransformModel {
    private static final String MODULE = CatalogUrlSeoTransform.class.getName();

    /**
     * Gets string arg.
     * @param args the args
     * @param key the key
     * @return the string arg
     */
    public String getStringArg(Map<?, ?> args, String key) {
        Object o = args.get(key);
        if (o instanceof SimpleScalar) {
            return ((SimpleScalar) o).getAsString();
        } else if (o instanceof GenericObjectModel) {
            return ((GenericObjectModel) o).getAsString();
        } else if (o instanceof SimpleNumber) {
            return ((SimpleNumber) o).getAsNumber().toString();
        } else if (o instanceof NumberModel) {
            return ((NumberModel) o).getAsNumber().toString();
        }
        return null;
    }

    /**
     * Check arg boolean.
     * @param args the args
     * @param key the key
     * @param defaultValue the default value
     * @return the boolean
     */
    public boolean checkArg(Map<?, ?> args, String key, boolean defaultValue) {
        if (!args.containsKey(key)) {
            return defaultValue;
        }
        Object o = args.get(key);
        if (o instanceof SimpleScalar) {
            SimpleScalar s = (SimpleScalar) o;
            return "true".equalsIgnoreCase(s.getAsString());
        }
        return defaultValue;
    }

    @Override
    public Writer getWriter(Writer out, @SuppressWarnings("rawtypes") Map args)
            throws TemplateModelException, IOException {
        final StringBuilder buf = new StringBuilder();
        final boolean fullPath = checkArg(args, "fullPath", false);
        final boolean secure = checkArg(args, "secure", false);

        return new Writer(out) {

            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                buf.append(cbuf, off, len);
            }

            @Override
            public void flush() throws IOException {
                out.flush();
            }

            @Override
            public void close() throws IOException {
                try {
                    Environment env = Environment.getCurrentEnvironment();
                    BeanModel req = (BeanModel) env.getVariable("request");
                    String previousCategoryId = getStringArg(args, "previousCategoryId");
                    String productCategoryId = getStringArg(args, "productCategoryId");
                    String productId = getStringArg(args, "productId");
                    String url = "";

                    Object prefix = env.getVariable("urlPrefix");
                    String viewSize = getStringArg(args, "viewSize");
                    String viewIndex = getStringArg(args, "viewIndex");
                    String viewSort = getStringArg(args, "viewSort");
                    String searchString = getStringArg(args, "searchString");
                    if (req != null) {
                        HttpServletRequest request = (HttpServletRequest) req.getWrappedObject();
                        StringBuilder newURL = new StringBuilder();
                        if (UtilValidate.isNotEmpty(productId)) {
                            if (SeoConfigUtil.isCategoryUrlEnabled(request.getContextPath())) {
                                url = CatalogUrlSeoTransform.makeProductUrl(request, productId, productCategoryId,
                                        previousCategoryId);
                            } else {
                                url = CatalogUrlFilter.makeProductUrl(request, previousCategoryId, productCategoryId,
                                        productId);
                            }
                        } else {
                            if (SeoConfigUtil.isCategoryUrlEnabled(request.getContextPath())) {
                                url = CatalogUrlSeoTransform.makeCategoryUrl(request, productCategoryId,
                                        previousCategoryId, viewSize, viewIndex, viewSort, searchString);
                            } else {
                                url = CatalogUrlFilter.makeCategoryUrl(request, previousCategoryId, productCategoryId,
                                        productId, viewSize, viewIndex, viewSort, searchString);
                            }
                        }

                        // add / update csrf token to link when required
                        String tokenValue = CsrfUtil.generateTokenForNonAjax(request, "product");
                        url = CsrfUtil.addOrUpdateTokenInUrl(url, tokenValue);

                        // make the link
                        if (fullPath) {
                            try {
                                OfbizUrlBuilder builder = OfbizUrlBuilder.from(request);
                                builder.buildHostPart(newURL, "", secure);
                            } catch (WebAppConfigurationException e) {
                                Debug.logError(e.getMessage(), MODULE);
                            }
                        }
                        newURL.append(url);
                        out.write(newURL.toString());
                    } else if (prefix != null) {
                        Delegator delegator = FreeMarkerWorker.getWrappedObject("delegator", env);
                        LocalDispatcher dispatcher = FreeMarkerWorker.getWrappedObject("dispatcher", env);
                        Locale locale = (Locale) args.get("locale");
                        String prefixString = ((GenericObjectModel) prefix).getAsString();
                        prefixString = prefixString.replace("&#47;", "/");
                        String contextPath = prefixString;
                        int lastSlashIndex = prefixString.lastIndexOf('/');
                        if (lastSlashIndex > -1 && lastSlashIndex < prefixString.length()) {
                            contextPath = prefixString.substring(prefixString.lastIndexOf('/'));
                        }
                        if (UtilValidate.isNotEmpty(productId)) {
                            GenericValue product = EntityQuery.use(delegator).from("Product")
                                    .where("productId", productId).queryOne();
                            ProductContentWrapper wrapper = new ProductContentWrapper(dispatcher, product, locale,
                                    EntityUtilProperties.getPropertyValue("content", "defaultMimeType",
                                            "text/html; charset=utf-8", delegator));
                            if (SeoConfigUtil.isCategoryUrlEnabled(contextPath)) {
                                url = CatalogUrlSeoTransform.makeProductUrl(delegator, wrapper, prefixString,
                                        contextPath, productCategoryId, previousCategoryId, productId);
                            } else {
                                url = CatalogUrlFilter.makeProductUrl(wrapper, null, prefixString, previousCategoryId,
                                        productCategoryId, productId);
                            }
                        } else {
                            GenericValue productCategory = EntityQuery.use(delegator).from("ProductCategory")
                                    .where("productCategoryId", productCategoryId).queryOne();
                            CategoryContentWrapper wrapper = new CategoryContentWrapper(dispatcher, productCategory,
                                    locale, EntityUtilProperties.getPropertyValue("content", "defaultMimeType",
                                            "text/html; charset=utf-8", delegator));
                            if (SeoConfigUtil.isCategoryUrlEnabled(contextPath)) {
                                url = CatalogUrlSeoTransform.makeCategoryUrl(delegator, wrapper, prefixString,
                                        productCategoryId, previousCategoryId, productId, viewSize, viewIndex, viewSort,
                                        searchString);
                            } else {
                                url = CatalogUrlFilter.makeCategoryUrl(delegator, wrapper, null, prefixString,
                                        previousCategoryId, productCategoryId, productId, viewSize, viewIndex, viewSort,
                                        searchString);
                            }
                        }
                        out.write(url);
                    } else {
                        out.write(buf.toString());
                    }
                } catch (TemplateModelException | GenericEntityException e) {
                    throw new IOException(e.getMessage());
                }
            }
        };
    }
}
