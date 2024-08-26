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

package io.ballerina.flowmodelgenerator.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.projects.DiagnosticResult;
import io.ballerina.projects.Document;
import io.ballerina.projects.Project;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocumentChange;
import io.ballerina.tools.text.TextRange;
import org.ballerinalang.util.diagnostic.DiagnosticErrorCode;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates text edits for the nodes that are requested to delete.
 *
 * @since 1.4.0
 */
public class DeleteNodeGenerator {

    private final Gson gson;
    private final FlowNode nodeToDelete;
    private final Path filePath;

    public DeleteNodeGenerator(JsonElement nodeToDelete, Path filePath) {
        this.gson = new Gson();
        this.nodeToDelete = new Gson().fromJson(nodeToDelete, FlowNode.class);
        this.filePath = filePath;
    }

    public JsonElement getTextEditsToDeletedNode(Document document, Project project) {
        LineRange lineRange = nodeToDelete.codedata().lineRange();
        TextDocument textDocument = document.textDocument();
        int startTextPosition = textDocument.textPositionFrom(lineRange.startLine());
        int endTextPosition = textDocument.textPositionFrom(lineRange.endLine());
        io.ballerina.tools.text.TextEdit te = io.ballerina.tools.text.TextEdit.from(TextRange.from(startTextPosition,
                endTextPosition - startTextPosition), "");
        TextDocument apply = textDocument
                .apply(TextDocumentChange.from(List.of(te).toArray(new io.ballerina.tools.text.TextEdit[0])));
        Document modifiedDoc =
                project.duplicate().currentPackage().module(document.module().moduleId())
                        .document(document.documentId()).modify().withContent(String.join(System.lineSeparator(),
                                apply.textLines())).apply();
        ModulePartNode modulePartNode = modifiedDoc.syntaxTree().rootNode();
        NodeList<ImportDeclarationNode> imports = modulePartNode.imports();

        List<TextEdit> textEdits = new ArrayList<>();
        DiagnosticResult diagnostics = modifiedDoc.module().getCompilation().diagnostics();
        for (Diagnostic diagnostic : diagnostics.diagnostics()) {
            DiagnosticInfo diagnosticInfo = diagnostic.diagnosticInfo();
            if (diagnostic.diagnosticInfo().severity() == DiagnosticSeverity.ERROR &&
                    diagnosticInfo.code().equals(DiagnosticErrorCode.UNUSED_MODULE_PREFIX.diagnosticId())) {
                ImportDeclarationNode importNode =
                        getUnusedImport(String.valueOf(diagnostic.properties().get(0).value()), imports);
                TextEdit deleteImportTextEdit = new TextEdit(CommonUtils.toRange(importNode.lineRange()), "");
                textEdits.add(deleteImportTextEdit);
            }
        }

        TextEdit textEdit = new TextEdit(CommonUtils.toRange(lineRange), "");
        textEdits.add(textEdit);
        Map<Path, List<TextEdit>> textEditsMap = new HashMap<>();
        textEditsMap.put(filePath, textEdits);
        return gson.toJsonTree(textEditsMap);
    }

    private ImportDeclarationNode getUnusedImport(String unusedModule, NodeList<ImportDeclarationNode> imports) {
        for (ImportDeclarationNode importNode : imports) {
            if (importNode.moduleName().get(0).text().equals(unusedModule)) {
                return importNode;
            }
        }
        throw new IllegalStateException("There should be an import node");
    }
}
