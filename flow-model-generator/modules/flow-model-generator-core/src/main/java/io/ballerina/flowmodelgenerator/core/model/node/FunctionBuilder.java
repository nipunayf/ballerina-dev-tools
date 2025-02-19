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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.db.model.Parameter;
import io.ballerina.flowmodelgenerator.core.db.model.ParameterResult;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.FormBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.flowmodelgenerator.core.utils.FlowNodeUtil;
import io.ballerina.flowmodelgenerator.core.utils.ParamUtils;
import io.ballerina.modelgenerator.commons.CommonUtils;
import java.util.Collection;

/**
 * Abstract base class for function and action call builders.
 *
 * @since 2.0.0
 */
public abstract class FunctionBuilder extends NodeBuilder {
    
    protected void setCustomProperties(Collection<ParameterResult> functionParameters) {
        boolean hasOnlyRestParams = functionParameters.size() == 1;
        for (ParameterResult paramResult : functionParameters) {
            if (paramResult.kind().equals(Parameter.Kind.PARAM_FOR_TYPE_INFER)
                    || paramResult.kind().equals(Parameter.Kind.INCLUDED_RECORD)) {
                continue;
            }

            String unescapedParamName = ParamUtils.removeLeadingSingleQuote(paramResult.name());
            Property.Builder<FormBuilder<NodeBuilder>> customPropBuilder = properties().custom();
            customPropBuilder
                    .metadata()
                        .label(unescapedParamName)
                        .description(paramResult.description())
                        .stepOut()
                    .codedata()
                        .kind(paramResult.kind().name())
                        .originalName(paramResult.name())
                        .importStatements(paramResult.importStatements())
                        .stepOut()
                    .placeholder(paramResult.defaultValue())
                    .typeConstraint(paramResult.type())
                    .editable()
                    .defaultable(paramResult.optional());

            switch (paramResult.kind()) {
                case INCLUDED_RECORD_REST -> {
                    if (hasOnlyRestParams) {
                        customPropBuilder.defaultable(false);
                    }
                    unescapedParamName = "additionalValues";
                    customPropBuilder.type(Property.ValueType.MAPPING_EXPRESSION_SET);
                }
                case REST_PARAMETER -> {
                    if (hasOnlyRestParams) {
                        customPropBuilder.defaultable(false);
                    }
                    customPropBuilder.type(Property.ValueType.EXPRESSION_SET);
                }
                default -> customPropBuilder.type(Property.ValueType.EXPRESSION);
            }

            customPropBuilder
                    .stepOut()
                    .addProperty(unescapedParamName);
        }
    }

    protected void setReturnTypeProperties(String returnTypeName, TemplateContext context, boolean isRemoteAction) {
        boolean editable = false;
        String updatedReturnType = returnTypeName;
        if (isRemoteAction && returnTypeName.contains(RemoteActionCallBuilder.TARGET_TYPE_KEY)) {
            updatedReturnType = returnTypeName.replace(RemoteActionCallBuilder.TARGET_TYPE_KEY, "json");
            editable = true;
        }
        properties()
                .type(updatedReturnType, editable)
                .data(updatedReturnType, context.getAllVisibleSymbolNames(), Property.VARIABLE_NAME);
    }

    protected void addCheckKeywordIfNeeded(SourceBuilder sourceBuilder, FlowNode flowNode) {
        if (FlowNodeUtil.hasCheckKeyFlagSet(flowNode)) {
            sourceBuilder.token().keyword(SyntaxKind.CHECK_KEYWORD);
        }
    }

    protected static boolean containsErrorInReturnType(SemanticModel semanticModel,
                                                       FunctionTypeSymbol functionTypeSymbol) {
        TypeSymbol errorTypeSymbol = semanticModel.types().ERROR;
        return functionTypeSymbol.returnTypeDescriptor()
                .map(returnTypeDesc -> CommonUtils.subTypeOf(returnTypeDesc, errorTypeSymbol)).orElse(false);
    }
}