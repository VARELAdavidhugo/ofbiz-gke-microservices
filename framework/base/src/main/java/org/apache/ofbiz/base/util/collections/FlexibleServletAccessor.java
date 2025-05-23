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
package org.apache.ofbiz.base.util.collections;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;

/**
 * Used to flexibly access Map values, supporting the "." (dot) syntax for
 * accessing sub-map values and the "[]" (square bracket) syntax for accessing
 * list elements. See individual Map operations for more information.
 *
 */
@SuppressWarnings("serial")
public class FlexibleServletAccessor<T> implements Serializable {

    private String name;
    private String attributeName;
    private FlexibleMapAccessor<T> fma;
    private boolean needsExpand;
    private boolean empty;

    public FlexibleServletAccessor(String name) {
        init(name);
    }

    public FlexibleServletAccessor(String name, String defaultName) {
        if (UtilValidate.isEmpty(name)) {
            init(defaultName);
        } else {
            init(name);
        }
    }

    /**
     * Init.
     * @param name the name
     */
    protected void init(String name) {
        this.name = name;
        if (UtilValidate.isEmpty(name)) {
            empty = true;
            needsExpand = false;
            fma = FlexibleMapAccessor.getInstance(name);
            attributeName = name;
        } else {
            empty = false;
            int openPos = name.indexOf("${");
            if (openPos != -1 && name.indexOf('}', openPos) != -1) {
                fma = null;
                attributeName = null;
                needsExpand = true;
            } else {
                int dotIndex = name.indexOf('.');
                if (dotIndex != -1) {
                    attributeName = name.substring(0, dotIndex);
                    fma = FlexibleMapAccessor.getInstance(name.substring(dotIndex + 1));
                } else {
                    attributeName = name;
                    fma = null;
                }

                needsExpand = false;
            }
        }
    }

    /**
     * Is empty boolean.
     * @return the boolean
     */
    public boolean isEmpty() {
        return this.empty;
    }

    /** Based on name get from ServletRequest or from List in ServletRequest
     * @param request request to get the value from
     * @param expandContext the context to use for name expansion
     * @return the object corresponding to this getter class
     */
    public T get(ServletRequest request, Map<String, Object> expandContext) {
        AttributeAccessor<T> aa = new AttributeAccessor<>(name, expandContext, this.attributeName, this.fma, this.needsExpand);
        return aa.get(request);
    }

    /** Based on name get from HttpSession or from List in HttpSession
     * @param session
     * @param expandContext
     * @return the found value
     */
    public T get(HttpSession session, Map<String, Object> expandContext) {
        AttributeAccessor<T> aa = new AttributeAccessor<>(name, expandContext, this.attributeName, this.fma, this.needsExpand);
        return aa.get(session);
    }

    /** Based on name put in ServletRequest or from List in ServletRequest;
     * If the brackets for a list are empty the value will be appended to the list,
     * otherwise the value will be set in the position of the number in the brackets.
     * If a "+" (plus sign) is included inside the square brackets before the index
     * number the value will inserted/added at that point instead of set at the point.
     * @param request
     * @param value
     * @param expandContext
     */
    public void put(ServletRequest request, T value, Map<String, Object> expandContext) {
        AttributeAccessor<T> aa = new AttributeAccessor<>(name, expandContext, this.attributeName, this.fma, this.needsExpand);
        aa.put(request, value);
    }

    /** Based on name put in HttpSession or from List in HttpSession;
     * If the brackets for a list are empty the value will be appended to the list,
     * otherwise the value will be set in the position of the number in the brackets.
     * If a "+" (plus sign) is included inside the square brackets before the index
     * number the value will inserted/added at that point instead of set at the point.
     * @param session
     * @param value
     * @param expandContext
     */
    public void put(HttpSession session, T value, Map<String, Object> expandContext) {
        AttributeAccessor<T> aa = new AttributeAccessor<>(name, expandContext, this.attributeName, this.fma, this.needsExpand);
        aa.put(session, value);
    }

    /** Based on name remove from ServletRequest or from List in ServletRequest
     * @param request
     * @param expandContext
     * @return the removed value
     */
    public T remove(ServletRequest request, Map<String, Object> expandContext) {
        AttributeAccessor<T> aa = new AttributeAccessor<>(name, expandContext, this.attributeName, this.fma, this.needsExpand);
        return aa.remove(request);
    }

    /** Based on name remove from HttpSession or from List in HttpSession
     * @param session
     * @param expandContext
     * @return the removed value
     */
    public T remove(HttpSession session, Map<String, Object> expandContext) {
        AttributeAccessor<T> aa = new AttributeAccessor<>(name, expandContext, this.attributeName, this.fma, this.needsExpand);
        return aa.remove(session);
    }

    /** The equals and hashCode methods are implemented just case this object is ever accidently used as a Map key *
     * @return the hashcode
     */
    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    /**
     * The equals and hashCode methods are implemented just in case this object is ever accidently used as a Map key
     * @param obj
     * @return whether this object is equal to the passed object
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof FlexibleServletAccessor<?>)) {
            return false;
        } else {
            FlexibleServletAccessor<?> flexibleServletAccessor = (FlexibleServletAccessor<?>) obj;
            if (name == null) {
                return flexibleServletAccessor.name == null;
            }
            return name.equals(flexibleServletAccessor.name);
        }
    }

    /** To be used for a string representation of the accessor, returns the original name.
     * @return the name of this accessor
     */
    @Override
    public String toString() {
        return this.name;
    }

    protected static class AttributeAccessor<T> implements Serializable {
        private String attributeName;
        private FlexibleMapAccessor<T> fma;
        private boolean isListReference;
        private boolean isAddAtIndex;
        private boolean isAddAtEnd;
        private int listIndex;
        private int openBrace;
        private int closeBrace;

        public AttributeAccessor(String origName, Map<String, Object> expandContext, String defAttributeName,
                                 FlexibleMapAccessor<T> defFma, boolean needsExpand) {
            attributeName = defAttributeName;
            fma = defFma;

            if (needsExpand) {
                String name = FlexibleStringExpander.expandString(origName, expandContext);
                int dotIndex = name.indexOf('.');
                if (dotIndex != -1) {
                    attributeName = name.substring(0, dotIndex);
                    fma = FlexibleMapAccessor.getInstance(name.substring(dotIndex + 1));
                } else {
                    attributeName = name;
                    fma = null;
                }
            }

            isListReference = false;
            isAddAtIndex = false;
            isAddAtEnd = false;
            listIndex = -1;
            openBrace = attributeName.indexOf('[');
            closeBrace = (openBrace == -1 ? -1 : attributeName.indexOf(']', openBrace));
            if (openBrace != -1 && closeBrace != -1) {
                String liStr = attributeName.substring(openBrace + 1, closeBrace);
                //if brackets are empty, append to list
                if (liStr.isEmpty()) {
                    isAddAtEnd = true;
                } else {
                    if (liStr.charAt(0) == '+') {
                        liStr = liStr.substring(1);
                        listIndex = Integer.parseInt(liStr);
                        isAddAtIndex = true;
                    } else {
                        listIndex = Integer.parseInt(liStr);
                    }
                }
                attributeName = attributeName.substring(0, openBrace);
                isListReference = true;
            }

        }

        /**
         * Get t.
         ** @param request the request
         * @return the t
         */
        public T get(ServletRequest request) {
            Object theValue = null;
            if (isListReference) {
                List<T> lst = UtilGenerics.cast(request.getAttribute(attributeName));
                theValue = lst.get(listIndex);
            } else {
                theValue = request.getAttribute(attributeName);
            }

            if (fma != null) {
                return fma.get(UtilGenerics.cast(theValue));
            }
            return UtilGenerics.<T>cast(theValue);
        }

        /**
         * Get t.
         * @param session the session
         * @return the t
         */
        public T get(HttpSession session) {
            Object theValue = null;
            if (isListReference) {
                List<T> lst = UtilGenerics.cast(session.getAttribute(attributeName));
                theValue = lst.get(listIndex);
            } else {
                theValue = session.getAttribute(attributeName);
            }

            if (fma != null) {
                return fma.get(UtilGenerics.cast(theValue));
            }
            return UtilGenerics.<T>cast(theValue);
        }

        /**
         * Put in list.
         * @param lst   the lst
         * @param value the value
         */
        protected void putInList(List<T> lst, T value) {
            //if brackets are empty, append to list
            if (isAddAtEnd) {
                lst.add(value);
            } else {
                if (isAddAtIndex) {
                    lst.add(listIndex, value);
                } else {
                    lst.set(listIndex, value);
                }
            }
        }

        /**
         * Put.
         * @param request the request
         * @param value   the value
         */
        public void put(ServletRequest request, T value) {
            if (fma == null) {
                if (isListReference) {
                    List<T> lst = UtilGenerics.cast(request.getAttribute(attributeName));
                    putInList(lst, value);
                } else {
                    request.setAttribute(attributeName, value);
                }
            } else {
                Object theObj = request.getAttribute(attributeName);
                if (isListReference) {
                    List<T> lst = UtilGenerics.cast(theObj);
                    fma.put(UtilGenerics.checkMap(lst.get(listIndex), String.class, Object.class), value);
                } else {
                    fma.put(UtilGenerics.checkMap(theObj, String.class, Object.class), value);
                }
            }
        }

        /**
         * Put.
         * @param session the session
         * @param value   the value
         */
        public void put(HttpSession session, T value) {
            if (fma == null) {
                if (isListReference) {
                    List<T> lst = UtilGenerics.cast(session.getAttribute(attributeName));
                    putInList(lst, value);
                } else {
                    session.setAttribute(attributeName, value);
                }
            } else {
                Object theObj = session.getAttribute(attributeName);
                if (isListReference) {
                    List<T> lst = UtilGenerics.cast(theObj);
                    fma.put(UtilGenerics.checkMap(lst.get(listIndex), String.class, Object.class), value);
                } else {
                    fma.put(UtilGenerics.checkMap(theObj, String.class, Object.class), value);
                }
            }
        }

        /**
         * Remove t.
         * @param request the request
         * @return the t
         */
        public T remove(ServletRequest request) {
            if (fma != null) {
                Object theObj = request.getAttribute(attributeName);
                if (isListReference) {
                    List<Object> lst = UtilGenerics.cast(theObj);
                    return fma.remove(UtilGenerics.checkMap(lst.get(listIndex), String.class, Object.class));
                }
                return fma.remove(UtilGenerics.checkMap(theObj, String.class, Object.class));
            }
            if (isListReference) {
                List<Object> lst = UtilGenerics.cast(request.getAttribute(attributeName));
                return UtilGenerics.<T>cast(lst.remove(listIndex));
            }
            Object theValue = request.getAttribute(attributeName);
            request.removeAttribute(attributeName);
            return UtilGenerics.<T>cast(theValue);
        }

        /**
         * Remove t.
         * @param session the session
         * @return the t
         */
        public T remove(HttpSession session) {
            if (fma != null) {
                Object theObj = session.getAttribute(attributeName);
                if (isListReference) {
                    List<Object> lst = UtilGenerics.cast(theObj);
                    return fma.remove(UtilGenerics.checkMap(lst.get(listIndex), String.class, Object.class));
                }
                return fma.remove(UtilGenerics.checkMap(theObj, String.class, Object.class));
            }
            if (isListReference) {
                List<Object> lst = UtilGenerics.cast(session.getAttribute(attributeName));
                return UtilGenerics.<T>cast(lst.remove(listIndex));
            }
            Object theValue = session.getAttribute(attributeName);
            session.removeAttribute(attributeName);
            return UtilGenerics.<T>cast(theValue);
        }
    }
}
