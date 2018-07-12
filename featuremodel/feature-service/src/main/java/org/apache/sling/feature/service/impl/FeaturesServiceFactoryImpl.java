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
import org.apache.sling.feature.service.FeaturesFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

class FeaturesServiceFactoryImpl implements FeaturesFactory {
    private final BundleContext bundleContext;

    public FeaturesServiceFactoryImpl(BundleContext context) {
        bundleContext = context;
    }

    @Override
    public void initialize(Map<String, String> bfm) {
        Map<Entry<String, Version>, String> bundleFeatureMapping = new HashMap<>();
        for (Map.Entry<String, String> entry : bfm.entrySet()) {
            String[] bv = entry.getKey().split(":");
            if (bv.length == 2) {
                try {
                    Map.Entry<String, Version> k = new AbstractMap.SimpleEntry<String, Version>(
                            bv[0], Version.parseVersion(bv[1]));
                    bundleFeatureMapping.put(k, entry.getValue());
                } catch (IllegalArgumentException iae) {
                    // TODO log
                }
            }
        }

        Features fs = new FeaturesServiceImpl(bundleFeatureMapping);
        bundleContext.registerService(Features.class, fs, new Hashtable<>(bfm));
    }
}
