<#--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

<#macro renderField text=""><#if text??>${text?xml}</#if></#macro>

<#macro renderFormatListWrapperOpen formName style columnStyles><${formName}Export></#macro>

<#macro renderFormatListWrapperClose formName></${formName}Export></#macro>

<#macro renderFormatItemRowOpen formName itemIndex altRowStyles evenRowStyle oddRowStyle><${formName}></#macro>

<#macro renderFormatItemRowClose formName></${formName}></#macro>

<#macro renderFormatItemRowCellOpen fieldName style positionSpan><${fieldName}></#macro>

<#macro renderFormatItemRowCellClose fieldName></${fieldName}></#macro>

<#macro renderFormatSingleWrapperOpen formName style><${formName}Export></#macro>

<#macro renderFormatSingleWrapperClose formName></${formName}Export></#macro>

<#macro renderDisplayField type imageLocation idName description title class alert inPlaceEditorUrl="" inPlaceEditorParams="">
<@renderField description/>
</#macro>
<#macro renderHyperlinkField></#macro>

<#macro renderTextField type pattern name className alert value="" textSize="" maxlength="" id="" event="" action=""
disabled=false clientAutocomplete="" ajaxUrl="" ajaxEnabled="" mask="" tabindex="" readonly="" required=false
placeholder="" delegatorName="default"><@renderField value/></#macro>

<#macro renderTextareaField name className alert cols="" rows="" maxlength="" id="" readonly="" value="" visualEditorEnable="" buttons="" tabindex="" language="" disabled=false placeholder=""><@renderField value/></#macro>

<#macro renderDateTimeField name className timeDropdownParamName defaultDateTimeString localizedIconTitle timeHourName timeMinutesName ampmName compositeType alert=false isTimeType=false isDateType=false amSelected=false pmSelected=false timeDropdown="" classString="" isTwelveHour=false hour1="" hour2="" minutes=0 shortDateInput="" title="" value="" size="" maxlength="" id="" formName="" mask="" event="" action="" step="" timeValues="" tabindex="" disabled=false isXMLHttpRequest=false><@renderField value/></#macro>

<#macro renderDropDownField name className id formName explicitDescription options ajaxEnabled
        otherFieldName="" otherValue="" otherFieldSize=""
        alert="" conditionGroup="" tabindex="" multiple=false event="" size="" placeCurrentValueAsFirstOption=false
        currentValue="" allowEmpty=false dDFCurrent="" noCurrentSelectedKey="" disabled=false action="">
</#macro>

<#macro renderCheckField items className alert id currentValue name event action conditionGroup tabindex disabled allChecked=""></#macro>
<#macro renderRadioField items className alert currentValue noCurrentSelectedKey name event action conditionGroup tabindex disabled></#macro>

<#macro renderSubmitField buttonType className alert formName action imgSrc ajaxUrl id title="" name="" event="" confirmation="" containerId="" tabindex="" disabled=false closeOnSubmit="true"></#macro>
<#macro renderResetField className alert name title></#macro>

<#macro renderHiddenField name conditionGroup="" value="" id="" event="" action="" disabled=false></#macro>
<#macro renderIgnoredField></#macro>

<#macro renderFieldTitle style title id fieldHelpText="" for=""></#macro>
<#macro renderEmptyFormDataMessage message></#macro>
<#macro renderSingleFormFieldTitle></#macro>

<#macro renderFormOpen linkUrl formType targetWindow containerId containerStyle autocomplete name viewIndexField viewSizeField viewIndex viewSize useRowSubmit focusFieldName hasRequiredField csrfNameValue></#macro>
<#macro renderFormClose></#macro>
<#macro renderMultiFormClose></#macro>

<#macro renderFormatHeaderOpen></#macro>
<#macro renderFormatHeaderClose></#macro>
<#macro renderFormatHeaderRowOpen style></#macro>
<#macro renderFormatHeaderRowClose>
</#macro>
<#macro renderFormatHeaderRowCellOpen style positionSpan></#macro>
<#macro renderFormatHeaderRowCellClose></#macro>

<#macro renderFormatHeaderRowFormCellOpen style></#macro>
<#macro renderFormatHeaderRowFormCellClose></#macro>
<#macro renderFormatHeaderRowFormCellTitleSeparator style isLast></#macro>

<#macro renderFormatItemRowFormCellOpen style></#macro>
<#macro renderFormatItemRowFormCellClose></#macro>

<#macro renderFormatFieldRowOpen></#macro>
<#macro renderFormatFieldRowClose></#macro>
<#macro renderFormatFieldRowTitleCellOpen style></#macro>
<#macro renderFormatFieldRowTitleCellClose></#macro>
<#macro renderFormatFieldRowSpacerCell></#macro>
<#macro renderFormatFieldRowWidgetCellOpen positionSpan style></#macro>
<#macro renderFormatFieldRowWidgetCellClose></#macro>

<#macro renderFormatEmptySpace> </#macro>

<#macro renderTextFindField name value defaultOption opEquals opBeginsWith opContains opIsEmpty opNotEqual className alert size maxlength autocomplete titleStyle hideIgnoreCase ignCase ignoreCase tabindex><@renderField value/></#macro>

<#macro renderDateFindField id name formName defaultOptionFrom defaultOptionThru opEquals opSameDay opGreaterThanFromDayStart opGreaterThan opGreaterThan opLessThan opUpToDay opUpThruDay opIsEmpty className="" alert=false imgSrc="" value="" isTimeType=false isDateType=false conditionGroup="" localizedInputTitle="" value2="" size="" maxlength="" titleStyle="" tabindex="" disabled=false></#macro>

<#macro renderRangeFindField className alert name value size maxlength autocomplete titleStyle defaultOptionFrom opEquals opGreaterThan opGreaterThanEquals opLessThan opLessThanEquals value2 defaultOptionThru conditionGroup tabindex>
</#macro>

<#macro renderLookupField name formName fieldFormName conditionGroup className="" alert="false" value="" size="" maxlength="" id="" event="" action="" readonly=false autocomplete="" descriptionFieldName="" targetParameterIter="" imgSrc="" ajaxUrl="" ajaxEnabled=javaScriptEnabled presentation="layer" width="" height="" position="" fadeBackground="true" clearText="" showDescription="" initiallyCollapsed="" lastViewName="main" tabindex="" delegatorName="default">></#macro>
<#macro renderNextPrev paginateStyle paginateFirstStyle viewIndex highIndex listSize viewSize ajaxEnabled javaScriptEnabled ajaxFirstUrl firstUrl paginateFirstLabel paginatePreviousStyle ajaxPreviousUrl previousUrl paginatePreviousLabel pageLabel ajaxSelectUrl selectUrl ajaxSelectSizeUrl selectSizeUrl commonDisplaying paginateNextStyle ajaxNextUrl nextUrl paginateNextLabel paginateLastStyle ajaxLastUrl lastUrl paginateLastLabel paginateViewSizeLabel></#macro>
<#macro renderFileField className alert name value size maxlength autocomplete tabindex></#macro>
<#macro renderPasswordField className alert name value size maxlength id autocomplete tabindex></#macro>
<#macro renderImageField value description alternate style event action></#macro>
<#macro renderBanner style leftStyle rightStyle leftText text rightText></#macro>
<#macro renderContainerField id className></#macro>
<#macro renderFieldGroupOpen style id title collapsed collapsibleAreaId collapsible expandToolTip collapseToolTip></#macro>
<#macro renderFieldGroupClose style id title></#macro>

<#macro renderHyperlinkTitle name title showSelectAll="N"></#macro>
<#macro renderSortField style title linkUrl ajaxEnabled tooltip=""></#macro>
<#macro formatBoundaryComment boundaryType widgetType widgetName></#macro>
<#macro makeHiddenFormLinkForm actionUrl name parameters targetWindow></#macro>
<#macro makeHiddenFormLinkAnchor linkStyle hiddenFormName event action imgSrc description confirmation><@renderField description /></#macro>
<#macro makeHyperlinkString hiddenFormName imgSrc imgTitle title alternate linkUrl description text="" linkStyle="" event="" action="" targetParameters="" targetWindow="" confirmation="" uniqueItemName="" height="" width="" id=""><@renderField description /></#macro>
<#macro renderTooltip tooltip tooltipStyle></#macro>
<#macro renderAsterisks requiredField></#macro>
<#macro renderDateRangePicker className alert id name value formName event action locale alwaysShowCalendars applyButtonClasses applyLabel autoApply buttonClasses cancelButtonClasses cancelLabel clearTitle
drops linkedCalendars maxSpan maxYear minYear opens rangeLastMonthLabel rangeLastWeekLabel rangeNextMonthLabel rangeNextWeekLabel rangeThisMonthLabel rangeThisWeekLabel showDropdowns showIsoWeekNumbers showRanges showWeekNumbers
singleDatePicker timePicker timePicker24Hour timePickerIncrement timePickerSeconds conditionGroup="" value2="" titleStyle="" tabindex=""></#macro>
