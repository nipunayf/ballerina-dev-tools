/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.wso2.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.flowmodelgenerator.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ExternalFunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.DefaultableParameterNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.RestParameterNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.node.DataMapperDefinitionBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.FunctionDefinitionBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.NPFunctionDefinitionBuilder;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.ModuleInfo;
import io.ballerina.modelgenerator.commons.ParameterData;
import io.ballerina.tools.text.LineRange;

import java.util.Optional;

/**
 * Analyzes the module level functions and generates the flow model.
 *
 * @since 2.0.0
 */
public class ModuleNodeAnalyzer extends NodeVisitor {

    private final ModuleInfo moduleInfo;
    private final SemanticModel semanticModel;
    private final Gson gson;
    private JsonElement node;

    public ModuleNodeAnalyzer(ModuleInfo moduleInfo, SemanticModel semanticModel) {
        this.moduleInfo = moduleInfo;
        this.semanticModel = semanticModel;
        this.gson = new Gson();
    }

    public Optional<JsonElement> findFunction(ModulePartNode rootNode, String functionName) {
        for (ModuleMemberDeclarationNode member : rootNode.members()) {
            if (member.kind() != SyntaxKind.FUNCTION_DEFINITION) {
                continue;
            }

            FunctionDefinitionNode functionNode = (FunctionDefinitionNode) member;
            if (functionNode.functionName().text().equals(functionName)) {
                functionNode.accept(this);
                return Optional.of(this.node);
            }

        }
        return Optional.empty();
    }

    @Override
    public void visit(FunctionDefinitionNode functionDefinitionNode) {
        boolean isNpFunction = isNpFunction(functionDefinitionNode);

        // Build the metadata based on the function body kind
        FunctionMetadata metadata = functionDefinitionNode.functionBody().kind() == SyntaxKind.EXPRESSION_FUNCTION_BODY
                ? new FunctionMetadata(
                    NodeKind.DATA_MAPPER_DEFINITION,
                    DataMapperDefinitionBuilder.PARAMETERS_LABEL,
                    DataMapperDefinitionBuilder.PARAMETERS_DOC,
                    false,
                    DataMapperDefinitionBuilder.RECORD_TYPE)
                : isNpFunction
                    ? new FunctionMetadata(
                        NodeKind.NP_FUNCTION_DEFINITION,
                        NPFunctionDefinitionBuilder.PARAMETERS_LABEL,
                        NPFunctionDefinitionBuilder.PARAMETERS_DOC,
                        true,
                        null)
                    : new FunctionMetadata(
                        NodeKind.FUNCTION_DEFINITION,
                        FunctionDefinitionBuilder.PARAMETERS_LABEL,
                        FunctionDefinitionBuilder.PARAMETERS_DOC,
                        true,
                        null);
        NodeBuilder nodeBuilder = NodeBuilder.getNodeFromKind(metadata.nodeKind)
                .defaultModuleName(this.moduleInfo);

        // Set the line range of the function definition node
        LineRange functionKeywordLineRange = functionDefinitionNode.functionKeyword().lineRange();
        nodeBuilder.codedata().lineRange(LineRange.from(
                functionKeywordLineRange.fileName(),
                functionKeywordLineRange.startLine(),
                functionDefinitionNode.functionBody().lineRange().startLine()));

        // Set the function name, return type and nested properties
        nodeBuilder.properties()
                .functionName(functionDefinitionNode.functionName())
                .returnType(
                        functionDefinitionNode.functionSignature().returnTypeDesc()
                                .map(type -> type.type().toSourceCode().strip())
                                .orElse(""),
                        metadata.returnTypeConstraint,
                        metadata.returnTypeConstraint == null)
                .nestedProperty();

        boolean isContextParamAvailable = false;
        String npPromptDefaultValue = null;

        // Set the function parameters
        for (ParameterNode parameter : functionDefinitionNode.functionSignature().parameters()) {
            String paramType;
            Optional<Token> paramName;
            switch (parameter.kind()) {
                case REQUIRED_PARAM -> {
                    RequiredParameterNode reqParam = (RequiredParameterNode) parameter;
                    paramType = getNodeValue(reqParam.typeName());
                    paramName = reqParam.paramName();
                }
                case DEFAULTABLE_PARAM -> {
                    DefaultableParameterNode defParam = (DefaultableParameterNode) parameter;
                    paramType = getNodeValue(defParam.typeName());
                    paramName = defParam.paramName();
                }
                case REST_PARAM -> {
                    RestParameterNode restParam = (RestParameterNode) parameter;
                    paramType = getNodeValue(restParam.typeName()) + restParam.ellipsisToken().text();
                    paramName = restParam.paramName();
                }
                default -> {
                    continue;
                }
            }
            if (isNpFunctionProperty(parameter)) {
                if (Constants.NaturalFunctions.CONTEXT.equals(paramName.get().text())) {
                    isContextParamAvailable = true;
                } else if (Constants.NaturalFunctions.PROMPT.equals(paramName.get().text())) {
                    npPromptDefaultValue = ((DefaultableParameterNode) parameter).expression().toSourceCode();
                }

                continue;
            }
            nodeBuilder.properties().parameter(paramType, paramName.map(Token::text).orElse(""),
                    paramName.orElse(null));
        }

        nodeBuilder.properties().endNestedProperty(
                Property.ValueType.REPEATABLE_PROPERTY,
                Property.PARAMETERS_KEY,
                metadata.parametersLabel,
                metadata.parametersDoc,
                FunctionDefinitionBuilder.getParameterSchema(),
                metadata.optionalParameters);

        if (isNpFunction) {
            processNpFunctionDefinitionProperties(nodeBuilder, npPromptDefaultValue, isContextParamAvailable);
        }

        // Build the definition node
        this.node = gson.toJsonTree(nodeBuilder.build());
    }

    private void processNpFunctionDefinitionProperties(NodeBuilder nodeBuilder, String promptDefaultValue,
                                                       boolean isContextAvailable) {
        // Set the 'prompt' property
        nodeBuilder.properties().custom()
                .metadata()
                    .label(Constants.NaturalFunctions.PROMPT_LABEL)
                    .description(Constants.NaturalFunctions.PROMPT_DESCRIPTION)
                    .stepOut()
                .codedata()
                    .kind(ParameterData.Kind.REQUIRED.name())
                    .stepOut()
                .typeConstraint(Constants.NaturalFunctions.MODULE_PREFIXED_PROMPT_TYPE)
                .value(promptDefaultValue)
                .editable()
                .hidden()
                .type(Property.ValueType.RAW_TEMPLATE)
                .stepOut()
                .addProperty(Constants.NaturalFunctions.PROMPT);

        // Set the `context` property if enabled
        if (isContextAvailable) {
            nodeBuilder.properties().custom()
                    .metadata()
                        .label(Constants.NaturalFunctions.CONTEXT_LABEL)
                        .description(Constants.NaturalFunctions.CONTEXT_DESCRIPTION)
                        .stepOut()
                    .codedata()
                        .kind(ParameterData.Kind.REQUIRED.name())
                    .stepOut()
                    .typeConstraint(Constants.NaturalFunctions.MODULE_PREFIXED_CONTEXT_TYPE)
                    .editable()
                    .optional(true)
                    .advanced(true)
                    .hidden()
                    .type(Property.ValueType.EXPRESSION)
                    .stepOut()
                    .addProperty(Constants.NaturalFunctions.CONTEXT);
        }

        // set the `enableModelContext` property
        nodeBuilder.properties().custom()
                .metadata()
                    .label(Constants.NaturalFunctions.ENABLE_MODEL_CONTEXT_LABEL)
                    .description(Constants.NaturalFunctions.ENABLE_MODEL_CONTEXT_DESCRIPTION)
                    .stepOut()
                .editable()
                .value(isContextAvailable)
                .optional(true)
                .advanced(true)
                .type(Property.ValueType.FLAG)
                .stepOut()
                .addProperty(Constants.NaturalFunctions.ENABLE_MODEL_CONTEXT);
    }

    private static String getNodeValue(Node node) {
        return node.toSourceCode().strip();
    }

    public JsonElement getNode() {
        return this.node;
    }

    private record FunctionMetadata(
            NodeKind nodeKind,
            String parametersLabel,
            String parametersDoc,
            boolean optionalParameters,
            String returnTypeConstraint) {
    }

    // Utils
    /**
     * Check whether the given function is a prompt as code function.
     *
     * @param functionDefinitionNode Function definition node
     * @return true if the function is a prompt as code function else false
     */
    private boolean isNpFunction(FunctionDefinitionNode functionDefinitionNode) {
        Optional<Symbol> funcSymbol = this.semanticModel.symbol(functionDefinitionNode);

        if (funcSymbol.isEmpty() || funcSymbol.get().kind() != SymbolKind.FUNCTION
                || !((FunctionSymbol) funcSymbol.get()).external()) {
            return false;
        }

        return CommonUtils.isNpFunction(((ExternalFunctionSymbol) funcSymbol.get()));
    }


    /**
     * Check whether a particular function parameter is a NP function property.
     * e.g. np:Prompt and np:Model are NP function properties.
     *
     * @return true if the function parameter is a NP function property else false
     */
    private boolean isNpFunctionProperty(ParameterNode parameterNode) {
        Optional<Token> paramName;
        if (parameterNode.kind() == SyntaxKind.REQUIRED_PARAM) {
            RequiredParameterNode reqParam = (RequiredParameterNode) parameterNode;
            paramName = reqParam.paramName();
        } else if (parameterNode.kind() == SyntaxKind.DEFAULTABLE_PARAM) {
            DefaultableParameterNode defParam = (DefaultableParameterNode) parameterNode;
            paramName = defParam.paramName();
        } else {
            return false;
        }

        if (paramName.isEmpty() ||
                (!Constants.NaturalFunctions.PROMPT.equals(paramName.get().text())
                        && !Constants.NaturalFunctions.CONTEXT.equals(paramName.get().text()))) {
            return false;
        }

        Optional<Symbol> paramSymbol = this.semanticModel.symbol(parameterNode);
        if (paramSymbol.isEmpty()) {
            return false;
        }

        TypeSymbol typeDesc = ((ParameterSymbol) paramSymbol.get()).typeDescriptor();
        return CommonUtils.isNpModule(typeDesc) && typeDesc.getName().isPresent()
                && (Constants.NaturalFunctions.PROMPT_TYPE_NAME.equals(typeDesc.getName().get())
                    || Constants.NaturalFunctions.CONTEXT_TYPE_NAME.equals(typeDesc.getName().get()));
    }
}
