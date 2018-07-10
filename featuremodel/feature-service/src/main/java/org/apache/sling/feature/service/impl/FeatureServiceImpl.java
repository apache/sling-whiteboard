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

import org.apache.sling.feature.service.Features;
import org.osgi.framework.Version;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class FeatureServiceImpl implements Features {
    private final Set<String> features;
    private final Map<Map.Entry<String, Version>, String> bundleFeatureMap;

    FeatureServiceImpl(Map<Map.Entry<String, Version>, String> bundleIDFeatures) {
        bundleFeatureMap = Collections.unmodifiableMap(bundleIDFeatures);

        Set<String> fs = new HashSet<>(bundleIDFeatures.values());
        features = Collections.unmodifiableSet(fs);
    }

    @Override
    public Collection<String> listFeatures() {
        return features;
    }

    @Override
    public String getFeatureForBundle(String bsn, Version version) {
        return bundleFeatureMap.get(new AbstractMap.SimpleEntry<String, Version>(bsn, version));
    }
}
