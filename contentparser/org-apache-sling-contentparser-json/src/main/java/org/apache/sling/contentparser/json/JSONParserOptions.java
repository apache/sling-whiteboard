/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Licensed to the Apache Software Foundation (ASF) under one
 ~ or more contributor license agreements.  See the NOTICE file
 ~ distributed with this work for additional information
 ~ regarding copyright ownership.  The ASF licenses this file
 ~ to you under the Apache License, Version 2.0 (the
 ~ "License"); you may not use this file except in compliance
 ~ with the License.  You may obtain a copy of the License at
 ~
 ~   http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package org.apache.sling.contentparser.json;

import java.util.Arrays;
import java.util.EnumSet;

import org.apache.sling.contentparser.api.ParserOptions;
import org.osgi.annotation.versioning.ConsumerType;

@ConsumerType
public final class JSONParserOptions extends ParserOptions {

    /**
     * List of JSON parser features activated by default.
     */
    public static final EnumSet<JSONParserFeature> DEFAULT_JSON_PARSER_FEATURES
            = EnumSet.of(JSONParserFeature.COMMENTS);

    private EnumSet<JSONParserFeature> features = DEFAULT_JSON_PARSER_FEATURES;

    /**
     * Set set of features the JSON parser should apply when parsing files.
     *
     * @param value JSON parser features
     * @return this
     */
    public JSONParserOptions withFeatures(EnumSet<JSONParserFeature> value) {
        this.features = value;
        return this;
    }

    public JSONParserOptions withFeatures(JSONParserFeature... value) {
        this.features = EnumSet.copyOf(Arrays.asList(value));
        return this;
    }

    public EnumSet<JSONParserFeature> getFeatures() {
        return features;
    }
}
