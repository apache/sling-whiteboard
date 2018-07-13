/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.feature.service;

import java.util.Map;
import java.util.Set;

/**
 * Service to initialize the {@link Features} service.
 */
public interface FeaturesFactory {
    /**
     * Initialize the Features service. The implementation of this
     * factory service will register a {@link Features} service
     * based on the mapping provided.
     * @param bundleFeatureMapping A mapping from a bundle to the features
     * that this bundle belongs to. If a bundle is not part of a feature
     * this must be represented by a {@code null} value in the set.
     * Bundles are listed using the {@code symbolic-name:version} syntax.
     * Features are described using the maven ID syntax:
     * {@code groupID:artifactID:type:classifier:version} where
     * type and classifier are optional.
     */
    void initialize(Map<String, Set<String>> bundleFeatureMapping);
}
