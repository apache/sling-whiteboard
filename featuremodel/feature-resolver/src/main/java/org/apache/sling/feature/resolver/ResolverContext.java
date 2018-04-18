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
package org.apache.sling.feature.resolver;

import org.apache.sling.feature.Feature;

import java.util.ArrayList;
import java.util.List;

public class ResolverContext {

    private final List<Feature> requiredFeatures = new ArrayList<>();

    private final List<Feature> optionalFeatures = new ArrayList<>();

    public List<Feature> getRequiredFeatures() {
        return requiredFeatures;
    }

    public List<Feature> getOptionalFeatures() {
        return optionalFeatures;
    }

    public void addRequiredFeatures(final List<Feature> features) {
        this.requiredFeatures.addAll(features);
    }

    public void addOptionalFeatures(final List<Feature> features) {
        this.optionalFeatures.addAll(features);
    }
}

