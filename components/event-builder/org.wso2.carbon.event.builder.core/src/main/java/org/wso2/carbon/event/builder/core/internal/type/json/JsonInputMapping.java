/*
 * Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.carbon.event.builder.core.internal.type.json;

import org.wso2.carbon.event.builder.core.config.InputMapping;
import org.wso2.carbon.event.builder.core.internal.config.InputMappingAttribute;
import org.wso2.carbon.event.builder.core.internal.util.EventBuilderConstants;

import java.util.ArrayList;
import java.util.List;

public class JsonInputMapping implements InputMapping {

    private List<InputMappingAttribute> inputMappingAttributes;
    private boolean batchProcessingEnabled = false;

    public JsonInputMapping() {
        this.inputMappingAttributes = new ArrayList<InputMappingAttribute>();
    }

    public boolean isBatchProcessingEnabled() {
        return batchProcessingEnabled;
    }

    public void setBatchProcessingEnabled(boolean batchProcessingEnabled) {
        this.batchProcessingEnabled = batchProcessingEnabled;
    }

    public List<InputMappingAttribute> getInputMappingAttributes() {
        return inputMappingAttributes;
    }

    public void addInputMappingAttribute(InputMappingAttribute inputMappingAttribute) {
        inputMappingAttributes.add(inputMappingAttribute);
    }

    @Override
    public String getMappingType() {
        return EventBuilderConstants.EB_JSON_MAPPING_TYPE;
    }


}
