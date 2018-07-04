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
package org.apache.sling.feature.whitelist.impl;

import org.apache.sling.feature.service.FeatureService;
import org.apache.sling.feature.whitelist.WhitelistService;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class WhitelistEnforcer implements ResolverHookFactory, ManagedService {
    private static final String CONFIG_REGION_MAPPING_PREFIX = "whitelist.region.";
    private static final String CONFIG_FEATURE_MAPPING_PREFIX = "whitelist.feature.";
    static final Logger LOG = LoggerFactory.getLogger(WhitelistEnforcer.class);

    final ServiceTracker<FeatureService, FeatureService> featureServiceTracker;
    volatile WhitelistService whitelistService = new NullWhitelistService();

    WhitelistEnforcer(ServiceTracker<FeatureService, FeatureService> tracker) {
        featureServiceTracker = tracker;
    }

    @Override
    public ResolverHook begin(Collection<BundleRevision> triggers) {
        return new ResolverHookImpl(featureServiceTracker, whitelistService);
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        if (properties == null) {
            whitelistService = new NullWhitelistService();
            return;
        }

        Map<String, Set<String>> frm = new HashMap<>();
        Map<String, Set<String>> rpm = new HashMap<>();

        for (Enumeration<String> e = properties.keys(); e.hasMoreElements(); ) {
            String key = e.nextElement().trim();

            if (key.startsWith(CONFIG_REGION_MAPPING_PREFIX)) {
                String region = key.substring(CONFIG_REGION_MAPPING_PREFIX.length());
                Set<String> packages = getStringPlusValue(properties.get(key));
                rpm.put(region, packages);
            } else if (key.startsWith(CONFIG_FEATURE_MAPPING_PREFIX)) {
                String feature = key.substring(CONFIG_FEATURE_MAPPING_PREFIX.length());
                Set<String> regions = getStringPlusValue(properties.get(key));
                frm.put(feature, regions);
            }
        }

        whitelistService = new WhitelistServiceImpl(rpm, frm);
    }

    Set<String> getStringPlusValue(Object val) {
        if (val == null)
            return null;

        if (val instanceof Collection) {
            return ((Collection<?>) val).stream().map(Object::toString)
                    .collect(Collectors.toSet());
        } else if (val instanceof String[]) {
            return new HashSet<>(Arrays.asList((String[]) val));
        }
        return Collections.singleton(val.toString());
    }

    static class NullWhitelistService implements WhitelistService {
        @Override
        public Set<String> listRegions(String feature) {
            return null;
        }

        @Override
        public Boolean regionWhitelistsPackage(String region, String packageName) {
            return null;
        }
    }

}
