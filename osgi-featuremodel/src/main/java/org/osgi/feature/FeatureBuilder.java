/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.osgi.feature;

import java.util.Map;

public interface FeatureBuilder {

    FeatureBuilder setTitle(String title);

    FeatureBuilder setVendor(String vendor);

    FeatureBuilder setLicense(String license);

    FeatureBuilder setLocation(String location);

    FeatureBuilder setComplete(boolean complete);

    FeatureBuilder setFinal(boolean isFinal);

    FeatureBuilder setDescription(String description);

    FeatureBuilder addBundles(Bundle ... bundles);

    FeatureBuilder addConfigurations(Configuration ... configs);

    FeatureBuilder addExtensions(Extension ... extensions);

    FeatureBuilder addVariable(String key, String value);

    FeatureBuilder addVariables(Map<String, String> variables);

    Feature build();

}
