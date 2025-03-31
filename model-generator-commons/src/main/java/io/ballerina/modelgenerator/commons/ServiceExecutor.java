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

package io.ballerina.modelgenerator.commons;

import org.ballerinalang.langserver.LSClientLogger;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class ServiceExecutor {

    private final String serviceName;
    private final LSClientLogger clientLogger;

    public ServiceExecutor(String serviceName, LSClientLogger lsClientLogger) {
        this.serviceName = serviceName;
        this.clientLogger = lsClientLogger;
    }

    public <T extends AbstractFlowModelResponse> CompletableFuture<T> createAsyncTask(
            LanguageServerAsyncTask<T> responseSupplier,
            T response,
            String api) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                responseSupplier.get(response);
            } catch (Throwable e) {
                response.setError(e);
                String msg = String.format("Operation %s failed", api);
                this.clientLogger.logError(api, msg, e, (TextDocumentIdentifier) null, (Position) null);
            }
            return response;
        });
    }

    public abstract class AbstractFlowModelResponse {

        private String errorMsg;
        private String stacktrace;

        public void setError(Throwable e) {
            this.errorMsg = e.toString();
            this.stacktrace = Arrays.toString(e.getStackTrace());
        }

        public String errorMsg() {
            return errorMsg;
        }

        public String stackTrace() {
            return stacktrace;
        }
    }

    public interface LanguageServerAsyncTask<T extends AbstractFlowModelResponse> {

        void get(T response);
    }
}
