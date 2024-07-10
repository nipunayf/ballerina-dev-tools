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
import io.ballerina.flowmodelgenerator.core.model.Branch;
import io.ballerina.flowmodelgenerator.core.model.Expression;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.tools.text.LineRange;

import java.util.List;
import java.util.Map;


/**
 * Represents the properties of a fail node.
 *
 * @since 1.4.0
 */
public class Fail extends FlowNode {
    public static final String FAIL_LABEL = "Fail";
    private static final String FAIL_EXPRESSION = "Expression";
    private static final String FAIL_EXPRESSION_KEY = "expression";
    public static final String FAIL_EXPRESSION_DOC = "Fail value";
    public static final Fail DEFAULT_NODE = new Fail(DEFAULT_ID, FAIL_LABEL, Kind.FAIL, false,
            Map.of(
                    FAIL_EXPRESSION_KEY,
                    Expression.Builder.getInstance()
                            .label(FAIL_EXPRESSION)
                            .value("")
                            .documentation(FAIL_EXPRESSION_DOC)
                            .typeKind(Expression.ExpressionTypeKind.BTYPE)
                            .editable()
                            .build()
            ), null, false, List.of(), 0);

    public Fail(String id, String label, Kind kind, boolean fixed, Map<String, Expression> nodeProperties,
                LineRange lineRange, boolean returning, List<Branch> branches, int flags) {
        super(id, label, kind, fixed, nodeProperties, lineRange, returning, branches, flags);
    }

    @Override
    public String toSource() {
        SourceBuilder sourceBuilder = new SourceBuilder();

        sourceBuilder.keyword(SyntaxKind.FAIL_KEYWORD);
        Expression expression = getProperty(FAIL_EXPRESSION_KEY);
        if (expression != null) {
            sourceBuilder
                    .whiteSpace()
                    .expression(expression);
        }
        sourceBuilder.endOfStatement();
        return sourceBuilder.build(false);
    }
}