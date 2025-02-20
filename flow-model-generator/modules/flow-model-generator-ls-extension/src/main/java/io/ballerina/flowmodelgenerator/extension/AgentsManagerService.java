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

package io.ballerina.flowmodelgenerator.extension;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.flowmodelgenerator.core.AgentsGenerator;
import io.ballerina.flowmodelgenerator.extension.request.GenToolRequest;
import io.ballerina.flowmodelgenerator.extension.request.GetAllModelsRequest;
import io.ballerina.flowmodelgenerator.extension.request.GetModelsRequest;
import io.ballerina.flowmodelgenerator.extension.request.GetToolsRequest;
import io.ballerina.flowmodelgenerator.extension.response.GenToolResponse;
import io.ballerina.flowmodelgenerator.extension.response.GetAgentsResponse;
import io.ballerina.flowmodelgenerator.extension.response.GetModelsResponse;
import io.ballerina.flowmodelgenerator.extension.response.GetToolsResponse;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.eclipse.lsp4j.services.LanguageServer;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@JavaSPIService("org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService")
@JsonSegment("agentsManager")
public class AgentsManagerService implements ExtendedLanguageServerService {
    private WorkspaceManager workspaceManager;

    @Override
    public void init(LanguageServer langServer, WorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    @Override
    public Class<?> getRemoteInterface() {
        return null;
    }

    @JsonRequest
    public CompletableFuture<GetAgentsResponse> getAllAgents() {
        return CompletableFuture.supplyAsync(() -> {
            GetAgentsResponse response = new GetAgentsResponse();
            try {
                AgentsGenerator agentsGenerator = new AgentsGenerator();
                response.setAgents(agentsGenerator.getAllAgents());
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<GetModelsResponse> getAllModels(GetAllModelsRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            GetModelsResponse response = new GetModelsResponse();
            try {
                AgentsGenerator agentsGenerator = new AgentsGenerator();
                response.setModels(agentsGenerator.getAllModels(request.agent()));
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<GetModelsResponse> getModels(GetModelsRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            GetModelsResponse response = new GetModelsResponse();
            try {
                Path filePath = Path.of(request.filePath());
                this.workspaceManager.loadProject(filePath);
                Optional<SemanticModel> semanticModel = this.workspaceManager.semanticModel(filePath);
                if (semanticModel.isEmpty()) {
                    return response;
                }
                AgentsGenerator agentsGenerator = new AgentsGenerator();
                response.setModels(agentsGenerator.getModels(semanticModel.get(), request.agent()));
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<GetToolsResponse> getTools(GetToolsRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            GetToolsResponse response = new GetToolsResponse();
            try {
                Path filePath = Path.of(request.filePath());
                this.workspaceManager.loadProject(filePath);
                Optional<SemanticModel> semanticModel = this.workspaceManager.semanticModel(filePath);
                if (semanticModel.isEmpty()) {
                    return response;
                }
                AgentsGenerator agentsGenerator = new AgentsGenerator();
                response.setTools(agentsGenerator.getTools(semanticModel.get()));
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<GenToolResponse> genTool(GenToolRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            GenToolResponse response = new GenToolResponse();
            try {
                Path filePath = Path.of(request.filePath());
                this.workspaceManager.loadProject(filePath);
                Optional<SemanticModel> semanticModel = this.workspaceManager.semanticModel(filePath);
                if (semanticModel.isEmpty()) {
                    return response;
                }
                AgentsGenerator agentsGenerator = new AgentsGenerator();
                response.setTextEdits(agentsGenerator.genTool(request.flowNode(), request.toolName(), filePath,
                        this.workspaceManager));
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            return response;
        });
    }
}
