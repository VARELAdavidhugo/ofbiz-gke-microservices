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

<#macro renderField text="">
  <#if text??>
    ${text}<#lt/>
  </#if>
</#macro>

<#macro renderDisplayField imageLocation alert type="" idName="" description="" title="" class="" inPlaceEditorUrl="" inPlaceEditorParams="">
  <#if type?has_content && type=="image">
    <img src="${imageLocation}" alt=""><#lt/>
  <#else>
    <#if inPlaceEditorUrl?has_content || class?has_content || alert=="true" || title?has_content>
      <span <#if inPlaceEditorUrl?has_content>data-inplace-editor-url="${inPlaceEditorUrl}" data-inplace-editor-params="${inPlaceEditorParams}"</#if> <#if idName?has_content>id="cc_${idName}"</#if> <#if title?has_content>title="${title}"</#if> <@renderClass class alert />><#t/>
    </#if>

    <#if description?has_content>
      ${description?replace("\n", "<br />")}<#t/>
    <#else>
      &nbsp;<#t/>
    </#if>
    <#if inPlaceEditorUrl?has_content || class?has_content || alert=="true">
      </span><#lt/>
    </#if>
  </#if>
</#macro>
<#macro renderHyperlinkField></#macro>

<#macro renderTextField type pattern name className alert value="" textSize="" maxlength="" id="" event="" action=""
        disabled=false clientAutocomplete="" ajaxUrl="" ajaxEnabled="" mask="" tabindex="" readonly="" required=false
        placeholder="" delegatorName="default">
  <input type="${type}" name="${name?default("")?html}"<#t/>
  <#if ajaxEnabled?has_content && ajaxEnabled && ajaxUrl?has_content>
    <#local defaultMinLength = modelTheme.getAutocompleterDefaultMinLength()>
    <#local defaultDelay = modelTheme.getAutocompleterDefaultDelay()>
    <#local className = className + " ajaxAutoCompleter"/>
     data-show-description="false"<#rt/>
     data-default-minlength="${defaultMinLength!2}"<#rt/>
     data-ajax-url="${ajaxUrl!}"<#rt/>
     data-default-delay="${defaultDelay!300}"<#rt/>
  </#if>
    <@renderClass className alert /> <@renderDisabled disabled />
    <#if value?has_content> value="${value}"</#if><#rt/>
    <#if textSize?has_content> size="${textSize}"</#if><#rt/>
    <#if maxlength?has_content> maxlength="${maxlength}"</#if><#rt/>
    <#if readonly?has_content && readonly> readonly</#if><#rt/>
    <#if mask?has_content> data-mask="${mask}"</#if><#rt/>
    <#if id?has_content> id="${id}"</#if><#rt/>
    <#if event?has_content && action?has_content> ${event}="${action}"</#if><#rt/>
    <#if clientAutocomplete?has_content && clientAutocomplete=="false"> autocomplete="off"</#if><#rt/>
    <#if placeholder?has_content> placeholder="${placeholder}"</#if><#rt/>
    <#if tabindex?has_content> tabindex="${tabindex}"</#if><#rt/>
    <#if required?has_content && required> required</#if>
    <#if pattern?has_content> pattern="${pattern}"</#if>
  /><#t/>
</#macro>

<#macro renderTextareaField name className alert cols="" rows="" maxlength="" id="" readonly="" value="" visualEditorEnable="" buttons="" tabindex="" language="" disabled=false placeholder="">
  <#if visualEditorEnable?has_content>
    <#local className = className + " visual-editor">
  </#if>
  <textarea name="${name}"<#t/>
    <@renderClass className alert /> <@renderDisabled disabled />
    <#if cols?has_content> cols="${cols}"</#if><#rt/>
    <#if rows?has_content> rows="${rows}"</#if><#rt/>
    <#if id?has_content> id="${id}"</#if><#rt/>
    <#if readonly?has_content && readonly=='readonly'> readonly</#if><#rt/>
    <#if maxlength?has_content> maxlength="${maxlength}"</#if><#rt/>
    <#if tabindex?has_content> tabindex="${tabindex}"</#if><#rt/>
    <#if visualEditorEnable?has_content> data-toolbar="${buttons?default("maxi")}"</#if><#rt/>
    <#if language?has_content> data-language="${language!"en"}"</#if><#rt/>
    <#if placeholder?has_content> placeholder="${placeholder}"</#if><#rt/>
    ><#t/>
    <#if value?has_content>${value}</#if><#t/>
  </textarea><#lt/>
</#macro>

<#macro renderDateTimeField name className timeDropdownParamName defaultDateTimeString localizedIconTitle timeHourName timeMinutesName ampmName compositeType alert=false isTimeType=false isDateType=false amSelected=false pmSelected=false timeDropdown="" classString="" isTwelveHour=false hour1="" hour2="" minutes=0 shortDateInput="" title="" value="" size="" maxlength="" id="" formName="" mask="" event="" action="" step="" timeValues="" tabindex="" disabled=false isXMLHttpRequest=false>
  <span class="view-calendar">
    <#local cultureInfo = Static["org.apache.ofbiz.common.JsLanguageFilesMappingUtil"].getFile("datejs", .locale)/>
    <#local datePickerLang = Static["org.apache.ofbiz.common.JsLanguageFilesMappingUtil"].getFile("jquery", .locale)/>
    <#local timePicker = "/common/js/node_modules/@chinchilla-software/jquery-ui-timepicker-addon/dist/jquery-ui-timepicker-addon.min.js,/common/js/node_modules/@chinchilla-software/jquery-ui-timepicker-addon/dist/jquery-ui-timepicker-addon.css"/>
    <#local timePickerLang = Static["org.apache.ofbiz.common.JsLanguageFilesMappingUtil"].getFile("dateTime", .locale)/>
    <#if !isTimeType >
      <input type="text" name="${name}_i18n" <@renderClass className alert?c /> <@renderDisabled disabled />
        <#if tabindex?has_content> tabindex="${tabindex}"</#if>
        <#if title?has_content> title="${title}"</#if>
        <#if value?has_content> value="${value}"</#if>
        <#if size?has_content> size="${size}"</#if><#rt/>
        <#if maxlength?has_content>  maxlength="${maxlength}"</#if>
        <#if id?has_content> id="${id}_i18n"</#if>/><#rt/>
        <#local className = className + " date-time-picker"/>
    </#if>
    <input type="hidden" name="${name}" <@renderClass className alert?c /> <@renderDisabled disabled />
      <#if tabindex?has_content> tabindex="${tabindex}"</#if>
      <#if event?has_content && action?has_content> ${event}="${action}"</#if><#rt/>
      <#if title?has_content> title="${title}"</#if>
      <#if value?has_content> value="${value}"</#if>
      <#if size?has_content> size="${size}"</#if><#rt/>
      <#if maxlength?has_content> maxlength="${maxlength}"</#if>
      <#if mask?has_content> data-mask="${mask}"</#if><#rt/>
      <#if cultureInfo?has_content> data-cultureinfo="${cultureInfo}"</#if><#rt/>
      <#if datePickerLang?has_content> data-datepickerlang="${datePickerLang}"</#if><#rt/>
      <#if timePicker?has_content> data-timepicker="${timePicker}"</#if><#rt/>
      <#if timePickerLang?has_content> data-timepickerlang="${timePickerLang}"</#if><#rt/>
      data-shortdate="${shortDateInput?string}"
      <#if id?has_content> id="${id}"</#if>/><#rt/>
    <#if timeDropdown?has_content && timeDropdown=="time-dropdown">
      <select name="${timeHourName}" <@renderDisabled disabled /> <#if classString?has_content>class="${classString}"</#if>><#rt/>
        <#if isTwelveHour>
          <#local x=11>
          <#list 0..x as i>
            <option value="${i}"<#if hour1?has_content><#if i=hour1> selected="selected"</#if></#if>>${i}</option><#rt/>
          </#list>
        <#else>
          <#local x=23>
          <#list 0..x as i>
            <option value="${i}"<#if hour2?has_content><#if i=hour2> selected="selected"</#if></#if>>${i}</option><#rt/>
          </#list>
        </#if>
        </select>:<select name="${timeMinutesName}" <@renderDisabled disabled /> <#if classString?has_content>class="${classString}"</#if>><#rt/>
          <#local values = Static["org.apache.ofbiz.base.util.StringUtil"].toList(timeValues)>
          <#list values as i>
            <option value="${i}"<#if minutes?has_content><#if i?number== minutes ||((i?number==(60 -step?number)) && (minutes &gt; 60 - (step?number/2))) || ((minutes &gt; i?number )&& (minutes &lt; i?number+(step?number/2))) || ((minutes &lt; i?number )&& (minutes &gt; i?number-(step?number/2)))> selected="selected"</#if></#if>>${i}</option><#rt/>
          </#list>
        </select>
        <#rt/>
        <#if isTwelveHour>
          <select name="${ampmName}" <@renderDisabled disabled /> <#if classString?has_content>class="${classString}"</#if>><#rt/>
            <option value="AM" <#if amSelected>selected="selected"</#if>>AM</option><#rt/>
            <option value="PM" <#if pmSelected>selected="selected"</#if>>PM</option><#rt/>
          </select>
        <#rt/>
      </#if>
    </#if>
    <input type="hidden" name="${compositeType}" value="Timestamp"/>
    <#if isXMLHttpRequest>
        <script>
          initDateTimePicker(document.getElementById("${id}"));
        </script>
    </#if>
  </span>
</#macro>

<#macro renderDropDownOptionList items currentValue multiple dDFCurrent noCurrentSelectedKey>
  <#list items as item>
    <#if item.options?has_content>
      <#if groupOpen??></optgroup></#if>
      <optgroup label="${item.description}"<#if item.id??> id="${item.id}"</#if><#if item.widgetStyle??> class="${item.widgetStyle}"</#if>/>
      <@renderDropDownOptionList item.options currentValue multiple dDFCurrent noCurrentSelectedKey/>
      <#assign groupOpen = true/>
    <#else>
      <#if multiple>
        <option
        <#if currentValue?has_content && item.selected()>
          selected
        <#elseif !currentValue?has_content && noCurrentSelectedKey?has_content && noCurrentSelectedKey == item.key()>
          selected
        </#if>
        value="${item.key()}">${item.description()}</option><#rt/>
      <#else>
        <option
        <#if currentValue?has_content && currentValue == item.key() && dDFCurrent?has_content && "selected" == dDFCurrent>
          selected
        <#elseif !currentValue?has_content && noCurrentSelectedKey?has_content && noCurrentSelectedKey == item.key()>
          selected
        </#if>
        value="${item.key()}">${item.description()}</option><#rt/>
      </#if>
    </#if>
  </#list>
  <#if groupOpen??></optgroup></#if>
</#macro>
<#macro renderDropDownField name className id formName explicitDescription options ajaxEnabled
        otherFieldName="" otherValue="" otherFieldSize=""
        alert="" conditionGroup="" tabindex="" multiple=false event="" size="" placeCurrentValueAsFirstOption=false
        currentValue="" allowEmpty=false dDFCurrent="" noCurrentSelectedKey="" disabled=false action="">
  <#if conditionGroup?has_content>
    <input type="hidden" name="${name}_grp" value="${conditionGroup}"/>
  </#if>
  <span class="ui-widget">
    <select name="${name?default("")}"
      <@renderClass className alert /> <@renderDisabled disabled />
      <#if id?has_content> id="${id}"</#if>
      <#if multiple> multiple="multiple"</#if>
      <#if ajaxEnabled> class="autoCompleteDropDown"</#if>
      <#if event?has_content> ${event}="${action}"</#if>
      <#if size?has_content> size="${size}"</#if>
      <#if tabindex?has_content> tabindex="${tabindex}"</#if><#rt/>
      <#if otherFieldName?has_content>
        data-other-field-name="${otherFieldName}"
        data-other-field-value='${otherValue?js_string}'
        data-other-field-size='${otherFieldSize}'
      </#if>>
      <#if placeCurrentValueAsFirstOption && currentValue?has_content && !multiple>
        <option selected="selected" value="${currentValue}">${explicitDescription}</option><#rt/>
      </#if>
      <#if allowEmpty>
        <option value="">&nbsp;</option>
      <#elseif !options?has_content>
          <option value="">&nbsp;</option>
      </#if>
      <@renderDropDownOptionList options currentValue multiple dDFCurrent noCurrentSelectedKey/>
    </select>
  </span>
  <#if otherFieldName?has_content>
    <noscript><input type='text' name='${otherFieldName}' /></noscript>
  </#if>
</#macro>

<#macro renderCheckField items className alert id name action conditionGroup="" allChecked="" currentValue=""  event="" tabindex="" disabled=false>
  <#if conditionGroup?has_content>
    <input type="hidden" name="${name}_grp" value="${conditionGroup}" <@renderDisabled disabled />/>
  </#if>
  <#list items as item>
    <span <@renderClass className alert />><#rt/>
      <input <@renderDisabled disabled /> type="checkbox"<#if (item_index == 0)> id="${id}"</#if><#rt/><#if tabindex?has_content> tabindex="${tabindex}"</#if><#rt/>
        name="${name?default("")?html}" value="${item.value?default("")?html}"<#if event?has_content> ${event}="${action}"</#if>
        <#if allChecked?has_content && allChecked> checked="checked" </#if>
        <#if item.checked?has_content && item.checked?boolean> checked="checked"</#if>/><#rt/>
      ${item.description?default("")}
    </span>
  </#list>
</#macro>

<#macro renderRadioField items className alert name action conditionGroup="" currentValue="" noCurrentSelectedKey="" event="" tabindex="" disabled=false>
  <#if conditionGroup?has_content>
    <input type="hidden" name="${name}_grp" value="${conditionGroup}" <@renderDisabled disabled />/>
  </#if>
  <#list items as item>
    <span <@renderClass className alert />><#rt/>
      <input type="radio" <@renderDisabled disabled />
        <#if currentValue?has_content>
          <#if currentValue==item.key> checked="checked"</#if>
          <#if tabindex?has_content> tabindex="${tabindex}"</#if><#rt/>
        <#elseif noCurrentSelectedKey?has_content && noCurrentSelectedKey == item.key> checked="checked"</#if>
        name="${name?default("")?html}" value="${item.key?default("")?html}"
        <#if event?has_content> ${event}="${action}"</#if>/><#rt/>
      ${item.description}
    </span>
  </#list>
</#macro>

<#macro renderSubmitField buttonType className alert formName action imgSrc ajaxUrl id title="" name="" event="" confirmation="" containerId="" tabindex="" disabled=false closeOnSubmit="true">
  <#if buttonType=="text-link">
    <a <@renderClass className alert /> href="javascript:document.${formName}.submit()" <#if confirmation?has_content>onclick="return confirm('${confirmation?js_string}');"</#if>><#if title?has_content>${title}</#if> </a>
  <#elseif buttonType=="image">
    <input type="image" src="${imgSrc}" <@renderClass className alert /> <@renderDisabled disabled /> <#if name?has_content> name="${name}"</#if>
    <#if title?has_content> alt="${title}"</#if><#if event?has_content> ${event}="${action}"</#if>
    <#if confirmation?has_content>onclick="return confirm('${confirmation?js_string}');"</#if>/>
  <#else>
    <input type="submit" <@renderClass className alert /> <@renderDisabled disabled />
      <#if id?has_content> id="${id}"</#if><#rt/>
      <#if name??> name="${name}"</#if>
      <#if title?has_content> value="${title}"</#if>
      <#if event?has_content> ${event}="${action}"</#if>
      <#if !containerId?has_content>
          <#if confirmation?has_content> onclick="return confirm('${confirmation?js_string}');"</#if>
          <#if tabindex?has_content> tabindex="${tabindex}"</#if><#rt/>
      </#if>/>
      <#if containerId?has_content>
        <#-- the form will be submit by ajax, we inform that perss enter need to call  -->
        <script>
            $("form[name='${formName}']")
            .submit(function(e) {
                e.preventDefault();
                e.stopPropagation();
                if ($(this).valid()) {
                    <#if confirmation?has_content>if (confirm('${confirmation?js_string}')) </#if>ajaxSubmitFormUpdateAreas('${formName}', '${ajaxUrl}', '${closeOnSubmit}');
                }
            })
            .keypress(function(e) {
                if (e.which === 13 && ! $(e.target).is('textarea')) {
                    e.preventDefault();
                    $("#${id!}").click();
                }
            });
        </script>
      </#if>
  </#if>
</#macro>

<#macro renderResetField className alert name title="">
  <input type="reset" <@renderClass className alert /> name="${name}"<#if title?has_content> value="${title}"</#if>/>
</#macro>

<#macro renderHiddenField name conditionGroup="" value="" id="" event="" action="" disabled=false>
  <#if conditionGroup?has_content>
    <input type="hidden" name="${name}_grp" value="${conditionGroup}" <@renderDisabled disabled />/>
  </#if>
  <input type="hidden" name="${name}" <@renderDisabled disabled />
    <#if value?has_content> value="${value}"</#if>
    <#if id?has_content> id="${id}"</#if>
    <#if event?has_content && action?has_content> ${event}="${action}"</#if>/>
</#macro>

<#macro renderIgnoredField></#macro>

<#macro renderFieldTitle style title id fieldHelpText="" for="">
  <label <#if for?has_content>for="${for}"</#if> <#if fieldHelpText?has_content> title="${fieldHelpText}"</#if><#if style?has_content> class="${style}"</#if><#if id?has_content> id="${id}"</#if>><#t/>
    ${title}<#t/>
  </label><#t/>
</#macro>
<#macro renderEmptyFormDataMessage message>
  <h3><#if message?has_content>${message}</#if></h3>
</#macro>
<#macro renderSingleFormFieldTitle></#macro>

<#macro renderFormOpen linkUrl formType name viewIndexField viewSizeField viewIndex viewSize targetWindow="" containerId="" containerStyle="" autocomplete="" useRowSubmit="" focusFieldName="" hasRequiredField="" csrfNameValue="">
  <form method="post" action="${linkUrl}"
        <#if formType=="upload"> enctype="multipart/form-data"</#if>
        <#if targetWindow?has_content> target="${targetWindow}"</#if>
        <#if containerId?has_content> id="${containerId}"</#if>
        <#if focusFieldName?has_content> data-focus-field="${focusFieldName}"</#if>
        class="<#if containerStyle?has_content>${containerStyle}<#else>basic-form</#if>"
        onsubmit="javascript:submitFormDisableSubmits(this)"
        <#if autocomplete?has_content> autocomplete="${autocomplete}"</#if>
        name="${name}"><#lt/>
    <#if csrfNameValue?has_content>
      <#assign result = csrfNameValue?matches(r"(\w+) (\w+)")>
      <#if result>
        <input type="hidden" name="${result?groups[1]}" value="${result?groups[2]}"/>
      </#if>
    </#if>
    <#if useRowSubmit?has_content && useRowSubmit>
      <input type="hidden" name="_useRowSubmit" value="Y"/>
      <#if linkUrl?index_of("VIEW_INDEX") &lt;= 0 && linkUrl?index_of(viewIndexField) &lt;= 0>
        <input type="hidden" name="${viewIndexField}" value="${viewIndex}"/>
      </#if>
      <#if linkUrl?index_of("VIEW_SIZE") &lt;= 0 && linkUrl?index_of(viewSizeField) &lt;= 0>
        <input type="hidden" name="${viewSizeField}" value="${viewSize}"/>
      </#if>
    </#if>
</#macro>
<#macro renderFormClose>
  </form><#lt/>
</#macro>
<#macro renderMultiFormClose>
  </form><#lt/>
</#macro>

<#macro renderFormatListWrapperOpen formName columnStyles style="">
  <table cellspacing="0" class="<#if style?has_content>${style}<#else>basic-table form-widget-table dark-grid</#if>"><#lt/>
</#macro>

<#macro renderFormatListWrapperClose formName>
  </table><#lt/>
</#macro>

<#macro renderFormatHeaderOpen>
  <thead>
</#macro>
<#macro renderFormatHeaderClose>
  </thead>
</#macro>

<#macro renderFormatHeaderRowOpen style="">
  <tr class="<#if style?has_content>${style}<#else>header-row</#if>">
</#macro>
<#macro renderFormatHeaderRowClose>
  </tr>
</#macro>
<#macro renderFormatHeaderRowCellOpen style="" positionSpan="">
  <td <#if positionSpan?has_content && positionSpan gt 1 >colspan="${positionSpan}"</#if><#if style?has_content>class="${style}"</#if>>
</#macro>
<#macro renderFormatHeaderRowCellClose>
  </td>
</#macro>

<#macro renderFormatHeaderRowFormCellOpen style="">
  <td <#if style?has_content>class="${style}"</#if>>
</#macro>
<#macro renderFormatHeaderRowFormCellClose>
  </td>
</#macro>
<#macro renderFormatHeaderRowFormCellTitleSeparator isLast style="">
  <#if style?has_content><span class="${style}"></#if> - <#if style?has_content></span></#if>
</#macro>

<#macro renderFormatItemRowOpen formName itemIndex="" altRowStyles="" evenRowStyle="" oddRowStyle="">
  <tr <#if itemIndex?has_content><#if itemIndex%2==0><#if evenRowStyle?has_content>class="${evenRowStyle}<#if altRowStyles?has_content> ${altRowStyles}</#if>"<#elseif altRowStyles?has_content>class="${altRowStyles}"</#if><#else><#if oddRowStyle?has_content>class="${oddRowStyle}<#if altRowStyles?has_content> ${altRowStyles}</#if>"<#elseif altRowStyles?has_content>class="${altRowStyles}"</#if></#if></#if> >
</#macro>
<#macro renderFormatItemRowClose formName>
  </tr>
</#macro>
<#macro renderFormatItemRowCellOpen fieldName style="" positionSpan="">
  <td <#if positionSpan?has_content && positionSpan gt 1>colspan="${positionSpan}"</#if><#if style?has_content>class="${style}"</#if>>
</#macro>
<#macro renderFormatItemRowCellClose fieldName>
  </td>
</#macro>
<#macro renderFormatItemRowFormCellOpen style="">
  <td<#if style?has_content> class="${style}"</#if>>
</#macro>
<#macro renderFormatItemRowFormCellClose>
  </td>
</#macro>

<#macro renderFormatSingleWrapperOpen formName style="">
  <table cellspacing="0" <#if style?has_content>class="${style}"</#if>>
</#macro>
<#macro renderFormatSingleWrapperClose formName>
  </table>
</#macro>

<#macro renderFormatFieldRowOpen>
  <tr>
</#macro>
<#macro renderFormatFieldRowClose>
  </tr>
</#macro>
<#macro renderFormatFieldRowTitleCellOpen style="">
  <td class="<#if style?has_content>${style}<#else>label</#if>">
</#macro>
<#macro renderFormatFieldRowTitleCellClose>
  </td>
</#macro>
<#macro renderFormatFieldRowSpacerCell></#macro>
<#macro renderFormatFieldRowWidgetCellOpen positionSpan="" style="">
  <td<#if positionSpan?has_content && positionSpan gt 0> colspan="${1+positionSpan*3}"</#if><#if style?has_content> class="${style}"</#if>>
</#macro>
<#macro renderFormatFieldRowWidgetCellClose>
  </td>
</#macro>

<#--
    Initial work to convert table based layout for "single" form to divs.
<#macro renderFormatSingleWrapperOpen style> <div <#if style?has_content>class="${style}"</#if> ></#macro>
<#macro renderFormatSingleWrapperClose> </div></#macro>

<#macro renderFormatFieldRowOpen>  <div></#macro>
<#macro renderFormatFieldRowClose>  </div></#macro>
<#macro renderFormatFieldRowTitleCellOpen style>   <div class="<#if style?has_content>${style}<#else>label</#if>"></#macro>
<#macro renderFormatFieldRowTitleCellClose></div></#macro>
<#macro renderFormatFieldRowSpacerCell></#macro>
<#macro renderFormatFieldRowWidgetCellOpen positionSpan style>   <div<#if positionSpan?has_content && positionSpan gt 0> colspan="${1+positionSpan*3}"</#if><#if style?has_content> class="${style}"</#if>></#macro>
<#macro renderFormatFieldRowWidgetCellClose></div></#macro>

-->


<#macro renderFormatEmptySpace>&nbsp;</#macro>

<#macro renderTextFindField name defaultOption opBeginsWith opContains opIsEmpty opNotEqual className alert hideIgnoreCase ignCase ignoreCase conditionGroup="" value="" opEquals="" size="" maxlength="" autocomplete="" titleStyle="" tabindex="" disabled=false>
  <#if conditionGroup?has_content>
    <input type="hidden" name="${name}_grp" value="${conditionGroup}" <@renderDisabled disabled />/>
  </#if>
  <#if opEquals?has_content>
    <select <@renderDisabled disabled /> <#if name?has_content>name="${name}_op"</#if> class="selectBox"><#rt/>
      <option value="equals"<#if defaultOption=="equals"> selected="selected"</#if>>${opEquals}</option><#rt/>
      <option value="like"<#if defaultOption=="like"> selected="selected"</#if>>${opBeginsWith}</option><#rt/>
      <option value="contains"<#if defaultOption=="contains"> selected="selected"</#if>>${opContains}</option><#rt/>
      <option value="empty"<#rt/><#if defaultOption=="empty"> selected="selected"</#if>>${opIsEmpty}</option><#rt/>
      <option value="notEqual"<#if defaultOption=="notEqual"> selected="selected"</#if>>${opNotEqual}</option><#rt/>
    </select>
  <#else>
    <input type="hidden" name=<#if name?has_content> "${name}_op"</#if>    value="${defaultOption}"/><#rt/>
  </#if>
    <input type="text" <@renderClass className alert /> <@renderDisabled disabled /> name="${name}"
      <#if value?has_content> value="${value}"</#if>
      <#if size?has_content> size="${size}"</#if>
      <#if maxlength?has_content> maxlength="${maxlength}"</#if>
      <#if autocomplete?has_content> autocomplete="off"</#if>
      <#if tabindex?has_content> tabindex="${tabindex}"</#if>/><#rt/>
    <#if titleStyle?has_content><span class="${titleStyle}" ><#rt/></#if>
    <#if hideIgnoreCase>
      <input type="hidden" name="${name}_ic" value=<#if ignCase>"Y"<#else> ""</#if>/><#rt/>
    <#else>
      <input type="checkbox" name="${name}_ic" value="Y" <#if ignCase> checked="checked"</#if> <@renderDisabled disabled />/> ${ignoreCase}<#rt/>
    </#if>
    <#if titleStyle?has_content></span>
  </#if>
</#macro>

<#macro renderDateFindField id name formName defaultOptionFrom defaultOptionThru opEquals opSameDay opGreaterThanFromDayStart opGreaterThan opGreaterThan opLessThan opUpToDay opUpThruDay opIsEmpty language="" className="" alert=false imgSrc="" value="" isTimeType=false isDateType=false conditionGroup="" localizedInputTitle="" value2="" size="" maxlength="" titleStyle="" tabindex="" disabled=false>
  <#if conditionGroup?has_content>
    <input type="hidden" name="${name}_grp" value="${conditionGroup}" <@renderDisabled disabled />/>
  </#if>
  <#if !isTimeType>
    <#local className = className + " date-time-picker"/>
  </#if>
  <#local shortDateInput = isDateType/>
  <#local cultureInfo = Static["org.apache.ofbiz.common.JsLanguageFilesMappingUtil"].getFile("datejs", .locale)/>
  <#local datePickerLang = Static["org.apache.ofbiz.common.JsLanguageFilesMappingUtil"].getFile("jquery", .locale)/>
  <#local timePicker = "/common/js/node_modules/@chinchilla-software/jquery-ui-timepicker-addon/dist/jquery-ui-timepicker-addon.min.js,/common/js/node_modules/@chinchilla-software/jquery-ui-timepicker-addon/dist/jquery-ui-timepicker-addon.css"/>
  <#local timePickerLang = Static["org.apache.ofbiz.common.JsLanguageFilesMappingUtil"].getFile("dateTime", .locale)/>
  <span class="view-calendar">
    <#if language?matches("fr.*")>
      <select <@renderDisabled disabled /> <#if name?has_content> name="${name}_fld0_op"</#if> class="selectBox"><#rt/>
        <option value="equals"<#if defaultOptionFrom=="equals"> selected="selected"</#if>>${opEquals}</option><#rt/>
        <option value="sameDay"<#if defaultOptionFrom=="sameDay"> selected="selected"</#if>>${opSameDay}</option><#rt/>
        <option value="greaterThanFromDayStart"<#if defaultOptionFrom=="greaterThanFromDayStart"> selected="selected"</#if>>${opGreaterThanFromDayStart}</option><#rt/>
        <option value="greaterThan"<#if defaultOptionFrom=="greaterThan"> selected="selected"</#if>>${opGreaterThan}</option><#rt/>
      </select><#rt/>
    </#if>
    <input id="${id}_fld0_value" type="text" <@renderClass className alert?c /> <@renderDisabled disabled />
        <#if name?has_content> name="${name?html}_fld0_value"</#if>
        <#if localizedInputTitle?has_content> title="${localizedInputTitle}"</#if>
        <#if value?has_content> value="${value}"</#if>
        <#if size?has_content> size="${size}"</#if>
        <#if maxlength?has_content> maxlength="${maxlength}"</#if>
        <#if tabindex?has_content> tabindex="${tabindex}"</#if><#rt/>
        <#if cultureInfo?has_content> data-cultureinfo="${cultureInfo}"</#if><#rt/>
        <#if datePickerLang?has_content> data-datepickerlang="${datePickerLang}"</#if><#rt/>
        <#if timePicker?has_content> data-timepicker="${timePicker}"</#if><#rt/>
        <#if timePickerLang?has_content> data-timepickerlang="${timePickerLang}"</#if><#rt/>
         data-shortdate="${shortDateInput?string}"
    />
    <#if titleStyle?has_content>
      <span class="${titleStyle}"><#rt/>
    </#if>
      <#if !language?matches("fr.*")>
        <select <@renderDisabled disabled /> <#if name?has_content> name="${name}_fld0_op"</#if> class="selectBox"><#rt/>
          <option value="equals"<#if defaultOptionFrom=="equals"> selected="selected"</#if>>${opEquals}</option><#rt/>
          <option value="sameDay"<#if defaultOptionFrom=="sameDay"> selected="selected"</#if>>${opSameDay}</option><#rt/>
          <option value="greaterThanFromDayStart"<#if defaultOptionFrom=="greaterThanFromDayStart"> selected="selected"</#if>>${opGreaterThanFromDayStart}</option><#rt/>
          <option value="greaterThan"<#if defaultOptionFrom=="greaterThan"> selected="selected"</#if>>${opGreaterThan}</option><#rt/>
        </select><#rt/>
      </#if>
    <#if titleStyle?has_content>
      </span><#rt/>
    </#if>
    <#rt/>
    <#if language?matches("fr.*")>
      <select name=<#if name?has_content>"${name}_fld1_op"</#if> class="selectBox" <@renderDisabled disabled />><#rt/>
        <option value="opLessThan"<#if defaultOptionThru=="opLessThan"> selected="selected"</#if>>${opLessThan}</option><#rt/>
        <option value="upToDay"<#if defaultOptionThru=="upToDay"> selected="selected"</#if>>${opUpToDay}</option><#rt/>
        <option value="upThruDay"<#if defaultOptionThru=="upThruDay"> selected="selected"</#if>>${opUpThruDay}</option><#rt/>
        <option value="empty"<#if defaultOptionFrom=="empty"> selected="selected"</#if>>${opIsEmpty}</option><#rt/>
      </select><#rt/>
    </#if>
    <input id="${id}_fld1_value" type="text" <@renderClass className alert?c /> <@renderDisabled disabled />
        <#if name?has_content> name="${name}_fld1_value"</#if>
        <#if localizedInputTitle??> title="${localizedInputTitle?html}"</#if>
        <#if value2?has_content> value="${value2}"</#if>
        <#if size?has_content> size="${size}"</#if>
        <#if maxlength?has_content> maxlength="${maxlength}"</#if>
        <#if cultureInfo?has_content> data-cultureinfo="${cultureInfo}"</#if><#rt/>
        <#if datePickerLang?has_content> data-datepickerlang="${datePickerLang}"</#if><#rt/>
        <#if timePicker?has_content> data-timePicker="${timePicker}"</#if><#rt/>
        <#if timePickerLang?has_content> data-timepickerlang="${timePickerLang}"</#if><#rt/>
        data-shortdate="${shortDateInput?string}"
    /><#rt/>
    <#if titleStyle?has_content>
      <span class="${titleStyle}"><#rt/>
    </#if>
      <#if !language?matches("fr.*")>
        <select name=<#if name?has_content>"${name}_fld1_op"</#if> class="selectBox" <@renderDisabled disabled />><#rt/>
          <option value="opLessThan"<#if defaultOptionThru=="opLessThan"> selected="selected"</#if>>${opLessThan}</option><#rt/>
          <option value="upToDay"<#if defaultOptionThru=="upToDay"> selected="selected"</#if>>${opUpToDay}</option><#rt/>
          <option value="upThruDay"<#if defaultOptionThru=="upThruDay"> selected="selected"</#if>>${opUpThruDay}</option><#rt/>
          <option value="empty"<#if defaultOptionFrom=="empty"> selected="selected"</#if>>${opIsEmpty}</option><#rt/>
        </select><#rt/>
      </#if>
    <#if titleStyle?has_content>
      </span>
    </#if>
  </span>
</#macro>

<#macro renderRangeFindField className alert value defaultOptionFrom opEquals opGreaterThan opGreaterThanEquals opLessThan opLessThanEquals defaultOptionThru conditionGroup="" name="" size="" maxlength="" autocomplete="" titleStyle="" value2="" tabindex="" disabled=false>
  <#if conditionGroup?has_content>
    <input type="hidden" name="${name}_grp" value="${conditionGroup}" <@renderDisabled disabled />/>
  </#if>
  <input type="text" <@renderClass className alert /> <@renderDisabled disabled />
    <#if name?has_content>name="${name}_fld0_value"</#if>
    <#if value?has_content> value="${value}"</#if>
    <#if size?has_content> size="${size}"</#if>
    <#if maxlength?has_content> maxlength="${maxlength}"</#if>
    <#if autocomplete?has_content> autocomplete="off"</#if>
    <#if tabindex?has_content> tabindex="${tabindex}"</#if>/><#rt/>
  <#if titleStyle?has_content>
    <span class="${titleStyle}" ><#rt/>
  </#if>
  <select <@renderDisabled disabled /> <#if name?has_content>name="${name}_fld0_op"</#if> class="selectBox"><#rt/>
    <option value="equals"<#if defaultOptionFrom=="equals"> selected="selected"</#if>>${opEquals}</option><#rt/>
    <option value="greaterThan"<#if defaultOptionFrom=="greaterThan"> selected="selected"</#if>>${opGreaterThan}</option><#rt/>
    <option value="greaterThanEqualTo"<#if defaultOptionFrom=="greaterThanEqualTo"> selected="selected"</#if>>${opGreaterThanEquals}</option><#rt/>
  </select><#rt/>
  <#if titleStyle?has_content>
    </span><#rt/>
  </#if>
  <br /><#rt/>
  <input type="text" <@renderClass className alert /> <@renderDisabled disabled />
    <#if name?has_content> name="${name}_fld1_value"</#if>
    <#if value2?has_content> value="${value2}"</#if>
    <#if size?has_content> size="${size}"</#if>
    <#if maxlength?has_content> maxlength="${maxlength}"</#if>
    <#if autocomplete?has_content> autocomplete="off"</#if>/><#rt/>
  <#if titleStyle?has_content>
    <span class="${titleStyle}"><#rt/>
  </#if>
  <select <@renderDisabled disabled /> name=<#if name?has_content>"${name}_fld1_op"</#if> class="selectBox"><#rt/>
    <option value="lessThan"<#if defaultOptionThru=="lessThan"> selected="selected"</#if>>${opLessThan?html}</option><#rt/>
    <option value="lessThanEqualTo"<#if defaultOptionThru=="lessThanEqualTo"> selected="selected"</#if>>${opLessThanEquals?html}</option><#rt/>
  </select><#rt/>
  <#if titleStyle?has_content>
    </span>
  </#if>
</#macro>

<#macro renderDateRangePicker className alert id name value formName event action locale
        alwaysShowCalendars applyButtonClasses applyLabel autoApply buttonClasses cancelButtonClasses cancelLabel clearTitle
        drops linkedCalendars maxSpan maxYear minYear opens rangeLastMonthLabel rangeLastWeekLabel rangeNextMonthLabel
        rangeNextWeekLabel rangeThisMonthLabel rangeThisWeekLabel showDropdowns showIsoWeekNumbers showRanges showWeekNumbers
        singleDatePicker timePicker timePicker24Hour timePickerIncrement timePickerSeconds
        conditionGroup="" value2="" titleStyle="" tabindex="">
  <#if conditionGroup?has_content>
    <input type="hidden" name="${name}_grp" value="${conditionGroup}"/>
  </#if>
  <span class="view-calendar drp <#if timePicker>time<#else>no-time</#if> <#if singleDatePicker>single<#else>range</#if> <#if timePickerSeconds>seconds<#else>no-seconds</#if>">
    <#if !singleDatePicker>
      <input id="${id}_fld0_op" type="hidden" value="greaterThan" <#if name?has_content> name="${name}_fld0_op"</#if>/>
      <input id="${id}_fld0_value" type="hidden"
        <#if name?has_content> name="${name?html}_fld0_value"</#if>
        <#if value?has_content> value="${value}"</#if>
        <#if event?has_content && action?has_content> ${event}="${action}"</#if>/>
      <input id="${id}_fld1_op" type="hidden" value="opLessThan" <#if name?has_content> name="${name}_fld1_op"</#if>/>
      <input id="${id}_fld1_value" type="hidden"
        <#if name?has_content> name="${name?html}_fld1_value"</#if>
        <#if value2?has_content> value="${value2}"</#if>
        <#if event?has_content && action?has_content> ${event}="${action}"</#if>/>
      <input id="${id}" type="text" value="" <@renderClass className alert/><#if tabindex?has_content> tabindex="${tabindex}"</#if>/><#rt/>
    <#else>
      <input type="text" name="${name}_i18n" <@renderClass className alert /><#rt/>
        <#if tabindex?has_content> tabindex="${tabindex}"</#if>
        <#if title?has_content> title="${title}"</#if>
        <#if value?has_content> value="${value}"</#if>
        <#if id?has_content> id="${id}_i18n"</#if>/><#rt/>
      <input type="hidden" name="${name}" <@renderClass className alert /><#rt/>
        <#if event?has_content && action?has_content> ${event}="${action}"</#if>
        <#if tabindex?has_content> tabindex="${tabindex}"</#if>
        <#if value?has_content> value="${value}"</#if>
        <#if id?has_content> id="${id}"</#if>/><#rt/>
    </#if>
    <button id="${id}_clear" class="clear-calendar" title="${clearTitle}">❌</button>
    <script type="text/javascript">
      $(document).ready(function () {
        moment.locale('${locale}');
        // didn't find how to get rid of freemarker escaping ":"...
        const u = (i) => i.replaceAll('&#x3a;', ':');
        const timePicker = ${timePicker?c};
        const timePickerSeconds = ${timePickerSeconds?c};
        const singleDatePicker = ${singleDatePicker?c};
        const outputFormat = 'YYYY-MM-DD' + ((timePicker || !singleDatePicker) ? ' HH:mm:ss' : '');
        let displayFormat = 'DD/MM/YYYY' + (timePicker ? ' HH:mm' : '') + (timePicker && timePickerSeconds ? ':ss' : '');
        const start = u('${value}');
        const end = u('${value2}');
        const visibleInput = singleDatePicker ? $('input[name="${name}_i18n"]') : $('#${id}');
        const hiddenInputs = singleDatePicker ? $('[name="${name}"]') : $('#${id}_fld0_value, #${id}_fld1_value');

        visibleInput.daterangepicker({
          alwaysShowCalendars: ${alwaysShowCalendars?c},
          <#if applyButtonClasses?has_content>applyButtonClasses: '${applyButtonClasses}',</#if>
          autoUpdateInput: false,
          autoApply: ${autoApply?c},
          <#if buttonClasses?has_content>buttonClasses: '${buttonClasses}',</#if>
          <#if cancelButtonClasses?has_content>cancelButtonClasses: '${cancelButtonClasses}',</#if>
          <#if drops?has_content>drops: '${drops}',</#if>
          endDate: end ? moment(end) : undefined,
          linkedCalendars: ${linkedCalendars?c},
          locale: {
            <#if applyLabel?has_content>applyLabel: '${applyLabel}',</#if>
            <#if cancelLabel?has_content>cancelLabel: '${cancelLabel}',</#if>
            weekLabel: 'S'
          },
          <#if maxSpan?has_content>maxSpan: { days: ${maxSpan} },</#if>
          <#if maxYear?has_content>maxYear: ${maxYear},</#if>
          <#if minYear?has_content>minYear: ${minYear},</#if>
          <#if opens?has_content>opens: '${opens}',</#if>
          <#if showRanges && !singleDatePicker>
            ranges: {
              '${rangeThisWeekLabel}': [moment().startOf('week'), moment().endOf('week')],
              '${rangeLastWeekLabel}': [moment().subtract(1, 'week').startOf('week'), moment().subtract(1, 'week').endOf('week')],
              '${rangeNextWeekLabel}': [moment().add(1, 'week').startOf('week'), moment().add(1, 'week').endOf('week')],
              '${rangeThisMonthLabel}': [moment().startOf('month'), moment().endOf('month')],
              '${rangeLastMonthLabel}': [moment().subtract(1, 'month').startOf('month'), moment().subtract(1, 'month').endOf('month')],
              '${rangeNextMonthLabel}': [moment().add(1, 'month').startOf('month'), moment().add(1, 'month').endOf('month')],
            },
          </#if>
          showCustomRangeLabel: false,
          showDropdowns: ${showDropdowns?c},
          showIsoWeekNumbers: ${showIsoWeekNumbers?c},
          showWeekNumbers: ${showWeekNumbers?c},
          singleDatePicker: ${singleDatePicker?c},
          startDate: start ? moment(start) : undefined,
          timePicker,
          timePicker24Hour: ${timePicker24Hour?c},
          <#if timePickerIncrement?has_content>timePickerIncrement: ${timePickerIncrement},</#if>
          timePickerSeconds
        });

        updateInputs(moment(start), moment(end));
        visibleInput.on('apply.daterangepicker', function (ev, picker) { updateInputs(picker.startDate, picker.endDate) });
        $('#${id}_clear').on('click', function (e) {
          e.preventDefault();
          visibleInput.val('')
          hiddenInputs.val('');
        });

        visibleInput.on('paste', function (e) {
          e.preventDefault();
          const text = (e.originalEvent || e).clipboardData.getData('text/plain');
          updateInputs(moment(text, displayFormat));
        });

        visibleInput.on('input', function (e) {
          const text = e.target.value;
          if (text.length === 10) {
            updateInputs(moment(text, displayFormat));
          }
        });

        function updateInputs(start, end) {
          if (start.isValid()) {
            if (singleDatePicker) {
              visibleInput.val(start.format(displayFormat));
              hiddenInputs.val(start.format(outputFormat)).trigger('change');
            } else {
              if (end.isValid()) {
                visibleInput.val(start.format(displayFormat) + ' ➜ ' + end.format(displayFormat)).trigger('change');
                $('#${id}_fld0_value').val(start.format(outputFormat)).trigger('change');
                $('#${id}_fld1_value').val(end.format(outputFormat)).trigger('change');
              }
            }
          }
        }
      });
    </script>
  </span>
</#macro>


<#--
@renderLookupField

Description: Renders a text input field as a lookup field.

Parameter: name, String, required - The name of the lookup field.
Parameter: formName, String, required - The name of the form that contains the lookup field.
Parameter: fieldFormName, String, required - Contains the lookup window form name.
Parameter: className, String, optional - The CSS class name for the lookup field.
Parameter: alert, String, optional - If "true" then the "alert" CSS class will be added to the lookup field.
Parameter: value, Object, optional - The value of the lookup field.
Parameter: size, String, optional - The size of the lookup field.
Parameter: maxlength, String or Integer, optional - The max length of the lookup field.
Parameter: id, String, optional - The ID of the lookup field.
Parameter: event, String, optional - The lookup field event that invokes "action". If the event parameter is not empty, then the action parameter must be specified as well.
Parameter: action, String, optional - The action that is invoked on "event". If action parameter is not empty, then the event parameter must be specified as well.
Parameter: readonly, boolean, optional - If true, the lookup field is made read-only.
Parameter: autocomplete, String, optional - If not empty, autocomplete is turned off for the lookup field.
Parameter: descriptionFieldName, String, optional - If not empty and the presentation parameter contains "window", specifies an alternate input field for updating.
Parameter: targetParameterIter, List, optional - Contains a list of form field names whose values will be passed to the lookup window.
Parameter: imgSrc, Not used.
Parameter: ajaxUrl, String, optional - Contains the Ajax URL, used only when the ajaxEnabled parameter contains true.
Parameter: ajaxEnabled, boolean, optional - If true, invokes the Ajax auto-completer.
Parameter: presentation, String, optional - Contains the lookup window type, either "layer" or "window".
Parameter: width, String or Integer, optional - The width of the lookup field.
Parameter: height, String or Integer, optional - The height of the lookup field.
Parameter: position, String, optional - The position style of the lookup field.
Parameter: fadeBackground, ?
Parameter: clearText, String, optional - If the readonly parameter is true, clearText contains the text to be displayed in the field, default is CommonClear label.
Parameter: showDescription, String, optional - If the showDescription parameter is true, a special span with css class "tooltip" will be created at right of the lookup button and a description will fill in (see setLookDescription in OfbizUtil.js). For now not when the lookup is read only.
Parameter: initiallyCollapsed, Not used.
Parameter: lastViewName, String, optional - If the ajaxEnabled parameter is true, the contents of lastViewName will be appended to the Ajax URL.
Parameter: tabindex, String, optional - HTML tabindex number.
Parameter: delegatorName, String, optional - name of the delegator in context.
-->
<#macro renderLookupField name formName fieldFormName conditionGroup="" className="" alert="false" value="" size=""
        maxlength="" id="" event="" action="" readonly=false autocomplete="" descriptionFieldName="" targetParameterIter=""
        imgSrc="" ajaxUrl="" ajaxEnabled=javaScriptEnabled presentation="layer" width=modelTheme.getLookupWidth()
        height=modelTheme.getLookupHeight() position=modelTheme.getLookupPosition() fadeBackground="true" clearText=""
        showDescription="" initiallyCollapsed="" lastViewName="main" tabindex="" delegatorName="default" disabled=false>
  <#if Static["org.apache.ofbiz.widget.model.ModelWidget"].widgetBoundaryCommentsEnabled(context)><#-- context is always null here, but this is handled in widgetBoundaryCommentsEnabled -->
  <!-- @renderLookupField -->
  </#if>
  <#if (!showDescription?has_content)>
    <#local showDescription = "false" />
    <#if "Y" == modelTheme.getLookupShowDescription()>
      <#local showDescription = "true" />
    </#if>
  </#if>
  <#if (!ajaxUrl?has_content) && ajaxEnabled?has_content && ajaxEnabled>
    <#local ajaxUrl = requestAttributes._REQUEST_HANDLER_.makeLink(request, response, fieldFormName)/>
    <#local ajaxUrl = id + "," + ajaxUrl + ",ajaxLookup=Y" />
  </#if>
  <#if ajaxEnabled?has_content && ajaxEnabled && (presentation?has_content && "window" == presentation)>
    <#local ajaxUrl = ajaxUrl + "&amp;_LAST_VIEW_NAME_=" + lastViewName />
  </#if>
  <#if conditionGroup?has_content>
    <input type="hidden" name="${name}_grp" value="${conditionGroup}" <@renderDisabled disabled />/>
  </#if>
  <span class="field-lookup">
    <#if size?has_content && size=="0">
      <input type="hidden" <@renderDisabled disabled />
        <#if name?has_content> name="${name}"</#if>
        <#if tabindex?has_content> tabindex="${tabindex}"</#if>
    <#else>
      <input type="text" <@renderClass className alert /> <@renderDisabled disabled />
        <#if name?has_content> name="${name}"</#if><#if value?has_content> value="${value}"</#if><#if tabindex?has_content> tabindex="${tabindex}"</#if><#rt/>
        <#if size?has_content> size="${size}"</#if><#if maxlength?has_content> maxlength="${maxlength}"</#if><#if id?has_content> id="${id}"</#if><#rt/>
        <#if readonly?has_content && readonly> readonly</#if><#rt/><#if event?has_content && action?has_content> ${event}="${action}"</#if><#rt/>
        <#if autocomplete?has_content> autocomplete="off"</#if><#rt/>
    </#if>
      data-lookup-ajax-enabled="<#if ajaxEnabled?has_content>${ajaxEnabled?string}<#else>false</#if>" <#rt/>
      data-lookup-presentation="${presentation!}" <#rt>
    <#if presentation?has_content && descriptionFieldName?has_content && "window" == presentation>
      data-lookup-field-formname="${fieldFormName}" data-lookup-form-name="${formName?html}" <#if descriptionFieldName?has_content>data-lookup-description-field="${descriptionFieldName}"</#if> <#rt>
      <#if targetParameterIter?has_content>
        <#local args = "${targetParameterIter?join(', ')}">
      </#if>
      data-lookup-args="${args!}" <#rt>
    <#elseif presentation?has_content && "window" == presentation>
      data-lookup-field-formname="${fieldFormName}" <#rt>
      <#if targetParameterIter?has_content>
        <#local args = "${targetParameterIter?join(', ')}">
      </#if>
      data-lookup-args="${args!}" <#rt>
    <#else>
      <#if ajaxEnabled?has_content && ajaxEnabled>
        <#local defaultMinLength = modelTheme.getAutocompleterDefaultMinLength()>
        <#local defaultDelay = modelTheme.getAutocompleterDefaultDelay()>
        <#if !ajaxUrl?contains("searchValueFieldName=")>
          <#if descriptionFieldName?has_content && "true" == showDescription>
            <#local ajaxUrl = ajaxUrl + "&amp;searchValueFieldName=" + descriptionFieldName />
          <#else>
            <#local ajaxUrl = ajaxUrl + "&amp;searchValueFieldName=" + name />
          </#if>
        </#if>
      </#if>
      data-lookup-request-url="${fieldFormName}" data-lookup-form-name="${formName?html}" <#rt>
      data-lookup-optional-target="<#if descriptionFieldName?has_content>${descriptionFieldName}</#if>" <#rt>
      data-lookup-width="${width}" data-lookup-height="${height}" data-lookup-position="${position}" <#rt>
      data-lookup-modal="${fadeBackground}" <#rt>
      data-lookup-show-description=<#if ajaxEnabled?has_content && ajaxEnabled>"${showDescription}"<#else>"false"</#if> <#rt>
      data-lookup-default-minlength="${defaultMinLength!2}" <#rt>
      data-lookup-default-delay="${defaultDelay!300}" <#rt>
      <#if targetParameterIter?has_content>
        <#local args = "${targetParameterIter?join(', ')}">
      </#if>
      data-lookup-args="${args!}"
    </#if>
    data-lookup-ajax-url="${ajaxUrl}" <#rt>
    /><#rt/>
    <#if readonly?has_content && readonly>
      <a id="${id}_clear"
        style="background:none;margin-left:5px;margin-right:15px;"
        class="clearField">
          <#if clearText?has_content>${clearText}<#else>${uiLabelMap.CommonClear}</#if>
      </a>
    </#if>
  </span>
</#macro>

<#macro renderNextPrev paginateStyle paginateFirstStyle viewIndex highIndex listSize viewSize ajaxEnabled javaScriptEnabled ajaxFirstUrl firstUrl paginateFirstLabel paginatePreviousStyle ajaxPreviousUrl previousUrl paginatePreviousLabel pageLabel ajaxSelectUrl selectUrl ajaxSelectSizeUrl selectSizeUrl commonDisplaying paginateNextStyle ajaxNextUrl nextUrl paginateNextLabel paginateLastStyle ajaxLastUrl lastUrl paginateLastLabel paginateViewSizeLabel>
  <#if listSize gt viewSize>
    <div class="${paginateStyle}">&nbsp;
      <ul>
        <li class="${paginateFirstStyle}<#if viewIndex gt 0>"><a href="javascript:void(0)" onclick="<#if ajaxEnabled>ajaxUpdateAreas('${ajaxFirstUrl}')<#else>submitPagination(this, '${firstUrl}')</#if>">${paginateFirstLabel}</a><#else>-disabled"><span>${paginateFirstLabel}</span></#if></li>
        <li class="${paginatePreviousStyle}<#if viewIndex gt 0>"><a href="javascript:void(0)" onclick="<#if ajaxEnabled>ajaxUpdateAreas('${ajaxPreviousUrl}')<#else>submitPagination(this, '${previousUrl}')</#if>">${paginatePreviousLabel}</a><#else>-disabled"><span>${paginatePreviousLabel}</span></#if></li>
        <#if listSize gt 0 && javaScriptEnabled>
          <li class="nav-page-select">
            ${pageLabel}
            <input type="text" placeholder="${viewIndex+1} of ${(listSize/viewSize)?ceiling}" size="15" onchange="<#if ajaxEnabled>ajaxUpdateAreas('${ajaxSelectUrl}')<#else>submitPagination(this, '${selectUrl}'+(this.value-1))</#if>"/>
          </li>
        </#if>
        <li class="${paginateNextStyle}<#if highIndex lt listSize>"><a href="javascript:void(0)" onclick="<#if ajaxEnabled>ajaxUpdateAreas('${ajaxNextUrl}')<#else>submitPagination(this, '${nextUrl}')</#if>">${paginateNextLabel}</a><#else>-disabled"><span>${paginateNextLabel}</span></#if></li>
        <li class="${paginateLastStyle}<#if highIndex lt listSize>"><a href="javascript:void(0)" onclick="<#if ajaxEnabled>ajaxUpdateAreas('${ajaxLastUrl}')<#else>submitPagination(this, '${lastUrl}')</#if>">${paginateLastLabel}</a><#else>-disabled"><span>${paginateLastLabel}</span></#if></li>
        <#if javaScriptEnabled><li class="nav-pagesize"><select name="pageSize" size="1" onchange="<#if ajaxEnabled>ajaxUpdateAreas('${ajaxSelectSizeUrl}')<#else>submitPagination(this, '${selectSizeUrl}')</#if>"><#rt/>
            <#local availPageSizes = [20, 30, 50, 100, 200]>
          <#list availPageSizes as ps>
            <option <#if viewSize == ps> selected="selected" </#if> value="${ps}">${ps}</option>
          </#list>
          </select> ${paginateViewSizeLabel}</li>
        </#if>
        <li class="nav-displaying">${commonDisplaying}</li>
      </ul>
    </div>
  </#if>
</#macro>

<#macro renderFileField className alert name="" value="" size="" maxlength="" autocomplete="" tabindex="" disabled=false>
  <input type="file"
    <@renderClass className alert />
    <@renderDisabled disabled />
    <#if name?has_content> name="${name}"</#if>
    <#if value?has_content> value="${value}"</#if>
    <#if size?has_content> size="${size}"</#if>
    <#if maxlength?has_content> maxlength="${maxlength}"</#if>
    <#if autocomplete?has_content> autocomplete="off"</#if>
    <#if tabindex?has_content> tabindex="${tabindex}"</#if>/><#rt/>
</#macro>

<#macro renderPasswordField className alert name="" value="" size="" maxlength="" id="" autocomplete="" tabindex="" disabled=false>
  <input type="password"
    <@renderClass className alert />
    <@renderDisabled disabled />
    <#if name?has_content> name="${name}"</#if>
    <#if value?has_content> value="${value}"</#if>
    <#if size?has_content> size="${size}"</#if>
    <#if maxlength?has_content> maxlength="${maxlength}"</#if>
    <#if id?has_content> id="${id}"</#if>
    <#if autocomplete?has_content> autocomplete="off"</#if>
    <#if tabindex?has_content> tabindex="${tabindex}"</#if>
    required/><#rt/>
</#macro>

<#macro renderImageField action value="" description="" alternate="" style="" event=""><img<#if value?has_content> src="${value}"</#if><#if description?has_content> title="${description}"</#if> alt="<#if alternate?has_content>${alternate}"</#if><#if style?has_content> class="${style}"</#if><#if event?has_content> ${event?html}="${action}" </#if>/></#macro>

<#macro renderBanner style="" leftStyle="" rightStyle="" leftText="" text="" rightText="">
  <table width="100%">
    <tr><#rt/>
      <#if leftText?has_content><td align="left"><#if leftStyle?has_content><div class="${leftStyle}"></#if>${leftText}<#if leftStyle?has_content></div></#if></td><#rt/></#if>
      <#if text?has_content><td align="center"><#if style?has_content><div class="${style}"></#if>${text}<#if style?has_content></div></#if></td><#rt/></#if>
      <#if rightText?has_content><td align="right"><#if rightStyle?has_content><div class="${rightStyle}"></#if>${rightText}<#if rightStyle?has_content></div></#if></td><#rt/></#if>
    </tr>
  </table>
</#macro>

<#macro renderContainerField id className><div id="${id}" class="${className}"/></#macro>

<#macro renderFieldGroupOpen collapsed collapsibleAreaId collapsible expandToolTip collapseToolTip style="" id="" title="">
  <#if style?has_content || id?has_content || title?has_content><div class="fieldgroup<#if style?has_content> ${style}</#if>"<#if id?has_content> id="${id}"</#if>>
    <div class="fieldgroup-title-bar">
      <#if collapsible>
        <ul>
          <li data-collapsible-area-id="${collapsibleAreaId}" data-expand-tooltip="${expandToolTip}" data-collapse-tooltip="${collapseToolTip}"
                  class="<#if collapsed>collapsed"><#else>expanded"></#if>
            &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<#if title?has_content>${title}</#if></a>
          </li>
        </ul>
      <#else>
        <#if title?has_content>${title}</#if>
      </#if><#rt/>
    </div>
    <div id="${collapsibleAreaId}" class="fieldgroup-body" <#if collapsed && collapsible> style="display: none;"</#if>>
  </#if>
</#macro>

<#macro renderFieldGroupClose style="" id="" title=""><#if style?has_content || id?has_content || title?has_content></div></div></#if></#macro>

<#macro renderHyperlinkTitle name="selectAll" title="" showSelectAll="N">
  <#if title?has_content>${title}<br /></#if>
  <#if showSelectAll="Y"><input type="checkbox" name="${name}" value="Y" class="selectAll"/></#if>
</#macro>

<#macro renderSortField title linkUrl style="" ajaxEnabled="" tooltip="">
  <a<#if style?has_content> class="${style}"</#if> href="<#if ajaxEnabled?has_content && ajaxEnabled>javascript:ajaxUpdateAreas('${linkUrl}')<#else>${linkUrl}</#if>"<#if tooltip?has_content> title="${tooltip}"</#if>>${title}</a>
</#macro>

<#macro formatBoundaryComment boundaryType widgetType widgetName><!-- ${boundaryType}  ${widgetType}  ${widgetName} --></#macro>

<#macro renderTooltip tooltip="" tooltipStyle="">
  <#if tooltip?has_content><span class="<#if tooltipStyle?has_content>${tooltipStyle}<#else>tooltip</#if>">${tooltip}</span><#rt/></#if>
</#macro>

<#macro renderClass className="" alert="">
  <#if className?has_content || (alert?has_content && alert=="true")> class="${className}<#if alert?has_content && alert=="true"> alert</#if>" </#if>
</#macro>

<#macro renderAsterisks requiredField>
  <#if requiredField=="true">*</#if>
</#macro>

<#macro renderDisabled disabled>
  <#if disabled?has_content && disabled> disabled </#if>
</#macro>

<#macro makeHiddenFormLinkForm actionUrl name parameters targetWindow="" disabled=false>
  <form method="post" action="${actionUrl}" <#if targetWindow?has_content>target="${targetWindow}"</#if> onsubmit="javascript:submitFormDisableSubmits(this)" name="${name}">
    <#list parameters as parameter>
      <input name="${parameter.name}" value="${parameter.value?html}" type="hidden" <@renderDisabled disabled />/>
    </#list>
  </form>
</#macro>
<#macro makeHiddenFormLinkAnchor hiddenFormName description event="" action="" imgSrc="" confirmation="" linkStyle="">
  <a <#if linkStyle?has_content>class="${linkStyle}"</#if> href="javascript:document.${hiddenFormName}.submit()"
    <#if action?has_content && event?has_content> ${event}="${action}"</#if>
    <#if confirmation?has_content> onclick="return confirm('${confirmation?js_string}')"</#if>>
      <#if imgSrc?has_content><img src="${imgSrc}" alt=""/></#if>${description}</a>
</#macro>
<#macro makeHyperlinkString hiddenFormName imgSrc imgTitle title alternate linkUrl description text="" linkStyle="" event="" action="" targetParameters="" targetWindow="" confirmation="" uniqueItemName="" height="" width="" id="">
    <#if uniqueItemName?has_content>
        <#local params = "{&quot;presentation&quot;: &quot;layer&quot;">
        <#if targetParameters?has_content && !targetParameters?is_hash>
          <#local parameterMap = targetParameters?eval>
          <#local parameterKeys = parameterMap?keys>
          <#list parameterKeys as key>
            <#local params += ",&quot;${key}&quot;: &quot;${parameterMap[key]}&quot;">
          </#list>
        </#if>
        <#local params += "}">
        <a href="javascript:void(0);" id="${uniqueItemName}_link"
           data-dialog-params="${params}"
           data-dialog-width="${width}"
           data-dialog-height="${height}"
           data-dialog-url="${linkUrl}"
        <#if text?has_content>data-dialog-title="${text}"</#if>
        <#if linkStyle?has_content>class="${linkStyle}"</#if>>
        <#if description?has_content>${description?html}</#if></a>
    <#else>
    <a <#if linkStyle?has_content && (description?has_content || imgSrc?has_content)>class="${linkStyle}"</#if>
      href="${linkUrl}"<#if targetWindow?has_content> target="${targetWindow}"</#if>
      <#if action?has_content && event?has_content> ${event}="${action}"</#if>
      <#if confirmation?has_content> data-confirm-message="${confirmation}"</#if>
      <#if id?has_content> id="${id}"</#if>
      <#if title?has_content> title="${title}"</#if>>
      <#if imgSrc?has_content><img src="${imgSrc}" alt="${alternate}" <#if imgTitle?has_content>title="${imgTitle}"</#if>/></#if>${description?html}</a>
    </#if>
</#macro>
