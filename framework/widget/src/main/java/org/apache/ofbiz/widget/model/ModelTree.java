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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilCodec;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.collections.MapStack;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.widget.WidgetWorker;
import org.apache.ofbiz.widget.model.CommonWidgetModels.Parameter;
import org.apache.ofbiz.widget.renderer.ScreenRenderException;
import org.apache.ofbiz.widget.renderer.ScreenStringRenderer;
import org.apache.ofbiz.widget.renderer.TreeStringRenderer;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Models the &lt;tree&gt; element.
 *
 * @see <code>widget-tree.xsd</code>
 */
@SuppressWarnings("serial")
public class ModelTree extends ModelWidget {

    /*
     * ----------------------------------------------------------------------- *
     *                     DEVELOPERS PLEASE READ
     * ----------------------------------------------------------------------- *
     * This model is intended to be a read-only data structure that represents
     * an XML element. Outside of object construction, the class should not
     * have any behaviors.
     * Instances of this class will be shared by multiple threads - therefore
     * it is immutable. DO NOT CHANGE THE OBJECT'S STATE AT RUN TIME!
     */

    private static final String MODULE = ModelTree.class.getName();

    private final String defaultEntityName;
    private final String defaultRenderStyle;
    private final FlexibleStringExpander defaultWrapStyleExdr;
    private final FlexibleStringExpander expandCollapseRequestExdr;
    private final boolean forceChildCheck;
    private final String location;
    private final Map<String, ModelNode> nodeMap;
    private final int openDepth;
    private final int postTrailOpenDepth;
    private final String rootNodeName;
    private final FlexibleStringExpander trailNameExdr;

    public ModelTree(Element treeElement, String location) {
        super(treeElement);
        this.location = location;
        this.rootNodeName = treeElement.getAttribute("root-node-name");
        String defaultRenderStyle = UtilXml.checkEmpty(treeElement.getAttribute("default-render-style"), "simple");
        // A temporary hack to accommodate those who might still be using "render-style" instead of "default-render-style"
        if (defaultRenderStyle.isEmpty() || "simple".equals(defaultRenderStyle)) {
            String rStyle = treeElement.getAttribute("render-style");
            if (!rStyle.isEmpty()) {
                defaultRenderStyle = rStyle;
            }
        }
        this.defaultRenderStyle = defaultRenderStyle;
        this.defaultWrapStyleExdr = FlexibleStringExpander.getInstance(treeElement.getAttribute("default-wrap-style"));
        this.expandCollapseRequestExdr = FlexibleStringExpander.getInstance(treeElement.getAttribute("expand-collapse-request"));
        this.trailNameExdr = FlexibleStringExpander.getInstance(UtilXml.checkEmpty(treeElement.getAttribute("trail-name"),
                "trail"));
        this.forceChildCheck = !"false".equals(treeElement.getAttribute("force-child-check"));
        this.defaultEntityName = treeElement.getAttribute("entity-name");
        int openDepth = 0;
        if (treeElement.hasAttribute("open-depth")) {
            try {
                openDepth = Integer.parseInt(treeElement.getAttribute("open-depth"));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid open-depth attribute value for the tree definition with name: "
                        + getName());
            }
        }
        this.openDepth = openDepth;
        int postTrailOpenDepth = 999;
        if (treeElement.hasAttribute("post-trail-open-depth")) {
            try {
                postTrailOpenDepth = Integer.parseInt(treeElement.getAttribute("post-trail-open-depth"));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Invalid post-trail-open-depth attribute value for the tree definition with name: " + getName());
            }
        }
        this.postTrailOpenDepth = postTrailOpenDepth;
        List<? extends Element> nodeElements = UtilXml.childElementList(treeElement, "node");
        if (nodeElements.isEmpty()) {
            throw new IllegalArgumentException("No node elements found for the tree definition with name: " + getName());
        }
        Map<String, ModelNode> nodeMap = new HashMap<>();
        for (Element nodeElementEntry : UtilXml.childElementList(treeElement, "node")) {
            ModelNode node = new ModelNode(nodeElementEntry, this);
            String nodeName = node.getName();
            nodeMap.put(nodeName, node);
        }
        this.nodeMap = Collections.unmodifiableMap(nodeMap);
    }

    @Override
    public void accept(ModelWidgetVisitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    public String getBoundaryCommentName() {
        return location + "#" + getName();
    }

    /**
     * Gets default entity name.
     * @return the default entity name
     */
    public String getDefaultEntityName() {
        return this.defaultEntityName;
    }

    /**
     * Gets default pk name.
     * @param context the context
     * @return the default pk name
     */
    public String getDefaultPkName(Map<String, Object> context) {
        ModelEntity modelEntity = WidgetWorker.getDelegator(context).getModelEntity(this.defaultEntityName);
        if (modelEntity.getPksSize() == 1) {
            ModelField modelField = modelEntity.getOnlyPk();
            return modelField.getName();
        }
        return null;
    }

    /**
     * Gets expand collapse request.
     * @param context the context
     * @return the expand collapse request
     */
    public String getExpandCollapseRequest(Map<String, Object> context) {
        String expColReq = this.expandCollapseRequestExdr.expandString(context);
        if (UtilValidate.isEmpty(expColReq)) {
            HttpServletRequest request = (HttpServletRequest) context.get("request");
            String s1 = request.getRequestURI();
            int pos = s1.lastIndexOf('/');
            if (pos >= 0) {
                expColReq = s1.substring(pos + 1);
            } else {
                expColReq = s1;
            }
        }
        //append also the request parameters
        Map<String, Object> paramMap = UtilGenerics.cast(context.get("requestParameters"));
        if (UtilValidate.isNotEmpty(paramMap)) {
            Map<String, Object> requestParameters = new HashMap<>(paramMap);
            requestParameters.remove(this.getTrailName(context));
            if (UtilValidate.isNotEmpty(requestParameters)) {
                String queryString = UtilHttp.urlEncodeArgs(requestParameters, false);
                if (expColReq.indexOf('?') < 0) {
                    expColReq += "?";
                } else {
                    expColReq += "&amp;";
                }
                expColReq += queryString;
            }
        }
        return expColReq;
    }

    /**
     * Gets open depth.
     * @return the open depth
     */
    public int getOpenDepth() {
        return openDepth;
    }

    /**
     * Gets post trail open depth.
     * @return the post trail open depth
     */
    public int getPostTrailOpenDepth() {
        return postTrailOpenDepth;
    }

    /**
     * Gets render style.
     * @return the render style
     */
    public String getRenderStyle() {
        return this.defaultRenderStyle;
    }

    /**
     * Gets root node name.
     * @return the root node name
     */
    public String getRootNodeName() {
        return rootNodeName;
    }

    /**
     * Gets trail name.
     * @param context the context
     * @return the trail name
     */
    public String getTrailName(Map<String, Object> context) {
        return this.trailNameExdr.expandString(context);
    }

    /**
     * Gets wrap style.
     * @param context the context
     * @return the wrap style
     */
    public String getWrapStyle(Map<String, Object> context) {
        return this.defaultWrapStyleExdr.expandString(context);
    }

    /**
     * Renders this model.
     * @param writer
     * @param context
     * @param treeStringRenderer
     */
    @SuppressWarnings("rawtypes")
    public void renderTreeString(Appendable writer, Map<String, Object> context, TreeStringRenderer treeStringRenderer)
            throws GeneralException {
        Map<String, Object> parameters = UtilGenerics.cast(context.get("parameters"));
        ModelNode node = nodeMap.get(rootNodeName);
        String trailName = trailNameExdr.expandString(context);
        String treeString = (String) context.get(trailName);
        if (UtilValidate.isEmpty(treeString)) {
            treeString = (String) parameters.get(trailName);
        }
        List<String> trail = null;
        if (UtilValidate.isNotEmpty(treeString)) {
            trail = StringUtil.split(treeString, "|");
            if (UtilValidate.isEmpty(trail)) {
                throw new RuntimeException("Tree 'trail' value is empty.");
            }
            context.put("rootEntityId", trail.get(0));
            context.put(getDefaultPkName(context), trail.get(0));
        } else {
            trail = new LinkedList<>();
        }
        context.put("targetNodeTrail", trail);
        context.put("currentNodeTrail", new LinkedList());
        try {
            node.renderNodeString(writer, context, treeStringRenderer, 0);
        } catch (IOException e2) {
            String errMsg = "Error rendering included label with name [" + getName() + "] : " + e2.toString();
            Debug.logError(e2, errMsg, MODULE);
            throw new RuntimeException(errMsg);
        }

    }

    /**
     * Gets default render style.
     * @return the default render style
     */
    public String getDefaultRenderStyle() {
        return defaultRenderStyle;
    }

    /**
     * Gets default wrap style exdr.
     * @return the default wrap style exdr
     */
    public FlexibleStringExpander getDefaultWrapStyleExdr() {
        return defaultWrapStyleExdr;
    }

    /**
     * Gets expand collapse request exdr.
     * @return the expand collapse request exdr
     */
    public FlexibleStringExpander getExpandCollapseRequestExdr() {
        return expandCollapseRequestExdr;
    }

    /**
     * Gets force child check.
     * @return the force child check
     */
    public boolean getForceChildCheck() {
        return forceChildCheck;
    }

    /**
     * Gets location.
     * @return the location
     */
    public String getLocation() {
        return location;
    }

    /**
     * Gets node map.
     * @return the node map
     */
    public Map<String, ModelNode> getNodeMap() {
        return nodeMap;
    }

    /**
     * Gets trail name exdr.
     * @return the trail name exdr
     */
    public FlexibleStringExpander getTrailNameExdr() {
        return trailNameExdr;
    }

    /**
     * Models the &lt;node&gt; element.
     * @see <code>widget-tree.xsd</code>
     */
    public static class ModelNode extends ModelWidget {

        private final List<ModelAction> actions;
        // TODO: Make this a generic condition object.
        private final ModelTreeCondition condition;
        private final String entityName;
        private final String entryName;
        private final String expandCollapseStyle;
        private final Label label;
        private final Link link;
        private final ModelTree modelTree;
        private final String pkName;
        private final String renderStyle;
        private final FlexibleStringExpander screenLocationExdr;
        private final FlexibleStringExpander screenNameExdr;
        private final String shareScope;
        private final List<ModelSubNode> subNodeList;
        private final FlexibleStringExpander wrapStyleExdr;

        public ModelNode(Element nodeElement, ModelTree modelTree) {
            super(nodeElement);
            this.modelTree = modelTree;
            this.expandCollapseStyle = nodeElement.getAttribute("expand-collapse-style");
            this.wrapStyleExdr = FlexibleStringExpander.getInstance(nodeElement.getAttribute("wrap-style"));
            this.renderStyle = nodeElement.getAttribute("render-style");
            this.entryName = nodeElement.getAttribute("entry-name");
            this.entityName = nodeElement.getAttribute("entity-name");
            this.pkName = nodeElement.getAttribute("join-field-name");
            ArrayList<ModelAction> actions = new ArrayList<>();
            // FIXME: Validate child elements, should be only one of actions, entity-one, service, script.
            Element actionsElement = UtilXml.firstChildElement(nodeElement, "actions");
            if (actionsElement != null) {
                actions.addAll(ModelTreeAction.readNodeActions(this, actionsElement));
            }
            Element actionElement = UtilXml.firstChildElement(nodeElement, "entity-one");
            if (actionElement != null) {
                actions.add(new AbstractModelAction.EntityOne(this, actionElement));
            }
            actionElement = UtilXml.firstChildElement(nodeElement, "service");
            if (actionElement != null) {
                actions.add(new ModelTreeAction.Service(this, actionElement));
            }
            actionElement = UtilXml.firstChildElement(nodeElement, "script");
            if (actionElement != null) {
                actions.add(new ModelTreeAction.Script(this, actionElement));
            }
            actions.trimToSize();
            this.actions = Collections.unmodifiableList(actions);
            Element screenElement = UtilXml.firstChildElement(nodeElement, "include-screen");
            if (screenElement != null) {
                this.screenNameExdr = FlexibleStringExpander.getInstance(screenElement.getAttribute("name"));
                this.screenLocationExdr = FlexibleStringExpander.getInstance(screenElement.getAttribute("location"));
                this.shareScope = screenElement.getAttribute("share-scope");
            } else {
                this.screenNameExdr = FlexibleStringExpander.getInstance("");
                this.screenLocationExdr = FlexibleStringExpander.getInstance("");
                this.shareScope = "";
            }
            Element labelElement = UtilXml.firstChildElement(nodeElement, "label");
            if (labelElement != null) {
                this.label = new Label(labelElement);
            } else {
                this.label = null;
            }
            Element linkElement = UtilXml.firstChildElement(nodeElement, "link");
            if (linkElement != null) {
                this.link = new Link(linkElement);
            } else {
                this.link = null;
            }
            Element conditionElement = UtilXml.firstChildElement(nodeElement, "condition");
            if (conditionElement != null) {
                this.condition = new ModelTreeCondition(modelTree, conditionElement);
            } else {
                this.condition = null;
            }
            List<? extends Element> nodeElements = UtilXml.childElementList(nodeElement, "sub-node");
            if (!nodeElements.isEmpty()) {
                List<ModelSubNode> subNodeList = new ArrayList<>();
                for (Element subNodeElementEntry : nodeElements) {
                    ModelSubNode subNode = new ModelSubNode(subNodeElementEntry, this);
                    subNodeList.add(subNode);
                }
                this.subNodeList = Collections.unmodifiableList(subNodeList);
            } else {
                this.subNodeList = Collections.emptyList();
            }
        }

        @Override
        public void accept(ModelWidgetVisitor visitor) throws Exception {
            visitor.visit(this);
        }

        private List<Object[]> getChildren(Map<String, Object> context) {
            List<Object[]> subNodeValues = new ArrayList<>();
            for (ModelSubNode subNode : subNodeList) {
                String nodeName = subNode.getNodeName(context);
                ModelNode node = modelTree.nodeMap.get(nodeName);
                List<ModelAction> subNodeActions = subNode.getActions();
                AbstractModelAction.runSubActions(subNodeActions, context);
                Iterator<? extends Map<String, ? extends Object>> dataIter = subNode.getListIterator(context);
                if (dataIter instanceof EntityListIterator) {
                    try (EntityListIterator eli = (EntityListIterator) dataIter) {
                        Map<String, Object> val = null;
                        while ((val = eli.next()) != null) {
                            Object[] arr = {node, val };
                            subNodeValues.add(arr);
                        }
                    } catch (GenericEntityException e) {
                        Debug.logError(e, MODULE);
                        throw new RuntimeException(e.getMessage());
                    }
                } else if (dataIter != null) {
                    while (dataIter.hasNext()) {
                        Map<String, ? extends Object> val = dataIter.next();
                        Object[] arr = {node, val };
                        subNodeValues.add(arr);
                    }
                }
            }
            return subNodeValues;
        }

        /**
         * Gets entity name.
         * @return the entity name
         */
        public String getEntityName() {
            if (!this.entityName.isEmpty()) {
                return this.entityName;
            }
            return this.modelTree.getDefaultEntityName();
        }

        /**
         * Gets entry name.
         * @return the entry name
         */
        public String getEntryName() {
            return this.entryName;
        }

        /**
         * Gets expand collapse style.
         * @return the expand collapse style
         */
        public String getExpandCollapseStyle() {
            return expandCollapseStyle;
        }

        /**
         * Gets model tree.
         * @return the model tree
         */
        public ModelTree getModelTree() {
            return this.modelTree;
        }

        /**
         * Gets pk name.
         * @param context the context
         * @return the pk name
         */
        public String getPkName(Map<String, Object> context) {
            if (UtilValidate.isNotEmpty(this.pkName)) {
                return this.pkName;
            }
            return this.modelTree.getDefaultPkName(context);
        }

        /**
         * Gets render style.
         * @return the render style
         */
        public String getRenderStyle() {
            if (this.renderStyle.isEmpty()) {
                return modelTree.getRenderStyle();
            }
            return this.renderStyle;
        }

        /**
         * Gets wrap style.
         * @param context the context
         * @return the wrap style
         */
        public String getWrapStyle(Map<String, Object> context) {
            String val = this.wrapStyleExdr.expandString(context);
            if (val.isEmpty()) {
                val = this.modelTree.getWrapStyle(context);
            }
            return val;
        }

        /**
         * Has children boolean.
         * @param context the context
         * @return the boolean
         */
        public boolean hasChildren(Map<String, Object> context) {
            List<Object[]> subNodeValues = getChildren(context);
            boolean hasChildren = false;
            Long nodeCount = null;
            String countFieldName = "childBranchCount";
            Object obj = null;
            if (!this.entryName.isEmpty()) {
                Map<String, Object> map = UtilGenerics.cast(context.get(this.entryName));
                if (map instanceof GenericValue) {
                    ModelEntity modelEntity = ((GenericValue) map).getModelEntity();
                    if (modelEntity.isField(countFieldName)) {
                        obj = map.get(countFieldName);
                    }
                }
            } else {
                obj = context.get(countFieldName);
            }
            if (obj != null) {
                nodeCount = (Long) obj;
            }
            String entName = this.getEntityName();
            Delegator delegator = WidgetWorker.getDelegator(context);
            ModelEntity modelEntity = delegator.getModelEntity(entName);
            ModelField modelField = null;
            if (modelEntity.isField(countFieldName)) {
                modelField = modelEntity.getField(countFieldName);
            }
            if (nodeCount == null && modelField != null || this.modelTree.forceChildCheck) {
                getChildren(context);
                nodeCount = (long) subNodeValues.size();
                String pkName = this.getPkName(context);
                String id = null;
                if (!this.entryName.isEmpty()) {
                    id = UtilGenerics.<Map<String, String>>cast(context.get(this.entryName)).get(pkName);
                } else {
                    id = (String) context.get(pkName);
                }
                try {
                    if (id != null && modelEntity.getPksSize() == 1) {
                        GenericValue entity = EntityQuery.use(delegator).from(entName).where(pkName, id).queryOne();
                        if (modelEntity.isField("childBranchCount")) {
                            entity.put("childBranchCount", nodeCount);
                            entity.store();
                        }
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                    throw new RuntimeException(e.getMessage());
                }
            } else if (nodeCount == null) {
                getChildren(context);
                nodeCount = (long) subNodeValues.size();
            }
            if (nodeCount.intValue() > 0) {
                hasChildren = true;
            }
            return hasChildren;
        }

        /**
         * Is expand collapse boolean.
         * @return the boolean
         */
        public boolean isExpandCollapse() {
            boolean isExpCollapse = false;
            String rStyle = getRenderStyle();
            if (rStyle != null && "expand-collapse".equals(rStyle)) {
                isExpCollapse = true;
            }
            return isExpCollapse;
        }

        /**
         * Is follow trail boolean.
         * @return the boolean
         */
        public boolean isFollowTrail() {
            boolean isFollowTrail = false;
            String rStyle = getRenderStyle();
            if (rStyle != null && ("follow-trail".equals(rStyle) || "show-peers".equals(rStyle) || "follow-trail".equals(rStyle))) {
                isFollowTrail = true;
            }
            return isFollowTrail;
        }

        /**
         * Is root node boolean.
         * @return the boolean
         */
        public boolean isRootNode() {
            return getName().equals(modelTree.getRootNodeName());
        }

        /**
         * Render node string.
         * @param writer the writer
         * @param context the context
         * @param treeStringRenderer the tree string renderer
         * @param depth the depth
         * @throws IOException the io exception
         * @throws GeneralException the general exception
         */
        public void renderNodeString(Appendable writer, Map<String, Object> context, TreeStringRenderer treeStringRenderer,
                int depth) throws IOException, GeneralException {
            boolean passed = true;
            if (this.condition != null) {
                if (!this.condition.getCondition().eval(context)) {
                    passed = false;
                }
            }
            if (passed) {
                Object obj = context.get("currentNodeTrail");
                List<String> currentNodeTrail = (obj instanceof List) ? UtilGenerics.cast(obj) : null;
                context.put("processChildren", Boolean.TRUE);
                // this action will usually obtain the "current" entity
                ModelTreeAction.runSubActions(this.actions, context);
                String pkName = getPkName(context);
                String id = null;
                if (!this.entryName.isEmpty()) {
                    id = UtilGenerics.<Map<String, String>>cast(context.get(this.entryName)).get(pkName);
                } else {
                    id = (String) context.get(pkName);
                }
                if (currentNodeTrail != null) {
                    currentNodeTrail.add(id);
                }
                treeStringRenderer.renderNodeBegin(writer, context, this, depth);
                try {
                    String screenName = null;
                    if (!screenNameExdr.isEmpty()) {
                        screenName = screenNameExdr.expandString(context);
                    }
                    String screenLocation = null;
                    if (!screenLocationExdr.isEmpty()) {
                        screenLocation = screenLocationExdr.expandString(context);
                    }
                    if (screenName != null && screenLocation != null) {
                        ScreenStringRenderer screenStringRenderer = treeStringRenderer.getScreenStringRenderer(context);
                        ModelScreen modelScreen = ScreenFactory.getScreenFromLocation(screenLocation, screenName);
                        modelScreen.renderScreenString(writer, context, screenStringRenderer);
                    }
                    if (label != null) {
                        label.renderLabelString(writer, context, treeStringRenderer);
                    }
                    if (link != null) {
                        link.renderLinkString(writer, context, treeStringRenderer);
                    }
                    treeStringRenderer.renderLastElement(writer, context, this);
                    Boolean processChildren = (Boolean) context.get("processChildren");
                    if (processChildren) {
                        List<Object[]> subNodeValues = getChildren(context);
                        int newDepth = depth + 1;
                        for (Object[] arr : subNodeValues) {
                            ModelNode node = (ModelNode) arr[0];
                            Map<String, Object> val = UtilGenerics.cast(arr[1]);
                            String thisPkName = node.getPkName(context);
                            String thisEntityId = (String) val.get(thisPkName);
                            MapStack<String> newContext = MapStack.create(context);
                            newContext.push();
                            String nodeEntryName = node.getEntryName();
                            if (!nodeEntryName.isEmpty()) {
                                newContext.put(nodeEntryName, val);
                            } else {
                                newContext.putAll(val);
                            }
                            String targetEntityId = null;
                            List<String> targetNodeTrail = UtilGenerics.cast(context.get("targetNodeTrail"));
                            if (newDepth < targetNodeTrail.size()) {
                                targetEntityId = targetNodeTrail.get(newDepth);
                            }
                            if ((targetEntityId != null && targetEntityId.equals(thisEntityId))
                                    || this.showPeers(newDepth, context)) {
                                node.renderNodeString(writer, newContext, treeStringRenderer, newDepth);
                            }
                        }
                    }
                } catch (ScreenRenderException | SAXException | ParserConfigurationException | IOException e) {
                    String errMsg = "Error rendering included label with name [" + getName() + "] : " + e.toString();
                    Debug.logError(e, errMsg, MODULE);
                    throw new RuntimeException(errMsg);
                }
                treeStringRenderer.renderNodeEnd(writer, context, this);
                int removeIdx = 0;
                if (currentNodeTrail != null) {
                    removeIdx = currentNodeTrail.size() - 1;
                    if (removeIdx >= 0) {
                        currentNodeTrail.remove(removeIdx);
                    }
                }
            }
        }

        /**
         * Show peers boolean.
         * @param currentDepth the current depth
         * @param context the context
         * @return the boolean
         */
        public boolean showPeers(int currentDepth, Map<String, Object> context) {
            int trailSize = 0;
            List<?> trail = UtilGenerics.cast(context.get("targetNodeTrail"));
            int openDepth = modelTree.getOpenDepth();
            int postTrailOpenDepth = modelTree.getPostTrailOpenDepth();
            if (trail != null) {
                trailSize = trail.size();
            }

            boolean showPeers = false;
            String rStyle = getRenderStyle();
            if (rStyle == null) {
                showPeers = true;
            } else if (!isFollowTrail()) {
                showPeers = true;
            } else if ((currentDepth < trailSize)
                    && ("show-peers".equals(rStyle) || "expand-collapse".equals(rStyle))) {
                showPeers = true;
            } else if (openDepth >= currentDepth) {
                showPeers = true;
            } else {
                int depthAfterTrail = currentDepth - trailSize;
                if (depthAfterTrail >= 0 && depthAfterTrail <= postTrailOpenDepth) {
                    showPeers = true;
                }
            }
            return showPeers;
        }

        /**
         * Gets actions.
         * @return the actions
         */
        public List<ModelAction> getActions() {
            return actions;
        }

        /**
         * Gets condition.
         * @return the condition
         */
        public ModelTreeCondition getCondition() {
            return condition;
        }

        /**
         * Gets label.
         * @return the label
         */
        public Label getLabel() {
            return label;
        }

        /**
         * Gets link.
         * @return the link
         */
        public Link getLink() {
            return link;
        }

        /**
         * Gets pk name.
         * @return the pk name
         */
        public String getPkName() {
            return pkName;
        }

        /**
         * Gets screen location exdr.
         * @return the screen location exdr
         */
        public FlexibleStringExpander getScreenLocationExdr() {
            return screenLocationExdr;
        }

        /**
         * Gets screen name exdr.
         * @return the screen name exdr
         */
        public FlexibleStringExpander getScreenNameExdr() {
            return screenNameExdr;
        }

        /**
         * Gets share scope.
         * @return the share scope
         */
        public String getShareScope() {
            return shareScope;
        }

        /**
         * Gets sub node list.
         * @return the sub node list
         */
        public List<ModelSubNode> getSubNodeList() {
            return subNodeList;
        }

        /**
         * Gets wrap style exdr.
         * @return the wrap style exdr
         */
        public FlexibleStringExpander getWrapStyleExdr() {
            return wrapStyleExdr;
        }

        /**
         * Models the &lt;image&gt; element.
         * @see <code>widget-tree.xsd</code>
         */
        public static class Image {

            private final FlexibleStringExpander borderExdr;
            private final FlexibleStringExpander heightExdr;
            private final FlexibleStringExpander idExdr;
            private final FlexibleStringExpander srcExdr;
            private final FlexibleStringExpander styleExdr;
            private final String urlMode;
            private final FlexibleStringExpander widthExdr;

            public Image(Element imageElement) {
                this.borderExdr = FlexibleStringExpander
                        .getInstance(UtilXml.checkEmpty(imageElement.getAttribute("border"), "0"));
                this.heightExdr = FlexibleStringExpander.getInstance(imageElement.getAttribute("height"));
                this.idExdr = FlexibleStringExpander.getInstance(imageElement.getAttribute("id"));
                this.srcExdr = FlexibleStringExpander.getInstance(imageElement.getAttribute("src"));
                this.styleExdr = FlexibleStringExpander.getInstance(imageElement.getAttribute("style"));
                this.urlMode = UtilXml.checkEmpty(imageElement.getAttribute("url-mode"), "content");
                this.widthExdr = FlexibleStringExpander.getInstance(imageElement.getAttribute("width"));
            }

            /**
             * Gets border.
             * @param context the context
             * @return the border
             */
            public String getBorder(Map<String, Object> context) {
                return this.borderExdr.expandString(context);
            }

            /**
             * Gets height.
             * @param context the context
             * @return the height
             */
            public String getHeight(Map<String, Object> context) {
                return this.heightExdr.expandString(context);
            }

            /**
             * Gets id.
             * @param context the context
             * @return the id
             */
            public String getId(Map<String, Object> context) {
                return this.idExdr.expandString(context);
            }

            /**
             * Gets src.
             * @param context the context
             * @return the src
             */
            public String getSrc(Map<String, Object> context) {
                return this.srcExdr.expandString(context);
            }

            /**
             * Gets style.
             * @param context the context
             * @return the style
             */
            public String getStyle(Map<String, Object> context) {
                return this.styleExdr.expandString(context);
            }

            /**
             * Gets url mode.
             * @return the url mode
             */
            public String getUrlMode() {
                return this.urlMode;
            }

            /**
             * Gets width.
             * @param context the context
             * @return the width
             */
            public String getWidth(Map<String, Object> context) {
                return this.widthExdr.expandString(context);
            }

            /**
             * Render image string.
             * @param writer the writer
             * @param context the context
             * @param treeStringRenderer the tree string renderer
             */
            public void renderImageString(Appendable writer, Map<String, Object> context, TreeStringRenderer treeStringRenderer) {
                try {
                    treeStringRenderer.renderImage(writer, context, this);
                } catch (IOException e) {
                    String errMsg = "Error rendering image with id [" + getId(context) + "]: " + e.toString();
                    Debug.logError(e, errMsg, MODULE);
                    throw new RuntimeException(errMsg);
                }
            }
        }

        /**
         * Models the &lt;label&gt; element.
         * @see <code>widget-tree.xsd</code>
         */
        public static final class Label {
            private final FlexibleStringExpander idExdr;
            private final FlexibleStringExpander styleExdr;
            private final FlexibleStringExpander textExdr;

            public Label(Element labelElement) {
                String textAttr = labelElement.getAttribute("text");
                String pcdata = UtilXml.checkEmpty(UtilXml.elementValue(labelElement), "");
                this.textExdr = FlexibleStringExpander.getInstance(textAttr + pcdata);
                this.idExdr = FlexibleStringExpander.getInstance(labelElement.getAttribute("id"));
                this.styleExdr = FlexibleStringExpander.getInstance(labelElement.getAttribute("style"));
            }

            public String getId(Map<String, Object> context) {
                return this.idExdr.expandString(context);
            }

            public String getStyle(Map<String, Object> context) {
                return this.styleExdr.expandString(context);
            }

            public String getText(Map<String, Object> context) {
                String text = this.textExdr.expandString(context);
                // FIXME: Encoding should be done by the renderer, not by the model.
                UtilCodec.SimpleEncoder simpleEncoder = (UtilCodec.SimpleEncoder) context.get("simpleEncoder");
                if (simpleEncoder != null) {
                    text = simpleEncoder.encode(text);
                }
                return text;
            }

            public void renderLabelString(Appendable writer, Map<String, Object> context, TreeStringRenderer treeStringRenderer) {
                try {
                    treeStringRenderer.renderLabel(writer, context, this);
                } catch (IOException e) {
                    String errMsg = "Error rendering label with id [" + getId(context) + "]: " + e.toString();
                    Debug.logError(e, errMsg, MODULE);
                    throw new RuntimeException(errMsg);
                }
            }

            public FlexibleStringExpander getIdExdr() {
                return idExdr;
            }

            public FlexibleStringExpander getStyleExdr() {
                return styleExdr;
            }

            public FlexibleStringExpander getTextExdr() {
                return textExdr;
            }
        }

        /**
         * Models the &lt;link&gt; element.
         * @see <code>widget-tree.xsd</code>
         */
        public static class Link {
            private final boolean encode;
            private final boolean fullPath;
            private final FlexibleStringExpander idExdr;
            private final Image image;
            private final String linkType;
            private final FlexibleStringExpander nameExdr;
            private final List<Parameter> parameterList;
            private final FlexibleStringExpander prefixExdr;
            private final boolean secure;
            private final FlexibleStringExpander styleExdr;
            private final FlexibleStringExpander targetExdr;
            private final FlexibleStringExpander targetWindowExdr;
            private final FlexibleStringExpander textExdr;
            private final FlexibleStringExpander titleExdr;
            private final String urlMode;

            public Link(Element linkElement) {
                this.encode = "true".equals(linkElement.getAttribute("encode"));
                this.fullPath = "true".equals(linkElement.getAttribute("full-path"));
                this.idExdr = FlexibleStringExpander.getInstance(linkElement.getAttribute("id"));
                Element imageElement = UtilXml.firstChildElement(linkElement, "image");
                if (imageElement != null) {
                    this.image = new Image(imageElement);
                } else {
                    this.image = null;
                }
                this.linkType = linkElement.getAttribute("link-type");
                this.nameExdr = FlexibleStringExpander.getInstance(linkElement.getAttribute("name"));
                List<? extends Element> parameterElementList = UtilXml.childElementList(linkElement, "parameter");
                if (!parameterElementList.isEmpty()) {
                    List<Parameter> parameterList = new ArrayList<>(parameterElementList.size());
                    for (Element parameterElement : parameterElementList) {
                        parameterList.add(new Parameter(parameterElement));
                    }
                    this.parameterList = Collections.unmodifiableList(parameterList);
                } else {
                    this.parameterList = Collections.emptyList();
                }
                this.prefixExdr = FlexibleStringExpander.getInstance(linkElement.getAttribute("prefix"));
                this.secure = "true".equals(linkElement.getAttribute("secure"));
                this.styleExdr = FlexibleStringExpander.getInstance(linkElement.getAttribute("style"));
                this.targetExdr = FlexibleStringExpander.getInstance(linkElement.getAttribute("target"));
                this.targetWindowExdr = FlexibleStringExpander.getInstance(linkElement.getAttribute("target-window"));
                this.textExdr = FlexibleStringExpander.getInstance(linkElement.getAttribute("text"));
                this.titleExdr = FlexibleStringExpander.getInstance(linkElement.getAttribute("title"));
                this.urlMode = UtilXml.checkEmpty(linkElement.getAttribute("link-type"), "intra-app");
            }

            // FIXME: Using a widget model in this way is an ugly hack.
            public Link(String style, String target, String text) {
                this(style, target, text, null);
            }

            // FIXME: Something to be replaced by a builder class, but allows us to quickly
            // build Links to represent nodes with parameters in a tree, rather that trying
            // to encode the parameters early in the link's target.
            public Link(String style, String target, String text, List<Parameter> parameterList) {
                this.encode = false;
                this.fullPath = false;
                this.idExdr = FlexibleStringExpander.getInstance("");
                this.image = null;
                this.linkType = "";
                this.nameExdr = FlexibleStringExpander.getInstance("");
                this.parameterList = parameterList != null ? Collections.unmodifiableList(parameterList) : Collections.emptyList();
                this.prefixExdr = FlexibleStringExpander.getInstance("");
                this.secure = false;
                this.styleExdr = FlexibleStringExpander.getInstance(style);
                this.targetExdr = FlexibleStringExpander.getInstance(target);
                this.targetWindowExdr = FlexibleStringExpander.getInstance("");
                this.textExdr = FlexibleStringExpander.getInstance(text);
                this.titleExdr = FlexibleStringExpander.getInstance("");
                this.urlMode = "intra-app";
            }

            /**
             * Gets encode.
             * @return the encode
             */
            public boolean getEncode() {
                return this.encode;
            }

            /**
             * Gets full path.
             * @return the full path
             */
            public boolean getFullPath() {
                return this.fullPath;
            }

            /**
             * Gets id.
             * @param context the context
             * @return the id
             */
            public String getId(Map<String, Object> context) {
                return this.idExdr.expandString(context);
            }

            /**
             * Gets image.
             * @return the image
             */
            public Image getImage() {
                return this.image;
            }

            /**
             * Gets link type.
             * @return the link type
             */
            public String getLinkType() {
                return this.linkType;
            }

            /**
             * Gets name.
             * @param context the context
             * @return the name
             */
            public String getName(Map<String, Object> context) {
                return this.nameExdr.expandString(context);
            }

            /**
             * Gets parameter map.
             * @param context the context
             * @return the parameter map
             */
            public Map<String, String> getParameterMap(Map<String, Object> context) {
                Map<String, String> fullParameterMap = new HashMap<>();
                /* leaving this here... may want to add it at some point like the hyperlink element:
                Map<String, String> addlParamMap = this.parametersMapAcsr.get(context);
                if (addlParamMap != null) {
                    fullParameterMap.putAll(addlParamMap);
                }
                */
                for (CommonWidgetModels.Parameter parameter : this.parameterList) {
                    fullParameterMap.put(parameter.getName(), parameter.getValue(context));
                }
                return fullParameterMap;
            }

            /**
             * Gets prefix.
             * @param context the context
             * @return the prefix
             */
            public String getPrefix(Map<String, Object> context) {
                return this.prefixExdr.expandString(context);
            }

            /**
             * Gets secure.
             * @return the secure
             */
            public boolean getSecure() {
                return this.secure;
            }

            /**
             * Gets style.
             * @param context the context
             * @return the style
             */
            public String getStyle(Map<String, Object> context) {
                return this.styleExdr.expandString(context);
            }

            /**
             * Gets target.
             * @param context the context
             * @return the target
             */
            public String getTarget(Map<String, Object> context) {
                UtilCodec.SimpleEncoder simpleEncoder = (UtilCodec.SimpleEncoder) context.get("simpleEncoder");
                if (simpleEncoder != null) {
                    return this.targetExdr.expandString(UtilCodec.HtmlEncodingMapWrapper.getHtmlEncodingMapWrapper(context,
                            simpleEncoder));
                }
                return this.targetExdr.expandString(context);
            }

            /**
             * Gets target window.
             * @param context the context
             * @return the target window
             */
            public String getTargetWindow(Map<String, Object> context) {
                return this.targetWindowExdr.expandString(context);
            }

            /**
             * Gets text.
             * @param context the context
             * @return the text
             */
            public String getText(Map<String, Object> context) {
                String text = this.textExdr.expandString(context);
                // FIXME: Encoding should be done by the renderer, not by the model.
                UtilCodec.SimpleEncoder simpleEncoder = (UtilCodec.SimpleEncoder) context.get("simpleEncoder");
                if (simpleEncoder != null) {
                    text = simpleEncoder.encode(text);
                }
                return text;
            }

            /**
             * Gets title.
             * @param context the context
             * @return the title
             */
            public String getTitle(Map<String, Object> context) {
                String title = this.titleExdr.expandString(context);
                // FIXME: Encoding should be done by the renderer, not by the model.
                UtilCodec.SimpleEncoder simpleEncoder = (UtilCodec.SimpleEncoder) context.get("simpleEncoder");
                if (simpleEncoder != null) {
                    title = simpleEncoder.encode(title);
                }
                return title;
            }

            /**
             * Gets url mode.
             * @return the url mode
             */
            public String getUrlMode() {
                return this.urlMode;
            }

            /**
             * Render link string.
             * @param writer the writer
             * @param context the context
             * @param treeStringRenderer the tree string renderer
             */
            public void renderLinkString(Appendable writer, Map<String, Object> context, TreeStringRenderer treeStringRenderer) {
                try {
                    treeStringRenderer.renderLink(writer, context, this);
                } catch (IOException e) {
                    String errMsg = "Error rendering link with id [" + getId(context) + "]: " + e.toString();
                    Debug.logError(e, errMsg, MODULE);
                    throw new RuntimeException(errMsg);
                }
            }

            /**
             * Gets id exdr.
             * @return the id exdr
             */
            public FlexibleStringExpander getIdExdr() {
                return idExdr;
            }

            /**
             * Gets name exdr.
             * @return the name exdr
             */
            public FlexibleStringExpander getNameExdr() {
                return nameExdr;
            }

            /**
             * Gets parameter list.
             * @return the parameter list
             */
            public List<Parameter> getParameterList() {
                return parameterList;
            }

            /**
             * Gets prefix exdr.
             * @return the prefix exdr
             */
            public FlexibleStringExpander getPrefixExdr() {
                return prefixExdr;
            }

            /**
             * Gets style exdr.
             * @return the style exdr
             */
            public FlexibleStringExpander getStyleExdr() {
                return styleExdr;
            }

            /**
             * Gets target exdr.
             * @return the target exdr
             */
            public FlexibleStringExpander getTargetExdr() {
                return targetExdr;
            }

            /**
             * Gets target window exdr.
             * @return the target window exdr
             */
            public FlexibleStringExpander getTargetWindowExdr() {
                return targetWindowExdr;
            }

            /**
             * Gets text exdr.
             * @return the text exdr
             */
            public FlexibleStringExpander getTextExdr() {
                return textExdr;
            }

            /**
             * Gets title exdr.
             * @return the title exdr
             */
            public FlexibleStringExpander getTitleExdr() {
                return titleExdr;
            }
        }

        /**
         * Models the &lt;sub-node&gt; element.
         * @see <code>widget-tree.xsd</code>
         */
        public static class ModelSubNode extends ModelWidget {

            private final List<ModelAction> actions;
            private final FlexibleStringExpander nodeNameExdr;
            private final ModelNode rootNode;
            private final String iteratorKey;

            public ModelSubNode(Element subNodeElement, ModelNode modelNode) {
                super(subNodeElement);
                this.rootNode = modelNode;
                this.nodeNameExdr = FlexibleStringExpander.getInstance(subNodeElement.getAttribute("node-name"));
                ArrayList<ModelAction> actions = new ArrayList<>();
                // FIXME: Validate child elements, should be only one of actions, entity-and, entity-condition, service, script.
                Element actionsElement = UtilXml.firstChildElement(subNodeElement, "actions");
                if (actionsElement != null) {
                    actions.addAll(ModelTreeAction.readSubNodeActions(this, actionsElement));
                }
                Element actionElement = UtilXml.firstChildElement(subNodeElement, "entity-and");
                if (actionElement != null) {
                    actions.add(new ModelTreeAction.EntityAnd(this, actionElement));
                }
                actionElement = UtilXml.firstChildElement(subNodeElement, "service");
                if (actionElement != null) {
                    actions.add(new ModelTreeAction.Service(this, actionElement));
                }
                actionElement = UtilXml.firstChildElement(subNodeElement, "entity-condition");
                if (actionElement != null) {
                    actions.add(new ModelTreeAction.EntityCondition(this, actionElement));
                }
                actionElement = UtilXml.firstChildElement(subNodeElement, "script");
                if (actionElement != null) {
                    actions.add(new ModelTreeAction.Script(this, actionElement));
                }
                actions.trimToSize();
                this.actions = Collections.unmodifiableList(actions);
                this.iteratorKey = this.rootNode.getName().concat(".").concat(this.nodeNameExdr.getOriginal())
                        .concat(".ITERATOR");
            }

            @Override
            public void accept(ModelWidgetVisitor visitor) throws Exception {
                visitor.visit(this);
            }

            /**
             * Gets actions.
             * @return the actions
             */
            public List<ModelAction> getActions() {
                return actions;
            }

            /**
             * Gets list iterator.
             * @param context the context
             * @return the list iterator
             */
            @SuppressWarnings("unchecked")
            public ListIterator<? extends Map<String, ? extends Object>> getListIterator(Map<String, Object> context) {
                return (ListIterator<? extends Map<String, ? extends Object>>) context.get(this.iteratorKey);
            }

            /**
             * Gets node.
             * @return the node
             */
            public ModelTree.ModelNode getNode() {
                return this.rootNode;
            }

            /**
             * Gets node name.
             * @param context the context
             * @return the node name
             */
            public String getNodeName(Map<String, Object> context) {
                return this.nodeNameExdr.expandString(context);
            }

            /**
             * Gets node name exdr.
             * @return the node name exdr
             */
            public FlexibleStringExpander getNodeNameExdr() {
                return nodeNameExdr;
            }

            /**
             * Sets list iterator.
             * @param iter the iter
             * @param context the context
             */
            public void setListIterator(ListIterator<? extends Map<String, ? extends Object>> iter, Map<String, Object> context) {
                context.put(this.iteratorKey, iter);
            }
        }
    }
}
