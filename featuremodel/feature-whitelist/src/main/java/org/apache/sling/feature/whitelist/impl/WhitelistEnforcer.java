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
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.util.tracker.ServiceTracker;

import java.util.Collection;

class WhitelistEnforcer implements ResolverHookFactory {
    // static final Logger LOG = LoggerFactory.getLogger(WhitelistEnforcer.class);

    final ServiceTracker<Features, Features> featureServiceTracker;
    final WhitelistService whitelistService;

    WhitelistEnforcer(WhitelistService wls, ServiceTracker<Features, Features> tracker) {
        whitelistService = wls;
        featureServiceTracker = tracker;
    }

    @Override
    public ResolverHook begin(Collection<BundleRevision> triggers) {
        return new ResolverHookImpl(featureServiceTracker, whitelistService);
    }

    /*
    @Override
    public synchronized void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        if (wlsRegistration != null) {
            wlsRegistration.unregister();
            wlsRegistration = null;
        }

        if (properties == null) {
            whitelistService = null;
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
        wlsRegistration = bundleContext.registerService(WhitelistService.class, whitelistService, null);
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
    */
}
