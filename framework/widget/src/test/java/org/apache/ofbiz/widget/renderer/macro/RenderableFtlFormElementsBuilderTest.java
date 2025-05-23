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
package org.apache.ofbiz.widget.renderer.macro;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import org.apache.ofbiz.webapp.control.ConfigXMLReader;
import org.apache.ofbiz.webapp.control.RequestHandler;
import org.apache.ofbiz.widget.content.StaticContentUrlProvider;
import org.apache.ofbiz.widget.model.ModelForm;
import org.apache.ofbiz.widget.model.ModelFormField;
import org.apache.ofbiz.widget.model.ModelScreenWidget;
import org.apache.ofbiz.widget.model.ModelTheme;
import org.apache.ofbiz.widget.renderer.VisualTheme;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtlMacroCall;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtl;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtlNoop;
import org.junit.Test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class RenderableFtlFormElementsBuilderTest {

    @Injectable
    private VisualTheme visualTheme;

    @Injectable
    private RequestHandler requestHandler;

    @Injectable
    private HttpServletRequest request;

    @Injectable
    private HttpServletResponse response;

    @Injectable
    private StaticContentUrlProvider staticContentUrlProvider;

    @Mocked
    private HttpSession httpSession;

    @Mocked
    private ModelTheme modelTheme;

    @Mocked
    private ModelFormField.ContainerField containerField;

    @Mocked
    private ModelFormField modelFormField;

    @Tested
    private RenderableFtlFormElementsBuilder renderableFtlFormElementsBuilder;

    @Test
    public void emptyLabelUsesNoopMacro(@Mocked ModelScreenWidget.Label label) {
        new Expectations() {
            {
                label.getText(withNotNull());
                result = "";
            }
        };

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.label(ImmutableMap.of(), label);
        assertThat(renderableFtl, equalTo(RenderableFtlNoop.INSTANCE));
    }

    @Test
    public void labelMacroCallUsesText(@Mocked final ModelScreenWidget.Label label) {
        new Expectations() {
            {
                label.getText(withNotNull());
                result = "TEXT";
            }
        };

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.label(ImmutableMap.of(), label);
        assertThat(renderableFtl,
                MacroCallMatcher.hasNameAndParameters("renderLabel",
                        MacroCallParameterMatcher.hasNameAndStringValue("text", "TEXT")));
    }

    @Test
    public void displayFieldMacroUsesType(@Mocked final ModelFormField.DisplayField displayField) {
        new Expectations() {
            {
                displayField.getType();
                result = "TYPE";

                displayField.getDescription(withNotNull());
                result = "DESCRIPTION";
            }
        };

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.displayField(ImmutableMap.of(),
                displayField, false);
        assertThat(renderableFtl,
                MacroCallMatcher.hasNameAndParameters("renderDisplayField",
                        MacroCallParameterMatcher.hasNameAndStringValue("type", "TYPE")));
    }

    @Test
    public void containerMacroCallUsesContainerId() {
        new Expectations() {
            {
                modelFormField.getCurrentContainerId(withNotNull());
                result = "CurrentContainerId";
            }
        };

        final RenderableFtlMacroCall macroCall = renderableFtlFormElementsBuilder.containerMacroCall(ImmutableMap.of(), containerField);
        assertThat(macroCall,
                MacroCallMatcher.hasNameAndParameters("renderContainerField",
                        MacroCallParameterMatcher.hasNameAndStringValue("id", "CurrentContainerId")));
    }

    @Test
    public void basicAnchorLinkCreatesMacroCall(@Mocked final ModelFormField.SubHyperlink subHyperlink) {

        final Map<String, ConfigXMLReader.RequestMap> requestMapMap = new HashMap<>();

        new Expectations() {
            {
                subHyperlink.getStyle(withNotNull());
                result = "TestLinkStyle";

                subHyperlink.getUrlMode();
                result = "url-mode";

                subHyperlink.shouldUse(withNotNull());
                result = true;

                subHyperlink.getDescription(withNotNull());
                result = "LinkDescription";

                subHyperlink.getTarget(withNotNull());
                result = "/link/target/path";

                request.getAttribute("requestMapMap");
                result = requestMapMap;
            }
        };

        final RenderableFtl linkElement =
                renderableFtlFormElementsBuilder.makeHyperlinkString(subHyperlink, new HashMap<>());
        assertThat(linkElement,
                MacroCallMatcher.hasNameAndParameters("makeHyperlinkString",
                        MacroCallParameterMatcher.hasNameAndStringValue("linkStyle", "TestLinkStyle"),
                        MacroCallParameterMatcher.hasNameAndStringValue("linkUrl", "/link/target/path")));
    }

    @Test
    public void textFieldSetsIdValueAndLength(@Mocked final ModelFormField.TextField textField) {
        final int maxLength = 42;
        new Expectations() {
            {
                modelFormField.getCurrentContainerId(withNotNull());
                result = "CurrentTextId";

                modelFormField.getEntry(withNotNull(), anyString);
                result = "TEXTVALUE";

                textField.getMaxlength();
                result = maxLength;

                httpSession.getAttribute("delegatorName");
                result = "DelegatorName";
            }
        };

        final HashMap<String, Object> context = new HashMap<>();
        context.put("session", httpSession);

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.textField(context, textField, true);
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters("renderTextField",
                MacroCallParameterMatcher.hasNameAndStringValue("id", "CurrentTextId"),
                MacroCallParameterMatcher.hasNameAndStringValue("value", "TEXTVALUE"),
                MacroCallParameterMatcher.hasNameAndStringValue("maxlength", Integer.toString(maxLength))));

    }

    @Test
    public void textFieldCreatesAjaxUrl(@Mocked final ModelFormField.TextField textField) {

        final List<ModelForm.UpdateArea> updateAreas = ImmutableList.of(
                new ModelForm.UpdateArea("change", "areaId1", "target1?param1=${param1}&param2=ThisIsParam2"),
                new ModelForm.UpdateArea("change", "areaId2", "target2"));
        new Expectations() {
            {
                modelFormField.getOnChangeUpdateAreas();
                result = updateAreas;

                requestHandler.makeLink(request, response, "target1");
                result = "http://host.domain/target1";

                requestHandler.makeLink(request, response, "target2");
                result = "http://host.domain/target2";

                httpSession.getAttribute("delegatorName");
                result = "DelegatorName";
            }
        };

        final HashMap<String, Object> context = new HashMap<>();
        context.put("param1", "ThisIsParam1");
        context.put("session", httpSession);

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.textField(context, textField, true);
        assertThat(renderableFtl, MacroCallMatcher.hasName("renderTextField"));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(
                MacroCallParameterMatcher.hasNameAndStringValue("ajaxUrl",
                        "areaId1,http://host.domain/target1,param1=ThisIsParam1&param2=ThisIsParam2,"
                                + "areaId2,http://host.domain/target2,")));
    }

    @Test
    public void textFieldDefaultTypeIsText(@Mocked final ModelFormField.TextField textField) {
        new Expectations() {
            {
                httpSession.getAttribute("delegatorName"); result = "DelegatorName";
            }
        };

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.textField(Map.of("session", httpSession), textField, true);
        assertThat(renderableFtl, MacroCallMatcher.hasName("renderTextField"));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("type", "text")));
    }

    @Test
    public void textFieldTypeNumber(@Mocked final ModelFormField.TextField textField) {
        new Expectations() {
            {
                textField.getType(); result = "number";
                httpSession.getAttribute("delegatorName"); result = "DelegatorName";
            }
        };

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.textField(Map.of("session", httpSession), textField, true);
        assertThat(renderableFtl, MacroCallMatcher.hasName("renderTextField"));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("type", "number")));
    }

    @Test
    public void textFieldTypeEmail(@Mocked final ModelFormField.TextField textField) {
        new Expectations() {
            {
                textField.getType(); result = "email";
                httpSession.getAttribute("delegatorName"); result = "DelegatorName";
            }
        };

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.textField(Map.of("session", httpSession), textField, true);
        assertThat(renderableFtl, MacroCallMatcher.hasName("renderTextField"));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("type", "email")));
    }

    @Test
    public void textFieldTypeUrl(@Mocked final ModelFormField.TextField textField) {
        new Expectations() {
            {
                textField.getType(); result = "url";
                httpSession.getAttribute("delegatorName"); result = "DelegatorName";
            }
        };

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.textField(Map.of("session", httpSession), textField, true);
        assertThat(renderableFtl, MacroCallMatcher.hasName("renderTextField"));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("type", "url")));
    }

    @Test
    public void textFieldTypeTel(@Mocked final ModelFormField.TextField textField) {
        new Expectations() {
            {
                textField.getType(); result = "tel";
                httpSession.getAttribute("delegatorName"); result = "DelegatorName";
            }
        };

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.textField(Map.of("session", httpSession), textField, true);
        assertThat(renderableFtl, MacroCallMatcher.hasName("renderTextField"));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("type", "tel")));
    }

    @Test
    public void textFieldNotRequired(@Mocked final ModelFormField.TextField textField) {
        new Expectations() {
            {
                modelFormField.getRequiredField(); result = false;
                httpSession.getAttribute("delegatorName"); result = "DelegatorName";
            }
        };

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.textField(Map.of("session", httpSession), textField, true);
        assertThat(renderableFtl, MacroCallMatcher.hasName("renderTextField"));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndBooleanValue("required", false)));
    }

    @Test
    public void textFieldRequiredWithoutRequiredStyle(@Mocked final ModelFormField.TextField textField) {
        new Expectations() {
            {
                modelFormField.getModelForm().getType(); result = "single";
                modelFormField.getRequiredField(); result = true;
                httpSession.getAttribute("delegatorName"); result = "DelegatorName";
            }
        };

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.textField(Map.of("session", httpSession), textField, true);
        assertThat(renderableFtl, MacroCallMatcher.hasName("renderTextField"));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndBooleanValue("required", true)));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("className", "required")));
    }

    @Test
    public void textFieldRequiredWithRequiredStyle(@Mocked final ModelFormField.TextField textField) {
        new Expectations() {
            {
                modelFormField.getModelForm().getType(); result = "single";
                modelFormField.getRequiredField(); result = true;
                modelFormField.getRequiredFieldStyle(); result = "someCssClass";
                httpSession.getAttribute("delegatorName"); result = "DelegatorName";
            }
        };

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.textField(Map.of("session", httpSession), textField, true);
        assertThat(renderableFtl, MacroCallMatcher.hasName("renderTextField"));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndBooleanValue("required", true)));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(
                MacroCallParameterMatcher.hasNameAndStringValue("className", "required someCssClass")));
    }

    @Test
    public void textFieldPattern(@Mocked final ModelFormField.TextField textField) {
        new Expectations() {
            {
                textField.getPattern(); result = "\\d{4,4}";
                httpSession.getAttribute("delegatorName"); result = "DelegatorName";
            }
        };

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.textField(Map.of("session", httpSession), textField, true);
        assertThat(renderableFtl, MacroCallMatcher.hasName("renderTextField"));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("pattern", "\\d{4,4}")));
    }

    @Test
    public void textFieldPatternOnInvalidType(@Mocked final ModelFormField.TextField textField) {
        new Expectations() {
            {
                textField.getPattern(); result = "\\d{4,4}";
                textField.getType(); result = "number";
                httpSession.getAttribute("delegatorName"); result = "DelegatorName";
            }
        };

        textField.getPattern();
        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.textField(Map.of("session", httpSession), textField, true);

        assertThat(renderableFtl, MacroCallMatcher.hasName("renderTextField"));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("pattern", "")));
    }

    @Test
    public void textareaFieldSetsIdValueLengthAndSize(@Mocked final ModelFormField.TextareaField textareaField) {
        final int maxLength = 142;
        final int cols = 80;
        final int rows = 5;
        new Expectations() {
            {
                modelFormField.getCurrentContainerId(withNotNull());
                result = "CurrentTextareaId";

                modelFormField.getEntry(withNotNull(), anyString);
                result = "TEXTAREAVALUE";

                textareaField.getMaxlength();
                result = maxLength;

                textareaField.getCols();
                result = cols;

                textareaField.getRows();
                result = rows;
            }
        };

        final HashMap<String, Object> context = new HashMap<>();

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.textArea(context, textareaField);
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters("renderTextareaField",
                MacroCallParameterMatcher.hasNameAndStringValue("id", "CurrentTextareaId"),
                MacroCallParameterMatcher.hasNameAndStringValue("value", "TEXTAREAVALUE"),
                MacroCallParameterMatcher.hasNameAndIntegerValue("cols", cols),
                MacroCallParameterMatcher.hasNameAndIntegerValue("rows", rows),
                MacroCallParameterMatcher.hasNameAndIntegerValue("maxlength", maxLength)));
    }

    @Test
    public void textareaFieldSetsDisabledParameters(@Mocked final ModelFormField.TextareaField textareaField) {
        new Expectations() {
            {
                modelFormField.getDisabled(withNotNull());
                result = true;
            }
        };

        final HashMap<String, Object> context = new HashMap<>();

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.textArea(context, textareaField);
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters("renderTextareaField",
                MacroCallParameterMatcher.hasNameAndBooleanValue("disabled", true)));
    }

    @Test
    public void textareaFieldVisualEditorEnabledNoButtons(@Mocked final ModelFormField.TextareaField textareaField) {
        new Expectations() {
            {
                modelFormField.getDisabled(withNotNull());
                result = true;

                textareaField.getVisualEditorEnable();
                result = true;

                textareaField.getVisualEditorButtons(withNotNull());
                result = "";
            }
        };

        final HashMap<String, Object> context = new HashMap<>();

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.textArea(context, textareaField);
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters("renderTextareaField",
                MacroCallParameterMatcher.hasNameAndBooleanValue("visualEditorEnable", true)));
    }

    @Test
    public void textareaFieldVisualEditorEnabledButtons(@Mocked final ModelFormField.TextareaField textareaField) {
        String editorConfiguration = "[['formatting'],['strong','em','del'],['link'],['unorderedList','orderedList'],"
                + "['horizontalRule'],['removeformat'],['indent','outdent'],['fullscreen']]";

        new Expectations() {
            {
                modelFormField.getDisabled(withNotNull());
                result = true;

                textareaField.getVisualEditorEnable();
                result = true;

                textareaField.getVisualEditorButtons(withNotNull());
                result = editorConfiguration;
            }
        };

        final HashMap<String, Object> context = new HashMap<>();

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.textArea(context, textareaField);
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters("renderTextareaField",
                MacroCallParameterMatcher.hasNameAndBooleanValue("visualEditorEnable", true)));
        assertThat(renderableFtl, MacroCallMatcher.hasNameAndParameters(
                "renderTextareaField",
                MacroCallParameterMatcher.hasNameAndStringValue("buttons", editorConfiguration)));
    }

    @Test
    public void fieldGroupOpenRendersCollapsibleAreaId(@Mocked final ModelForm.FieldGroup fieldGroup) {
        new Expectations() {
            {
                fieldGroup.getStyle();
                result = "GROUPSTYLE";

                fieldGroup.getTitle();
                result = "TITLE${title}";

                fieldGroup.getId();
                result = "FIELDGROUPID";

                fieldGroup.initiallyCollapsed();
                result = true;
            }
        };

        final Map<String, Object> context = new HashMap<>();
        context.put("title", "ABC");

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.fieldGroupOpen(context, fieldGroup);
        assertThat(renderableFtl, MacroCallMatcher.hasName("renderFieldGroupOpen"));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("title", "TITLEABC")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("style", "GROUPSTYLE")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue(
                "collapsibleAreaId", "FIELDGROUPID_body")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndBooleanValue("collapsed", true)));
    }

    @Test
    public void fieldGroupCloseRendersStyle(@Mocked final ModelForm.FieldGroup fieldGroup) {
        new Expectations() {
            {
                fieldGroup.getStyle();
                result = "GROUPSTYLE";
            }
        };

        final Map<String, Object> context = new HashMap<>();
        context.put("title", "ABC");

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.fieldGroupClose(context, fieldGroup);
        assertThat(renderableFtl, MacroCallMatcher.hasName("renderFieldGroupClose"));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("style", "GROUPSTYLE")));
    }

    @Test
    public void dateRangePickerField(@Mocked final ModelFormField.DateRangePickerField dateRangePickerField) {
        new Expectations() {
            {
                dateRangePickerField.getAlwaysShowCalendars(); result = true;
                dateRangePickerField.getApplyButtonClasses(withNotNull()); result = "abc";
                dateRangePickerField.getApplyLabel(withNotNull()); result = "al";
                dateRangePickerField.getAutoApply(); result = false;
                dateRangePickerField.getButtonClasses(withNotNull()); result = "bc";
                dateRangePickerField.getCancelButtonClasses(withNotNull()); result = "cbc";
                dateRangePickerField.getCancelLabel(withNotNull()); result = "cl";
                dateRangePickerField.getClearTitle(withNotNull()); result = "ct";
                dateRangePickerField.getDrops(); result = "down";
                dateRangePickerField.getLinkedCalendars(); result = false;
                dateRangePickerField.getMaxSpan(); result = 9;
                dateRangePickerField.getMaxYear(); result = 2100;
                dateRangePickerField.getMinYear(); result = 1900;
                dateRangePickerField.getOpens(); result = "right";
                dateRangePickerField.getShowDropdowns(); result = true;
                dateRangePickerField.getShowIsoWeekNumbers(); result = true;
                dateRangePickerField.getShowRanges(); result = true;
                dateRangePickerField.getShowWeekNumbers(); result = true;
                dateRangePickerField.getSingleDatePicker(); result = false;
                dateRangePickerField.getTimePicker(); result = true;
                dateRangePickerField.getTimePicker24Hour(); result = true;
                dateRangePickerField.getTimePickerIncrement(); result = 5;
                dateRangePickerField.getTimePickerSeconds(); result = true;
            }
        };

        final Map<String, Object> context = new HashMap<>();
        context.put("locale", new Locale("fr"));

        final RenderableFtl renderableFtl = renderableFtlFormElementsBuilder.dateRangePicker(context, dateRangePickerField);
        assertThat(renderableFtl, MacroCallMatcher.hasName("renderDateRangePicker"));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndBooleanValue("alwaysShowCalendars", true)));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("applyButtonClasses", "abc")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("applyLabel", "al")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndBooleanValue("autoApply", false)));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("buttonClasses", "bc")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("cancelButtonClasses", "cbc")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("cancelLabel", "cl")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("clearTitle", "ct")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("drops", "down")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndBooleanValue("linkedCalendars", false)));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("maxSpan", "9")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("maxYear", "2100")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("minYear", "1900")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("opens", "right")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndBooleanValue("showDropdowns", true)));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndBooleanValue("showIsoWeekNumbers", true)));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndBooleanValue("showRanges", true)));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndBooleanValue("showWeekNumbers", true)));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndBooleanValue("singleDatePicker", false)));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndBooleanValue("timePicker", true)));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndBooleanValue("timePicker24Hour", true)));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndStringValue("timePickerIncrement", "5")));
        assertThat(renderableFtl, MacroCallMatcher.hasParameters(MacroCallParameterMatcher.hasNameAndBooleanValue("timePickerSeconds", true)));
    }
}
