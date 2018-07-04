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
package org.apache.sling.feature.service.impl;

import org.apache.sling.feature.Feature;
import org.apache.sling.feature.service.FeatureService;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class FeatureServiceImpl implements FeatureService {
    private final Set<Feature> features;
    private final Map<Long, Feature> bundleFeatureMap;

    FeatureServiceImpl(Map<Long, Feature> bundleIDFeatures) {
        Map<Long, Feature> bfm = new HashMap<>(bundleIDFeatures);
        bundleFeatureMap = Collections.unmodifiableMap(bfm);

        Set<Feature> fs = new HashSet<>(bundleIDFeatures.values());
        features = Collections.unmodifiableSet(fs);
    }

    @Override
    public Collection<Feature> listFeatures() {
        return features;
    }

    @Override
    public Feature getFeatureForBundle(long bundleId) {
        return bundleFeatureMap.get(bundleId);
    }
}
