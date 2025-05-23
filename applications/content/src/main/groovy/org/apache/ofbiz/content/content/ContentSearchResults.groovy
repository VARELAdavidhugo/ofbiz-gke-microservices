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
package org.apache.ofbiz.content.content

// note: this can be run multiple times in the same request without causing problems, will check to see on its own if it has run again
ContentSearchSession.processSearchParameters(parameters, request)
Map result = ContentSearchEvents.getContentSearchResult(request, delegator)

context.with {
    contentIds = result.get('contentIds')
    viewIndex = result.get('viewIndex')
    viewSize = result.get('viewSize')
    listSize = result.get('listSize')
    lowIndex = result.get('lowIndex')
    highIndex = result.get('highIndex')
    searchConstraintStrings = result.get('searchConstraintStrings')
    searchSortOrderString = result.get('searchSortOrderString')
}
