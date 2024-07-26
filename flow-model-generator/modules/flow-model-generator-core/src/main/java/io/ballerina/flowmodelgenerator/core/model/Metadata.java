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

import java.util.List;

/**
 * Represents the metadata of a diagram component.
 *
 * @param label       The label of the component
 * @param description The description of the component
 * @param keywords    The keywords of the component
 * @since 1.5.0
 */
public record Metadata(String label, String description, List<String> keywords) {

    public static class Builder<T> extends FacetedBuilder<T> {

        private String label;
        private String description;
        private List<String> keywords;

        public Builder(T parentBuilder) {
            super(parentBuilder);
        }

        public Builder<T> label(String label) {
            this.label = label;
            return this;
        }

        public Builder<T> description(String description) {
            this.description = description;
            return this;
        }

        public Builder<T> keywords(List<String> keywords) {
            this.keywords = keywords;
            return this;
        }

        public Metadata build() {
            return new Metadata(label, description, keywords);
        }
    }
}
