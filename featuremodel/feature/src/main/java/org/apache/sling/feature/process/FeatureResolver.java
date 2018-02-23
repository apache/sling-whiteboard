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
package org.apache.sling.feature.process;

import java.util.List;

import org.apache.sling.feature.Feature;

/**
 * A resolver that can perform operations on the feature model.
 */
public interface FeatureResolver extends AutoCloseable {
    /**
     * Order the features by their dependency chain. Each feature and its
     * components are resolved and each other feature providing the capabilities
     * needed by the feature is placed before the requiring feature in the
     * result.
     *
     * @param features
     *            The features to order.
     * @return The ordered features.
     */
    List<Feature> orderFeatures(List<Feature> features);
}
