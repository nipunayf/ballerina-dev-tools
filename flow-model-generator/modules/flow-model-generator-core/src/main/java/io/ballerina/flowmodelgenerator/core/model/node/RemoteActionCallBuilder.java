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
import io.ballerina.flowmodelgenerator.core.db.DatabaseManager;
import io.ballerina.flowmodelgenerator.core.db.model.FunctionResult;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.modelgenerator.commons.CommonUtils;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Represents the generalized action invocation node in the flow model.
 *
 * @since 2.0.0
 */
public class RemoteActionCallBuilder extends FunctionBuilder {

    public static final String TARGET_TYPE_KEY = "targetType";

    @Override
    public void setConcreteConstData() {
        codedata().node(NodeKind.REMOTE_ACTION_CALL);
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        sourceBuilder.newVariable();
        FlowNode flowNode = sourceBuilder.flowNode;

        addCheckKeywordIfNeeded(sourceBuilder, flowNode);

        Optional<Property> connection = flowNode.getProperty(Property.CONNECTION_KEY);
        if (connection.isEmpty()) {
            throw new IllegalStateException("Client must be defined for an action call node");
        }

        return sourceBuilder.token()
                .name(connection.get().toSourceCode())
                .keyword(SyntaxKind.RIGHT_ARROW_TOKEN)
                .name(flowNode.metadata().label())
                .stepOut()
                .functionParameters(flowNode,
                        Set.of(Property.CONNECTION_KEY, Property.VARIABLE_KEY, Property.TYPE_KEY, TARGET_TYPE_KEY,
                                Property.CHECK_ERROR_KEY))
                .textEdit(false)
                .acceptImport()
                .build();
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        Codedata codedata = context.codedata();
        this.cachedFlowNode = fetchNodeTemplate(codedata, context);
    }

    private static FlowNode fetchNodeTemplate(Codedata codedata, TemplateContext context) {
        if (codedata.org().equals("$anon")) {
            return null;
        }

        DatabaseManager dbManager = DatabaseManager.getInstance();
        Optional<FunctionResult> functionResult = codedata.id() != null ? dbManager.getFunction(codedata.id()) :
                dbManager.getAction(codedata.org(), codedata.module(), codedata.symbol(), null,
                        DatabaseManager.FunctionKind.REMOTE);
        if (functionResult.isEmpty()) {
            return null;
        }

        RemoteActionCallBuilder builder = new RemoteActionCallBuilder();
        FunctionResult function = functionResult.get();
        builder
                .metadata()
                .label(function.name())
                .description(function.description())
                .icon(CommonUtils.generateIcon(function.org(), function.packageName(), function.version()))
                .stepOut()
                .codedata()
                .org(function.org())
                .module(function.packageName())
                .object(NewConnectionBuilder.CLIENT_SYMBOL)
                .id(function.functionId())
                .symbol(function.name());

        builder.properties().custom()
                .metadata()
                .label(Property.CONNECTION_LABEL)
                .description(Property.CONNECTION_DOC)
                .stepOut()
                .type(Property.ValueType.IDENTIFIER)
                .typeConstraint(function.packageName() + ":" + NewConnectionBuilder.CLIENT_SYMBOL)
                .value(codedata.parentSymbol())
                .stepOut()
                .addProperty(Property.CONNECTION_KEY);

        builder.setCustomProperties(dbManager.getFunctionParameters(function.functionId()));

        String returnTypeName = function.returnType();
        if (CommonUtils.hasReturn(returnTypeName)) {
            builder.setReturnTypeProperties(returnTypeName, context, true);
        }

        if (function.returnError()) {
            builder.properties().checkError(true);
        }
        return builder.build();
    }
}
