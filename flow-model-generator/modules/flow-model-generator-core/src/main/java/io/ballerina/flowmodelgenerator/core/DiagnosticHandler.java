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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.flowmodelgenerator.core.model.Diagnostics;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.langserver.common.utils.PositionUtil;

import java.util.Iterator;

/**
 * Handles diagnostics for a given flow model.
 *
 * @since 1.4.0
 */
public class DiagnosticHandler {

    private final Iterator<Diagnostic> iterator;
    private Diagnostic currentDiagnostic;

    public DiagnosticHandler(SemanticModel semanticModel) {
        iterator = semanticModel.diagnostics().iterator();
        if (iterator.hasNext()) {
            currentDiagnostic = iterator.next();
        }
    }

    public void handle(DiagnosticCapable builder, LineRange nodeLineRange, boolean isLeafNode) {
        if (currentDiagnostic == null) {
            return;
        }
        LinePosition nodeStartLine = nodeLineRange.startLine();

        while (currentDiagnostic != null) {
            // Check whether the diagnostic is passed the node, and obtain the diagnostic line range
            LineRange diagnosticLineRange = currentDiagnostic.location().lineRange();
            LinePosition diagnosticEndLine = diagnosticLineRange.endLine();
            while (hasDiagnosticPassed(nodeStartLine, diagnosticEndLine)) {
                if (iterator.hasNext()) {
                    currentDiagnostic = iterator.next();
                    diagnosticLineRange = currentDiagnostic.location().lineRange();
                    continue;
                }
                currentDiagnostic = null;
                return;
            }

            boolean isNodeWithinDiagnostic = PositionUtil.isWithinLineRange(nodeLineRange, diagnosticLineRange);
            boolean isDiagnosticWithinNode = PositionUtil.isWithinLineRange(diagnosticLineRange, nodeLineRange);

            // Both node and diagnostic are within the same range
            if (isNodeWithinDiagnostic && isDiagnosticWithinNode) {
                addDiagnostic(builder);
                currentDiagnostic = iterator.hasNext() ? iterator.next() : null;
                continue;
            }

            // Node is within the diagnostic range
            if (isNodeWithinDiagnostic) {
                addDiagnostic(builder);
                return;
            }

            // Diagnostic is within the node range
            if (isDiagnosticWithinNode) {
                if (isLeafNode) {
                    addDiagnostic(builder);
                    currentDiagnostic = iterator.hasNext() ? iterator.next() : null;
                    continue;
                }
                builder.diagnostics().hasDiagnostics();
            }
            return;
        }
    }

    private void addDiagnostic(DiagnosticCapable builder) {
        builder.diagnostics()
                .diagnostic(currentDiagnostic.diagnosticInfo().severity(), currentDiagnostic.message());
    }

    private static boolean hasDiagnosticPassed(LinePosition nodeStartLine, LinePosition diagnosticEndLine) {
        return nodeStartLine.line() > diagnosticEndLine.line() || (nodeStartLine.line() == diagnosticEndLine.line() &&
                nodeStartLine.offset() > diagnosticEndLine.offset());
    }

    public interface DiagnosticCapable {

        Diagnostics.Builder<?> diagnostics();
    }
}