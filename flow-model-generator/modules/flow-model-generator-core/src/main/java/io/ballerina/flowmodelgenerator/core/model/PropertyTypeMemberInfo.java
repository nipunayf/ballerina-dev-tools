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


package io.ballerina.flowmodelgenerator.core.model;

import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.modelgenerator.commons.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * Represents the metadata of a diagram component.
 *
 * @param type       A member type of the parameter
 * @param packageInfo The package information of the member type
 * @param kind       The kind of the type
 * @since 2.0.0
 */
public record PropertyTypeMemberInfo(String type, String packageInfo, String kind) {

    public static class Builder<T> extends FacetedBuilder<T> {

        private String type;
        private String packageInfo;
        private String kind;

        public Builder(T parentBuilder) {
            super(parentBuilder);
        }

        public Builder<T> type(String type) {
            this.type = type;
            return this;
        }

        public Builder<T> packageInfo(String packageInfo) {
            this.packageInfo = packageInfo;
            return this;
        }

        public Builder<T> kind(String kind) {
            this.kind = kind;
            return this;
        }

        public PropertyTypeMemberInfo build() {
            return new PropertyTypeMemberInfo(type, packageInfo, kind);
        }
    }
}
