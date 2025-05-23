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
package org.apache.ofbiz.product.product;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.content.content.ContentWorker;
import org.apache.ofbiz.content.content.ContentWrapper;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.service.LocalDispatcher;

/**
 * Product Content Worker: gets product content to display
 */
public class ProductContentWrapper implements ContentWrapper {

    private static final String MODULE = ProductContentWrapper.class.getName();

    private static final UtilCache<String, String> PRODUCT_CONTENT_CACHE = UtilCache.createUtilCache("product.content.rendered", true);

    public static ProductContentWrapper makeProductContentWrapper(GenericValue product, HttpServletRequest request) {
        return new ProductContentWrapper(product, request);
    }

    private LocalDispatcher dispatcher;
    private GenericValue product;
    private Locale locale;
    private String mimeTypeId;

    public ProductContentWrapper(LocalDispatcher dispatcher, GenericValue product, Locale locale, String mimeTypeId) {
        this.dispatcher = dispatcher;
        this.product = product;
        this.locale = locale;
        this.mimeTypeId = mimeTypeId;
    }

    public ProductContentWrapper(GenericValue product, HttpServletRequest request) {
        this.dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        this.product = product;
        this.locale = UtilHttp.getLocale(request);
        this.mimeTypeId = ContentWrapper.getDefaultMimeTypeId((Delegator) request.getAttribute("delegator"));
    }

    @Override
    public StringUtil.StringWrapper get(String productContentTypeId, String encoderType) {
        if (this.product == null) {
            Debug.logWarning("Tried to get ProductContent for type [" + productContentTypeId
                    + "] but the product field in the ProductContentWrapper is null", MODULE);
            return null;
        }
        return StringUtil.makeStringWrapper(getProductContentAsText(this.product, productContentTypeId, locale, mimeTypeId, null,
                null, this.product.getDelegator(), dispatcher, encoderType));
    }

    public static String getProductContentAsText(GenericValue product, String productContentTypeId, HttpServletRequest request, String encoderType) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        String mimeTypeId = ContentWrapper.getDefaultMimeTypeId(product.getDelegator());
        return getProductContentAsText(product, productContentTypeId, UtilHttp.getLocale(request), mimeTypeId, null, null,
                product.getDelegator(), dispatcher, encoderType);
    }

    public static String getProductContentAsText(GenericValue product, String productContentTypeId, Locale locale, LocalDispatcher dispatcher,
                                                 String encoderType) {
        return getProductContentAsText(product, productContentTypeId, locale, null, null, null, null, dispatcher, encoderType);
    }

    public static String getProductContentAsText(GenericValue product, String productContentTypeId, Locale locale,
            String mimeTypeId, String partyId,
            String roleTypeId, Delegator delegator, LocalDispatcher dispatcher, String encoderType) {
        if (product == null) {
            return null;
        }

        /*
         * Look for a previously cached entry (may also be an entry with null value if
         * there was no content to retrieve) caching: there is one cache created,
         * "product.content.rendered" Each product's content is cached with a key of
         * contentTypeId::locale::mimeType::productId, or whatever the CACHE_KEY_SEPARATOR is
         * defined above to be.
         */
        String cacheKey = productContentTypeId + CACHE_KEY_SEPARATOR + locale + CACHE_KEY_SEPARATOR + mimeTypeId + CACHE_KEY_SEPARATOR
                + product.get("productId") + CACHE_KEY_SEPARATOR + encoderType + CACHE_KEY_SEPARATOR + delegator;
        String cachedValue = PRODUCT_CONTENT_CACHE.get(cacheKey);
        if (cachedValue != null || PRODUCT_CONTENT_CACHE.containsKey(cacheKey)) {
            return cachedValue;
        }

        // Get content of given contentTypeId
        boolean doCache = true;
        String outString = null;
        try {
            Writer outWriter = new StringWriter();
            // Use cache == true to have entity-cache managed content from cache while (not managed) rendered cache above
            // may be configured with short expire time
            getProductContentAsText(null, product, productContentTypeId, locale, mimeTypeId, partyId, roleTypeId,
                    delegator, dispatcher, outWriter, true);
            outString = outWriter.toString();
        } catch (GeneralException | IOException e) {
            Debug.logError(e, "Error rendering ProductContent", MODULE);
            doCache = false;
        }

        /*
         * If we did not found any content (or got an error), get the content of a
         * candidateFieldName matching the given contentTypeId
         */
        if (UtilValidate.isEmpty(outString)) {
            outString = ContentWrapper.getCandidateFieldValue(product, productContentTypeId);
        }
        // Encode found content via given encoderType
        outString = ContentWrapper.encodeContentValue(outString, encoderType);

        if (doCache) {
            PRODUCT_CONTENT_CACHE.put(cacheKey, outString);
        }
        return outString;
    }

    public static void getProductContentAsText(String productId, GenericValue product, String productContentTypeId, Locale locale, String mimeTypeId,
                                               String partyId, String roleTypeId, Delegator delegator, LocalDispatcher dispatcher, Writer outWriter)
            throws GeneralException, IOException {
        getProductContentAsText(productId, product, productContentTypeId, locale, mimeTypeId, partyId, roleTypeId, delegator, dispatcher,
                outWriter, true);
    }

    public static void getProductContentAsText(String productId, GenericValue product, String productContentTypeId, Locale locale, String mimeTypeId,
                                               String partyId, String roleTypeId, Delegator delegator, LocalDispatcher dispatcher,
                                               Writer outWriter, boolean cache) throws GeneralException, IOException {
        if (product != null) {
            productId = product.getString("productId");
        } else if (productId != null) {
            product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache(cache).queryOne();
        } else {
            throw new GeneralException("Missing parameter product or productId!");
        }

        if (delegator == null) {
            delegator = product.getDelegator();
        }
        if (UtilValidate.isEmpty(mimeTypeId)) {
            mimeTypeId = ContentWrapper.getDefaultMimeTypeId(delegator);
        }

        GenericValue parentProduct = null;
        List<GenericValue> productContentList = EntityQuery.use(delegator).from("ProductContent").where("productId", productId,
                "productContentTypeId", productContentTypeId).orderBy("-fromDate").cache(cache).filterByDate().queryList();
        if (UtilValidate.isEmpty(productContentList) && ("Y".equals(product.get("isVariant")))) {
            parentProduct = ProductWorker.getParentProduct(productId, delegator);
            if (parentProduct != null) {
                productContentList = EntityQuery.use(delegator).from("ProductContent").where("productId", parentProduct
                        .get("productId"), "productContentTypeId", productContentTypeId).orderBy("-fromDate").cache(
                                cache).filterByDate().queryList();
            }
        }
        GenericValue productContent = EntityUtil.getFirst(productContentList);
        if (productContent != null) {
            // when rendering the product content, always include the Product and ProductContent records that this comes from
            Map<String, Object> inContext = new HashMap<>();
            inContext.put("product", product);
            inContext.put("productContent", productContent);
            ContentWorker.renderContentAsText(dispatcher, productContent.getString("contentId"), outWriter, inContext, locale, mimeTypeId,
                    partyId, roleTypeId, cache);
        } else {
            String candidateValue = ContentWrapper.getCandidateFieldValue(product, productContentTypeId);
            if (UtilValidate.isEmpty(candidateValue) && parentProduct != null) {
                candidateValue = ContentWrapper.getCandidateFieldValue(parentProduct, productContentTypeId);
            }
            if (UtilValidate.isNotEmpty(candidateValue)) {
                outWriter.write(candidateValue);
            }
        }
    }
}
