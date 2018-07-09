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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class FeaturesServiceManager implements ManagedService {
    private final BundleContext bundleContext;
    private ServiceRegistration<Features> reg;

    FeaturesServiceManager(BundleContext context) {
        bundleContext = context;
    }

    @Override
    public synchronized void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        System.out.println("######******* Updated " + properties);
        if (reg != null)
            reg.unregister();

        if (properties == null)
            return;

        Map<String, Long> bsnVerToID = getBundleToID();

        Map<Long, String> bundleIDFeatures = new HashMap<>();
        for(Enumeration<String> e = properties.keys(); e.hasMoreElements(); ) {
            String key = e.nextElement();
            Long bid = bsnVerToID.get(key);
            if (bid != null) {
                bundleIDFeatures.put(bid, getStringPlus(properties.get(key)));
            }
        }

        FeatureServiceImpl fs = new FeatureServiceImpl(bundleIDFeatures);
        reg = bundleContext.registerService(Features.class, fs, null);
    }

    private Map<String, Long> getBundleToID() {
        Map<String, Long> m = new HashMap<>();

        for (Bundle b : bundleContext.getBundles()) {
            m.put(b.getSymbolicName() + ":" + b.getVersion(), b.getBundleId());
        }

        return m;
    }

    private String getStringPlus(Object obj) {
        if (obj instanceof String) {
            return (String) obj;
        }
        if (obj instanceof Collection) {
            Iterator<?> it = ((Collection<?>) obj).iterator();
            if (it.hasNext())
                return it.next().toString();
        }
        return null;
    }
}
