/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.feature.support.json;

import org.apache.sling.feature.Configuration;

import java.util.Arrays;
import java.util.List;

public abstract class JSONConstants {

    public static final String FEATURE_ID = "id";

    public static final String FEATURE_VARIABLES = "variables";

    public static final String FEATURE_BUNDLES = "bundles";

    public static final String FEATURE_FRAMEWORK_PROPERTIES = "framework-properties";

    public static final String FEATURE_CONFIGURATIONS = "configurations";

    public static final String FEATURE_INCLUDES = "includes";

    public static final String FEATURE_REQUIREMENTS = "requirements";

    public static final String FEATURE_CAPABILITIES = "capabilities";

    public static final String FEATURE_TITLE = "title";

    public static final String FEATURE_DESCRIPTION = "description";

    public static final String FEATURE_VENDOR = "vendor";

    public static final String FEATURE_LICENSE = "license";

    public static final String FEATURE_MODEL_VERSION = "model-version";

    public static final List<String> FEATURE_KNOWN_PROPERTIES = Arrays.asList(FEATURE_ID,
            FEATURE_MODEL_VERSION,
            FEATURE_VARIABLES,
            FEATURE_BUNDLES,
            FEATURE_FRAMEWORK_PROPERTIES,
            FEATURE_CONFIGURATIONS,
            FEATURE_INCLUDES,
            FEATURE_REQUIREMENTS,
            FEATURE_CAPABILITIES,
            FEATURE_TITLE,
            FEATURE_DESCRIPTION,
            FEATURE_VENDOR,
            FEATURE_LICENSE);

    public static final String ARTIFACT_ID = "id";

    public static final List<String> ARTIFACT_KNOWN_PROPERTIES = Arrays.asList(ARTIFACT_ID,
            Configuration.PROP_ARTIFACT,
            FEATURE_CONFIGURATIONS);

    public static final String INCLUDE_REMOVALS = "removals";

    public static final String INCLUDE_EXTENSION_REMOVALS = "extensions";

    public static final String REQCAP_NAMESPACE = "namespace";
    public static final String REQCAP_ATTRIBUTES = "attributes";
    public static final String REQCAP_DIRECTIVES = "directives";

    public static final String APP_FRAMEWORK = "frameworkId";
    public static final String APP_FEATURES = "features";

    public static final List<String> APP_KNOWN_PROPERTIES = Arrays.asList(APP_FRAMEWORK,
            FEATURE_BUNDLES,
            FEATURE_FRAMEWORK_PROPERTIES,
            FEATURE_CONFIGURATIONS,
            APP_FEATURES);
}
