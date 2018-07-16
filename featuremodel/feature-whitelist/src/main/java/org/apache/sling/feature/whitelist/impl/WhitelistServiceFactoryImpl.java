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

import org.apache.sling.feature.service.Features;
import org.apache.sling.feature.whitelist.WhitelistService;
import org.apache.sling.feature.whitelist.WhitelistServiceFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.util.tracker.ServiceTracker;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

public class WhitelistServiceFactoryImpl implements WhitelistServiceFactory {
    private final BundleContext bundleContext;
    private final ServiceTracker<Features, Features> featuresServiceTracker;

    WhitelistServiceFactoryImpl(BundleContext context,
            ServiceTracker<Features, Features> tracker) {
        bundleContext = context;
        featuresServiceTracker = tracker;
    }

    @Override
    public void initialize(Map<String, Map<String, Set<String>>> mappings) {
        Map<String, Set<String>> packages = mappings.get("packages");
        Map<String, Set<String>> regions = mappings.get("regions");

        WhitelistService wls = createWhitelistService(packages, regions);
        WhitelistEnforcer enforcer = new WhitelistEnforcer(wls, featuresServiceTracker);
        Hashtable<String, Set<String>> props = new Hashtable<>(packages);
        props.putAll(regions);
        bundleContext.registerService(ResolverHookFactory.class, enforcer, props);
    }

    WhitelistService createWhitelistService(Map<String, Set<String>> packages, Map<String, Set<String>> regions) {
        return new WhitelistServiceImpl(packages, regions);
    }
}
