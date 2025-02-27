/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
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

package io.ballerina.flowmodelgenerator.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.FormBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * This class is responsible for generating types from the semantic model.
 *
 * @since 2.0.0
 */
public class AgentsGenerator {

    private final Gson gson;
    private final SemanticModel semanticModel;
    private static final Map<String, Set<String>> modelsForAgent = Map.of("FunctionCallAgent", Set.of("ChatGptModel",
            "AzureChatGptModel"), "ReActAgent", Set.of("ChatGptModel", "AzureChatGptModel"));
    private static final String WSO2 = "wso2";
    private static final String BALLERINAX = "ballerinax";
    private static final String AI_AGENT = "ai.agent";
    private static final String INIT = "init";
    private static final String AGENT_FILE = "agents.bal";
    public static final String BASE_AGENT = "BaseAgent";

    public AgentsGenerator() {
        this.gson = new Gson();
        this.semanticModel = null;
    }

    public AgentsGenerator(SemanticModel semanticModel) {
        this.gson = new Gson();
        this.semanticModel = semanticModel;
    }

    public JsonArray getAllAgents() {
        List<Codedata> agents = new ArrayList<>();
        ModuleSymbol agentModule = getAgentModule();
        for (ClassSymbol classSymbol : agentModule.classes()) {
            List<TypeSymbol> typeInclusions = classSymbol.typeInclusions();
            for (TypeSymbol typeInclusion : typeInclusions) {
                if (typeInclusion.getName().isPresent() && typeInclusion.getName().get().equals(BASE_AGENT)) {
                    agents.add(new Codedata.Builder<>(null).node(NodeKind.AGENT)
                            .org(BALLERINAX)
                            .module(AI_AGENT)
                            .object(classSymbol.getName().orElse(BASE_AGENT))
                            .symbol(INIT)
                            .build());
                    break;
                }
            }
        }

        return gson.toJsonTree(agents).getAsJsonArray();
    }

    private ModuleSymbol getAgentModule() {
        assert semanticModel != null;
        for (Symbol symbol : semanticModel.moduleSymbols()) {
            if (symbol.kind() == SymbolKind.MODULE) {
                ModuleSymbol modSymbol = (ModuleSymbol) symbol;
                if (modSymbol.id().orgName().equals(BALLERINAX) && modSymbol.id().packageName().equals(AI_AGENT)) {
                    return modSymbol;
                }
            }
        }
        throw new IllegalStateException("Agent module not found");
    }

    public JsonArray getAllModels(String agent) {
        Codedata.Builder<Object> codedataBuilder = new Codedata.Builder<>(null);
        Codedata chatGptModel = codedataBuilder.node(NodeKind.CLASS)
                .org(WSO2)
                .module(AI_AGENT)
                .object("ChatGptModel")
                .symbol(INIT)
                .build();
        Codedata azureChatGptModel = codedataBuilder.node(NodeKind.CLASS)
                .org(WSO2)
                .module(AI_AGENT)
                .object("AzureChatGptModel")
                .symbol(INIT)
                .build();
        if (agent.equals("FunctionCallAgent")) {
            List<Codedata> models = List.of(chatGptModel, azureChatGptModel);
            return gson.toJsonTree(models).getAsJsonArray();
        } else if (agent.equals("ReActAgent")) {
            List<Codedata> models = List.of(chatGptModel, azureChatGptModel);
            return gson.toJsonTree(models).getAsJsonArray();
        }
        throw new IllegalStateException(String.format("Agent %s is not supported", agent));
    }

    public JsonArray getModels(SemanticModel semanticModel, String agent) {
        List<Symbol> moduleSymbols = semanticModel.moduleSymbols();
        Set<String> models = modelsForAgent.get(agent);
        if (models == null) {
            throw new IllegalStateException(String.format("Cannot find models for agent %s", agent));
        }
        List<String> availableModels = new ArrayList<>();
        for (Symbol moduleSymbol : moduleSymbols) {
            if (moduleSymbol.kind() != SymbolKind.VARIABLE) {
                continue;
            }
            VariableSymbol variableSymbol = (VariableSymbol) moduleSymbol;
            TypeSymbol typeSymbol = variableSymbol.typeDescriptor();
            String signature = typeSymbol.signature();
            if (models.contains(signature)) {
                availableModels.add(signature);
            }
        }
        return gson.toJsonTree(availableModels).getAsJsonArray();
    }

    public JsonArray getTools(SemanticModel semanticModel) {
        List<Symbol> moduleSymbols = semanticModel.moduleSymbols();
        List<String> functionNames = new ArrayList<>();
        for (Symbol moduleSymbol : moduleSymbols) {
            if (moduleSymbol.kind() == SymbolKind.FUNCTION) {
                // TODO: Filter from the annotations
                functionNames.add((moduleSymbol).getName().orElse(""));
            }
        }
        return gson.toJsonTree(functionNames).getAsJsonArray();
    }

    public JsonElement genTool(JsonElement node, String toolName, Path filePath, WorkspaceManager workspaceManager) {
        FlowNode flowNode = gson.fromJson(node, FlowNode.class);
        NodeKind nodeKind = flowNode.codedata().node();
        SourceBuilder sourceBuilder = new SourceBuilder(flowNode, workspaceManager, filePath);
        List<String> args = new ArrayList<>();
        if (nodeKind == NodeKind.FUNCTION_DEFINITION) {
            sourceBuilder.token().keyword(SyntaxKind.FUNCTION_KEYWORD);
            sourceBuilder.token().name(toolName).keyword(SyntaxKind.OPEN_PAREN_TOKEN);
            Optional<Property> parameters = flowNode.getProperty(Property.PARAMETERS_KEY);
            if (parameters.isPresent() && parameters.get().value() instanceof Map<?, ?> paramMap) {
                List<String> paramList = new ArrayList<>();
                for (Object obj : paramMap.values()) {
                    Property paramProperty = gson.fromJson(gson.toJsonTree(obj), Property.class);
                    if (!(paramProperty.value() instanceof Map<?, ?> paramData)) {
                        continue;
                    }
                    Map<String, Property> paramProperties = gson.fromJson(gson.toJsonTree(paramData),
                            FormBuilder.NODE_PROPERTIES_TYPE);

                    String paramType = paramProperties.get(Property.TYPE_KEY).value().toString();
                    String paramName = paramProperties.get(Property.VARIABLE_KEY).value().toString();
                    args.add(paramName);
                    paramList.add(paramType + " " + paramName);
                }
                sourceBuilder.token().name(String.join(", ", paramList));
            }
            sourceBuilder.token().keyword(SyntaxKind.CLOSE_PAREN_TOKEN);

            Optional<Property> returnType = flowNode.getProperty(Property.TYPE_KEY);
            if (returnType.isPresent() && !returnType.get().value().toString().isEmpty()) {
                sourceBuilder.token()
                        .keyword(SyntaxKind.RETURNS_KEYWORD)
                        .name(returnType.get().value().toString());
            }

            sourceBuilder.token().keyword(SyntaxKind.OPEN_BRACE_TOKEN);
            if (returnType.isPresent() && !returnType.get().value().toString().isEmpty()) {
                sourceBuilder.token()
                        .name(returnType.get().value().toString())
                        .whiteSpace()
                        .name("result")
                        .whiteSpace()
                        .keyword(SyntaxKind.EQUAL_TOKEN);
            }
            Optional<Property> optFuncName = flowNode.getProperty(Property.FUNCTION_NAME_KEY);
            if (optFuncName.isEmpty()) {
                throw new IllegalStateException("Function name is not present");
            }
            sourceBuilder.token()
                    .name(optFuncName.get().value().toString())
                    .keyword(SyntaxKind.OPEN_PAREN_TOKEN);
            sourceBuilder.token()
                    .name(String.join(", ", args))
                    .keyword(SyntaxKind.CLOSE_PAREN_TOKEN).endOfStatement();
            sourceBuilder.token()
                    .keyword(SyntaxKind.CLOSE_BRACE_TOKEN);
            sourceBuilder.textEdit(false, AGENT_FILE, false);
            return gson.toJsonTree(sourceBuilder.build());
        } else if (nodeKind == NodeKind.REMOTE_ACTION_CALL) {
            sourceBuilder.token().keyword(SyntaxKind.FUNCTION_KEYWORD);
            sourceBuilder.token().name(toolName).keyword(SyntaxKind.OPEN_PAREN_TOKEN);

            Map<String, Property> properties = flowNode.properties();
            Set<String> keys = new LinkedHashSet<>(properties != null ? properties.keySet() : Set.of());
            keys.removeAll(Set.of(Property.VARIABLE_KEY, Property.TYPE_KEY, Property.CONNECTION_KEY,
                    Property.CHECK_ERROR_KEY));
            List<String> paramList = new ArrayList<>();
            for (String key : keys) {
                Property property = properties.get(key);
                if (property == null) {
                    continue;
                }
                String paramType = property.valueTypeConstraint().toString();
                paramList.add(paramType + " " + key);
            }
            sourceBuilder.token().name(String.join(", ", paramList));
            sourceBuilder.token().keyword(SyntaxKind.CLOSE_PAREN_TOKEN);

            Optional<Property> returnType = flowNode.getProperty(Property.TYPE_KEY);
            if (returnType.isPresent() && !returnType.get().value().toString().isEmpty()) {
                sourceBuilder.token()
                        .keyword(SyntaxKind.RETURNS_KEYWORD)
                        .name(returnType.get().value().toString());
            }

            sourceBuilder.token().keyword(SyntaxKind.OPEN_BRACE_TOKEN);
            sourceBuilder.token()
                    .name(flowNode.codedata().sourceCode());
            if (returnType.isPresent() && !returnType.get().value().toString().isEmpty()) {
                sourceBuilder.token()
                        .keyword(SyntaxKind.RETURN_KEYWORD)
                        .name(flowNode.getProperty(Property.VARIABLE_KEY).get().value().toString())
                        .endOfStatement();
            }
            sourceBuilder.token()
                    .keyword(SyntaxKind.CLOSE_BRACE_TOKEN);
            sourceBuilder.textEdit(false, AGENT_FILE, false);
            return gson.toJsonTree(sourceBuilder.build());
        }
        throw new IllegalStateException("Unsupported node kind to generate tool");
    }
}
