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

import java.sql.Timestamp

import org.apache.ofbiz.accounting.payment.PaymentWorker
import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator
import org.apache.ofbiz.entity.util.EntityUtil
import org.apache.ofbiz.party.contact.ContactHelper
import org.apache.ofbiz.party.contact.ContactMechWorker
import org.apache.ofbiz.product.catalog.CatalogWorker
import org.apache.ofbiz.product.inventory.InventoryWorker
import org.apache.ofbiz.product.store.ProductStoreWorker

orderId = parameters.orderId
context.orderId = orderId

workEffortId = parameters.workEffortId
assignPartyId = parameters.partyId
assignRoleTypeId = parameters.roleTypeId
fromDate = parameters.fromDate
delegate = parameters.delegate
if (delegate && fromDate) {
    fromDate = parameters.toFromDate
}
context.workEffortId = workEffortId
context.assignPartyId = assignPartyId
context.assignRoleTypeId = assignRoleTypeId
context.fromDate = fromDate
context.delegate = delegate
context.todayDate = new java.sql.Date(System.currentTimeMillis()).toString()
String partyId = null

orderHeader = null
orderItems = null
orderAdjustments = null
comments = null

if (orderId) {
    orderHeader = from('OrderHeader').where('orderId', orderId).queryOne()
    comments = select('orderItemSeqId', 'changeComments', 'changeDatetime', 'changeUserLogin')
            .from('OrderItemChange')
            .where(UtilMisc.toList(EntityCondition.makeCondition('orderId', EntityOperator.EQUALS, orderId)))
            .orderBy('-changeDatetime')
            .queryList()
}

if (orderHeader) {
    // note these are overridden in the OrderViewWebSecure.groovy script if run
    context.hasPermission = true
    context.canViewInternalDetails = true

    orderReadHelper = new OrderReadHelper(orderHeader)
    orderItems = orderReadHelper.getOrderItems()
    orderAdjustments = orderReadHelper.getAdjustments()
    orderHeaderAdjustments = orderReadHelper.getOrderHeaderAdjustments()
    orderSubTotal = orderReadHelper.getOrderItemsSubTotal()
    backorderQuantity = orderReadHelper.getOrderBackorderQuantity()
    orderTerms = orderHeader.getRelated('OrderTerm', null, null, false)

    context << [
            orderHeader: orderHeader,
            backorderQuantity: backorderQuantity,
            comments: comments,
            orderReadHelper: orderReadHelper,
            orderItems: orderItems,
            orderAdjustments: orderAdjustments,
            orderHeaderAdjustments: orderHeaderAdjustments,
            orderSubTotal: orderSubTotal,
            currencyUomId: orderReadHelper.getCurrency(),
            orderTerms: orderTerms
    ]

    // get sales reps
    context.salesReps = orderHeader.getRelated('OrderRole', [orderId: orderHeader.orderId, roleTypeId: 'SALES_REP'], null, false)

    // get the order type
    orderType = orderHeader.orderTypeId
    context.orderType = orderType

    // get the display party
    displayParty = null
    if (orderType == 'PURCHASE_ORDER') {
        displayParty = orderReadHelper.getSupplierAgent()
    } else {
        displayParty = orderReadHelper.getPlacingParty()
    }
    if (displayParty) {
        partyId = displayParty.partyId
        context.displayParty = displayParty
        context.partyId = partyId

        paymentMethodValueMaps = PaymentWorker.getPartyPaymentMethodValueMaps(delegator, displayParty.partyId, false)
        context.paymentMethodValueMaps = paymentMethodValueMaps
    }

    otherAdjAmount = OrderReadHelper.calcOrderAdjustments(orderHeaderAdjustments, orderSubTotal, true, false, false)
    context.otherAdjAmount = otherAdjAmount

    shippingAmount = OrderReadHelper.getAllOrderItemsAdjustmentsTotal(orderItems, orderAdjustments, false, false, true)
    shippingAmount = shippingAmount.add(OrderReadHelper.calcOrderAdjustments(orderHeaderAdjustments, orderSubTotal, false, false, true))
    context.shippingAmount = shippingAmount

    taxAmount = OrderReadHelper.getOrderTaxByTaxAuthGeoAndParty(orderAdjustments).taxGrandTotal
    context.taxAmount = taxAmount

    grandTotal = orderReadHelper.getOrderGrandTotal()
    context.grandTotal = grandTotal

    orderItemList = orderReadHelper.getOrderItems()
    // Retrieve all non-promo items that aren't cancelled
    context.orderItemList = orderReadHelper.getOrderItems().findAll { item ->
        (item.statusId != 'ITEM_CANCELLED' || (item.isPromo == null || item.isPromo == 'N'))
    }

    shippingAddress = orderReadHelper.getShippingAddress()
    context.shippingAddress = shippingAddress

    billingAddress = orderReadHelper.getBillingAddress()
    context.billingAddress = billingAddress

    distributorId = orderReadHelper.getDistributorId()
    context.distributorId = distributorId

    affiliateId = orderReadHelper.getAffiliateId()
    context.affiliateId = affiliateId

    billingAccount = orderHeader.getRelatedOne('BillingAccount', false)
    context.billingAccount = billingAccount
    context.billingAccountMaxAmount = orderReadHelper.getBillingAccountMaxAmount()

    // get a list of all shipments, and a list of ItemIssuances per order item
    allShipmentsMap = [:]
    primaryShipments = orderHeader.getRelated('PrimaryShipment', null, null, false)
    primaryShipments.each { primaryShipment ->
        allShipmentsMap[primaryShipment.shipmentId] = primaryShipment
    }
    itemIssuancesPerItem = [:]
    itemIssuances = orderHeader.getRelated('ItemIssuance', null, [
        'shipmentId',
        'shipmentItemSeqId'
    ], false)
    itemIssuances.each { itemIssuance ->
        if (!allShipmentsMap.containsKey(itemIssuance.shipmentId)) {
            iiShipment = itemIssuance.getRelatedOne('Shipment', false)
            if (iiShipment) {
                allShipmentsMap[iiShipment.shipmentId] = iiShipment
            }
        }

        perItemList = itemIssuancesPerItem[itemIssuance.orderItemSeqId]
        if (!perItemList) {
            perItemList = []
            itemIssuancesPerItem[itemIssuance.orderItemSeqId] = perItemList
        }
        perItemList.add(itemIssuance)
    }
    context.allShipments = allShipmentsMap.values()
    context.itemIssuancesPerItem = itemIssuancesPerItem

    // get a list of all invoices
    orderBilling = from('OrderItemBilling').where('orderId', orderId).orderBy('invoiceId').queryList()
    context.invoices = orderBilling*.invoiceId.unique()

    ecl = EntityCondition.makeCondition([
        EntityCondition.makeCondition('orderId', EntityOperator.EQUALS, orderId),
        EntityCondition.makeCondition('statusId', EntityOperator.NOT_EQUAL, 'PAYMENT_CANCELLED')
    ],
    EntityOperator.AND)
    orderPaymentPreferences = from('OrderPaymentPreference').where(ecl).queryList()
    context.orderPaymentPreferences = orderPaymentPreferences

    // ship groups
    shipGroups = from('OrderItemShipGroup').where('orderId', orderId).orderBy('shipGroupSeqId').queryList()
    context.shipGroups = shipGroups

    orderItemDatas = []
    orderItemList.each { orderItem ->
        BigDecimal cancelQuantity = orderItem.get('cancelQuantity')
        BigDecimal quantity = orderItem.get('quantity')
        if ( cancelQuantity != null ) {
            quantityOrdered = quantity.subtract(cancelQuantity)
        } else {
            quantityOrdered = quantity
        }
        List oisgAssContents = []
        shipGroups.each { shipGroup ->
            oisgAssContents.addAll(EntityUtil.filterByAnd(shipGroup.getRelated('OrderItemShipGroupAssoc', null, null, false),
                    [orderItemSeqId: orderItem.getString('orderItemSeqId')]))
        }
        BigDecimal totalQuantityPlanned = 0
        oisgAssContents.each { oisgAssContent ->
            BigDecimal cancelQty = oisgAssContent.get('cancelQuantity')
            BigDecimal qty = oisgAssContent.get('quantity')
            if (qty != null) {
                totalQuantityPlanned = totalQuantityPlanned.add(qty)
            }
            if (cancelQty != null) {
                oisgAssContent.set('quantity', qty.subtract(cancelQty))
            } else {
                oisgAssContent.set('quantity', qty)
            }
        }
        totalQuantityToPlan = totalQuantityPlanned - quantityOrdered
        BigDecimal quantityNotAvailable = 0
        List<GenericValue> oisgirs = orderItem.getRelated('OrderItemShipGrpInvRes', null, null, false)
        for (GenericValue oisgir : oisgirs) {
            if (oisgir.get('quantityNotAvailable')) {
                quantityNotAvailable = quantityNotAvailable.add(oisgir.getBigDecimal('quantityNotAvailable'))
            }
        }
        orderItemData = [:]
        orderItemData.put('orderItem', orderItem)
        orderItemData.put('OISGAssContents', oisgAssContents)
        orderItemData.put('product', orderItem.getRelatedOne('Product', false))
        orderItemData.put('quantityOrdered', quantityOrdered)
        orderItemData.put('totalQuantityPlanned', totalQuantityPlanned)
        orderItemData.put('totalQuantityToPlan', totalQuantityToPlan)
        orderItemData.put('quantityNotAvailable', quantityNotAvailable)
        orderItemDatas.add(orderItemData)
    }
    context.put('orderItemDatas', orderItemDatas)

    // create the actualDate for calendar
    actualDateCal = Calendar.getInstance()
    actualDateCal.setTime(new Date())
    actualDateCal.set(Calendar.HOUR_OF_DAY, actualDateCal.getActualMinimum(Calendar.HOUR_OF_DAY))
    actualDateCal.set(Calendar.MINUTE, actualDateCal.getActualMinimum(Calendar.MINUTE))
    actualDateCal.set(Calendar.SECOND, actualDateCal.getActualMinimum(Calendar.SECOND))
    actualDateCal.set(Calendar.MILLISECOND, actualDateCal.getActualMinimum(Calendar.MILLISECOND))
    actualDateTs = new Timestamp(actualDateCal.getTimeInMillis())
    actualDateStr = actualDateTs.toString()
    actualDateStr = actualDateStr.substring(0, actualDateStr.indexOf('.'))
    context.put('actualDateStr', actualDateStr)

    // get Shipment tracking info
    orderShipmentInfoSummaryList = select('shipGroupSeqId', 'shipmentId', 'shipmentRouteSegmentId', 'carrierPartyId',
            'shipmentMethodTypeId', 'shipmentPackageSeqId', 'trackingCode', 'boxNumber')
            .from('OrderShipmentInfoSummary')
            .where('orderId', orderId)
            .orderBy('shipmentId', 'shipmentRouteSegmentId', 'shipmentPackageSeqId')
            .distinct()
            .queryList()
    context.orderShipmentInfoSummaryList = orderShipmentInfoSummaryList

    customerPoNumber = null
    orderItemList.each { orderItem ->
        customerPoNumber = orderItem.correspondingPoId
    }
    context.customerPoNumber = customerPoNumber

    statusChange = from('StatusValidChange').where('statusId', orderHeader.statusId).queryList()
    context.statusChange = statusChange

    currentStatus = orderHeader.getRelatedOne('StatusItem', false)
    context.currentStatus = currentStatus

    orderHeaderStatuses = orderReadHelper.getOrderHeaderStatuses()
    context.orderHeaderStatuses = orderHeaderStatuses

    adjustmentTypes = from('OrderAdjustmentType').orderBy('description').queryList()
    context.orderAdjustmentTypes = adjustmentTypes

    notes = from('OrderHeaderNoteView').where('orderId', orderId).orderBy('-noteDateTime').queryList()
    context.orderNotes = notes

    showNoteHeadingOnPDF = false
    if (notes && EntityUtil.filterByCondition(notes, EntityCondition.makeCondition('internalNote', EntityOperator.EQUALS, 'N')).size() > 0) {
        showNoteHeadingOnPDF = true
    }
    context.showNoteHeadingOnPDF = showNoteHeadingOnPDF

    cmvm = ContactMechWorker.getOrderContactMechValueMaps(delegator, orderId)
    context.orderContactMechValueMaps = cmvm

    orderItemChangeReasons = from('Enumeration').where('enumTypeId', 'ODR_ITM_CH_REASON').orderBy('sequenceId').queryList()
    context.orderItemChangeReasons = orderItemChangeReasons

    if (orderType == 'PURCHASE_ORDER') {
        // for purchase orders, we need also the supplier's postal address
        supplier = orderReadHelper.getBillFromParty()
        if (supplier) {
            supplierContactMechValueMaps = ContactMechWorker.getPartyContactMechValueMaps(delegator, supplier.partyId, false, 'POSTAL_ADDRESS')
            context.supplierContactMechValueMaps = supplierContactMechValueMaps
            supplierContactMechValueMaps.each { supplierContactMechValueMap ->
                contactMechPurposes = supplierContactMechValueMap.partyContactMechPurposes
                contactMechPurposes.each { contactMechPurpose ->
                    switch (contactMechPurpose.contactMechPurposeTypeId) {
                        case 'GENERAL_LOCATION':
                            context.supplierGeneralContactMechValueMap = supplierContactMechValueMap
                            break
                        case 'SHIPPING_LOCATION':
                            context.supplierShippingContactMechValueMap = supplierContactMechValueMap
                            break
                        case 'BILLING_LOCATION':
                            context.supplierBillingContactMechValueMap = supplierContactMechValueMap
                            break
                        case 'PAYMENT_LOCATION':
                            context.supplierPaymentContactMechValueMap = supplierContactMechValueMap
                            break
                    }
                }
            }
        }
        // get purchase order item types
        purchaseOrderItemTypeList = from('OrderItemType').where('parentTypeId', 'PURCHASE_SPECIFIC').cache(true).queryList()
        context.purchaseOrderItemTypeList = purchaseOrderItemTypeList
    }

    // see if an approved order with all items completed exists
    context.setOrderCompleteOption = false
    if (orderHeader.statusId == 'ORDER_APPROVED') {
        expr = EntityCondition.makeCondition('statusId', EntityOperator.NOT_EQUAL, 'ITEM_COMPLETED')
        notCreatedItems = orderReadHelper.getOrderItemsByCondition(expr)
        if (!notCreatedItems) {
            context.setOrderCompleteOption = true
        }
    }

    // get inventory summary for each shopping cart product item
    inventorySummary = runService('getProductInventorySummaryForItems', [orderItems: orderItems])
    context.availableToPromiseMap = inventorySummary.availableToPromiseMap
    context.quantityOnHandMap = inventorySummary.quantityOnHandMap
    context.mktgPkgATPMap = inventorySummary.mktgPkgATPMap
    context.mktgPkgQOHMap = inventorySummary.mktgPkgQOHMap

    // get inventory summary with respect to facility
    productStore = orderReadHelper.getProductStore()
    context.productStore = productStore
    if (productStore) {
        facility = productStore.getRelatedOne('Facility', false)
        if (facility) {
            inventorySummaryByFacility = runService('getProductInventorySummaryForItems', [orderItems: orderItems, facilityId: facility.facilityId])
            context.availableToPromiseByFacilityMap = inventorySummaryByFacility.availableToPromiseMap
            context.quantityOnHandByFacilityMap = inventorySummaryByFacility.quantityOnHandMap
            context.facility = facility
        }
    }

    // Get a list of facilities for purchase orders to receive against.
    // These facilities must be owned by the bill-to party of the purchase order.
    // For a given ship group, the allowed facilities are the ones associated
    // to the same contact mech of the ship group.
    if (orderType == 'PURCHASE_ORDER') {
        facilitiesForShipGroup = [:]
        if (orderReadHelper.getBillToParty()) {
            ownerPartyId = orderReadHelper.getBillToParty().partyId
            Map ownedFacilities = [:]
            shipGroups.each { shipGroup ->
                lookupMap = [ownerPartyId: ownerPartyId]
                if (shipGroup.contactMechId) {
                    lookupMap.contactMechId = shipGroup.contactMechId
                }
                facilities = from('FacilityAndContactMech').where(lookupMap).cache(true).queryList()
                facilitiesForShipGroup[shipGroup.shipGroupSeqId] = facilities
                facilities.each { facility ->
                    ownedFacilities[facility.facilityId] = facility
                }
            }
            context.facilitiesForShipGroup = facilitiesForShipGroup
            // Now get the list of all the facilities owned by the bill-to-party
            context.ownedFacilities = ownedFacilities.values()
        }
    }

    // set the type of return based on type of order
    if (orderType == 'SALES_ORDER') {
        context.returnHeaderTypeId = 'CUSTOMER_RETURN'
        // also set the product store facility Id for sales orders
        if (productStore) {
            context.storeFacilityId = productStore.inventoryFacilityId
            if (productStore.reqReturnInventoryReceive) {
                context.needsInventoryReceive = productStore.reqReturnInventoryReceive
            } else {
                context.needsInventoryReceive = 'Y'
            }
        }
    } else {
        context.returnHeaderTypeId = 'VENDOR_RETURN'
    }

    // QUANTITY: get the returned quantity by order item map
    context.returnQuantityMap = orderReadHelper.getOrderItemReturnedQuantities()

    // INVENTORY: construct a Set of productIds in the order for use in querying for inventory, otherwise these queries can get expensive
    productIds = orderReadHelper.getOrderProductIds()

    // INVENTORY: get the production quantity for each product and store the results in a map of productId -> quantity
    productionMap = [:]
    productIds.each { productId ->
        if (productId) {  // avoid order items without productIds, such as bulk order items
            resultOutput = runService('getProductManufacturingSummaryByFacility', [productId: productId])
            manufacturingInQuantitySummaryByFacility = resultOutput.summaryInByFacility
            BigDecimal productionQuantity = 0
            manufacturingInQuantitySummaryByFacility.values().each { manQuantity ->
                productionQuantity += manQuantity.estimatedQuantityTotal
            }
            productionMap[productId] = productionQuantity
        }
    }
    context.productionProductQuantityMap = productionMap

    if (productIds.size() > 0) {
        // INVENTORY: find the number of products in outstanding sales orders for the same product store
        requiredMap = InventoryWorker.getOutstandingProductQuantitiesForSalesOrders(productIds, delegator)
        context.requiredProductQuantityMap = requiredMap

        // INVENTORY: find the quantity of each product in outstanding purchase orders
        onOrderMap = InventoryWorker.getOutstandingProductQuantitiesForPurchaseOrders(productIds, delegator)
        context.onOrderProductQuantityMap = onOrderMap
    } else {
        context.requiredProductQuantityMap = [:]
        context.onOrderProductQuantityMap = [:]
    }

    // list to find all the POSTAL_ADDRESS for the shipment party.
    orderParty = from('Party').where('partyId', partyId).queryOne()
    shippingContactMechList = ContactHelper.getContactMech(orderParty, 'SHIPPING_LOCATION', 'POSTAL_ADDRESS', false)
    context.shippingContactMechList = shippingContactMechList

    // list to find all the shipmentMethods from the view named "ProductStoreShipmentMethView".
    shipGroupShippingMethods = [:]
    shipGroups.each { shipGroup ->
        shipGroupSeqId = shipGroup.shipGroupSeqId
        shippableItemFeatures = orderReadHelper.getFeatureIdQtyMap(shipGroupSeqId)
        shippableTotal = orderReadHelper.getShippableTotal(shipGroupSeqId)
        shippableWeight = orderReadHelper.getShippableWeight(shipGroupSeqId)
        shippableItemSizes = orderReadHelper.getShippableSizes(shipGroupSeqId)
        shippingAddress = orderReadHelper.getShippingAddress(shipGroupSeqId)

        List<GenericValue> productStoreShipmentMethList = ProductStoreWorker.getAvailableStoreShippingMethods(delegator,
                orderReadHelper.getProductStoreId(), shippingAddress, shippableItemSizes, shippableItemFeatures, shippableWeight, shippableTotal)
        shipGroupShippingMethods.put(shipGroupSeqId, productStoreShipmentMethList)
        context.shipGroupShippingMethods = shipGroupShippingMethods
    }

    // Get a map of returnable items
    returnableItems = [:]
    returnableItemServiceMap = run service: 'getReturnableItems', with: [orderId: orderId]
    if (returnableItemServiceMap.returnableItems) {
        returnableItems = returnableItemServiceMap.returnableItems
    }
    context.returnableItems = returnableItems

    // get the catalogIds for appending items
    if (context.request != null) {
        if (orderType == 'SALES_ORDER' && productStore) {
            catalogCol = CatalogWorker.getCatalogIdsAvailable(delegator, productStore.productStoreId, partyId)
        } else {
            catalogCol = CatalogWorker.getAllCatalogIds(request)
        }
        if (catalogCol) {
            currentCatalogId = catalogCol[0]
            currentCatalogName = CatalogWorker.getCatalogName(request, currentCatalogId)
            context.catalogCol = catalogCol
            context.currentCatalogId = currentCatalogId
            context.currentCatalogName = currentCatalogName
        }
    }

    // list to find all the POSTAL_ADDRESS for the party.
    orderParty = from('Party').where('partyId', partyId).queryOne()
    postalContactMechList = ContactHelper.getContactMechByType(orderParty, 'POSTAL_ADDRESS', false)
    context.postalContactMechList = postalContactMechList

    // list to find all the TELECOM_NUMBER for the party.
    telecomContactMechList = ContactHelper.getContactMechByType(orderParty, 'TELECOM_NUMBER', false)
    context.telecomContactMechList = telecomContactMechList

    // list to find all the EMAIL_ADDRESS for the party.
    emailContactMechList = ContactHelper.getContactMechByType(orderParty, 'EMAIL_ADDRESS', false)
    context.emailContactMechList = emailContactMechList
}

paramString = ''
if (orderId) {
    paramString += 'orderId=' + orderId
}
if (workEffortId) {
    paramString += '&workEffortId=' + workEffortId
}
if (assignPartyId) {
    paramString += '&partyId=' + assignPartyId
}
if (assignRoleTypeId) {
    paramString += '&roleTypeId=' + assignRoleTypeId
}
if (fromDate) {
    paramString += '&fromDate=' + fromDate
}
context.paramString = paramString

workEffortStatus = null
if (workEffortId && assignPartyId && assignRoleTypeId && fromDate) {
    wepa = from('WorkEffortPartyAssignment')
            .where('workEffortId', workEffortId, 'partyId', assignPartyId, 'roleTypeId', assignRoleTypeId, 'fromDate', fromDate).queryOne()

    if (wepa?.statusId == 'CAL_ACCEPTED') {
        workEffort = from('WorkEffort').where('workEffortId', workEffortId).queryOne()
        workEffortStatus = workEffort.currentStatusId
        if (workEffortStatus) {
            context.workEffortStatus = workEffortStatus
            if (workEffortStatus == 'WF_RUNNING' || workEffortStatus == 'WF_SUSPENDED') {
                context.inProcess = true
            }
        }
    }
}

if (orderItems) {
    orderItem = EntityUtil.getFirst(orderItems)
    context.orderItem = orderItem
}

// getting online ship estimates corresponding to this Order from UPS when "Hold" button will be clicked, when user packs from weight package screen.
// This case comes when order's shipping amount is  more then or less than default percentage (defined in shipment.properties)
// of online UPS shipping amount.

shipments = from('Shipment').where('primaryOrderId', orderId, 'statusId', 'SHIPMENT_PICKED').queryList()
if (shipments) {
    pickedShipmentId = EntityUtil.getFirst(shipments).shipmentId
    shipmentRouteSegment = from('ShipmentRouteSegment').where('shipmentId', pickedShipmentId).queryFirst()
    context.shipmentRouteSegmentId = shipmentRouteSegment.shipmentRouteSegmentId
    context.pickedShipmentId = pickedShipmentId
    if (pickedShipmentId && shipmentRouteSegment.trackingIdNumber) {
        if (shipmentRouteSegment.carrierPartyId == 'UPS' && productStore) {
            resultMap = runService('upsShipmentAlternateRatesEstimate', [productStoreId: productStore.productStoreId, shipmentId: pickedShipmentId])
            shippingRates = resultMap.shippingRates
            shippingRateList = []
            shippingRates.each { shippingRate ->
                shippingMethodAndRate = [:]
                serviceCodes = shippingRate.keySet()
                serviceCodes.each { serviceCode ->
                    carrierShipmentMethod = from('CarrierShipmentMethod').where('partyId', 'UPS', 'carrierServiceCode', serviceCode).queryFirst()
                    shipmentMethodTypeId = carrierShipmentMethod.shipmentMethodTypeId
                    rate = shippingRate.get(serviceCode)
                    shipmentMethodDescription = carrierShipmentMethod.getRelated('ShipmentMethodType', null, null, false)[0]?.description
                    shippingMethodAndRate.shipmentMethodTypeId = carrierShipmentMethod.shipmentMethodTypeId
                    shippingMethodAndRate.rate = rate
                    shippingMethodAndRate.shipmentMethodDescription = shipmentMethodDescription
                    shippingRateList.add(shippingMethodAndRate)
                }
            }
            context.shippingRateList = shippingRateList
        }
    }
}

// get orderAdjustmentId for SHIPPING_CHARGES
orderAdjustmentId = null
orderAdjustments.each { orderAdjustment ->
    if (orderAdjustment.orderAdjustmentTypeId == 'SHIPPING_CHARGES') {
        orderAdjustmentId = orderAdjustment.orderAdjustmentId
    }
}
context.orderAdjustmentId = orderAdjustmentId
