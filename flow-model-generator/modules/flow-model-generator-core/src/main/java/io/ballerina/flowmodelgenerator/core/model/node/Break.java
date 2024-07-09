/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.flowmodelgenerator.core.model.node;

import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;

/**
 * Represents the properties of a break node.
 *
 * @since 1.4.0
 */
public class Break extends FlowNode {

    public static final String LABEL = "Break";
    public static final String DESCRIPTION = "Exit the current loop";

    @Override
    protected void setConstData() {
        this.label = LABEL;
        this.description = DESCRIPTION;
        this.kind = Kind.BREAK;
    }

    @Override
    public String toSource() {
        return SyntaxKind.BREAK_KEYWORD.stringValue() + SyntaxKind.SEMICOLON_TOKEN.stringValue();
    }

    @Override
    public void setTemplateData() {

    }
}
