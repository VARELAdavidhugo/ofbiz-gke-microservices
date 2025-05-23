/*
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
*/
package org.apache.ofbiz.order.order

orderHeader = context.orderHeader

// can anybody view an anonymous order?  this is set in the screen widget and should only be turned on by an email confirmation screen
allowAnonymousView = context.allowAnonymousView

// if orderHeader is null in OrderView.groovy then it is not null but void here!
if (orderHeader) {
    // set hasPermission, must always exist if the orderHeader != null
    // hasPermission if: has ORDERMGR_VIEW, ORDERMGR_ROLE_VIEW & associated with order, or is associated in the SUPPLIER_AGENT role
    hasPermission = false
    canViewInternalDetails = false
    if ((orderHeader.orderTypeId == 'SALES_ORDER' && security.hasEntityPermission('ORDERMGR', '_VIEW', session))
        || (orderHeader.orderTypeId == 'PURCHASE_ORDER' && security.hasEntityPermission('ORDERMGR', '_PURCHASE_VIEW', session))) {
        hasPermission = true
        canViewInternalDetails = true
    } else if (security.hasEntityPermission('ORDERMGR_ROLE', '_VIEW', session)) {
        currentUserOrderRoles = orderHeader.getRelated('OrderRole', [partyId: userLogin.partyId], null, false)
        if (currentUserOrderRoles) {
            hasPermission = true
            canViewInternalDetails = true
        }
    } else {
        // regardless of permission, allow if this is the supplier
        currentUserSupplierOrderRoles = orderHeader.getRelated('OrderRole', [partyId: userLogin.partyId, roleTypeId: 'SUPPLIER_AGENT'], null, false)
        if (currentUserSupplierOrderRoles) {
            hasPermission = true
        }
    }
    // This is related with OFBIZ-11836 "IDOR vulnerability in the order processing feature"
    if (parameters.localDispatcherName == 'ecommerce') {
        List errMsgList = []
        if (orderHeader.createdBy == person.partyId
        || (orderHeader.createdBy == 'anonymous' && allowAnonymousView == 'Y')) {
            hasPermission = true
            canViewInternalDetails = true
        } else {
            hasPermission = false
            canViewInternalDetails = false
            errMsgList.add("It's not an error : you are not allowed to view this!")
            showErrorMsg = 'Y'
            request.setAttribute('_ERROR_MESSAGE_LIST_', errMsgList)
            context.showErrorMsg = showErrorMsg
        }
    }

    context.hasPermission = hasPermission
    context.canViewInternalDetails = canViewInternalDetails

    orderContentWrapper = OrderContentWrapper.makeOrderContentWrapper(orderHeader, request)
    context.orderContentWrapper = orderContentWrapper
}
