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
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
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
        if (reg != null)
            reg.unregister();

        if (properties == null)
            return;

        Map<Map.Entry<String, Version>, String> bundleIDFeatures = new HashMap<>();
        Dictionary<String, String> props = new Hashtable<>();
        for(Enumeration<String> e = properties.keys(); e.hasMoreElements(); ) {
            String key = e.nextElement();
            if (key.startsWith("."))
                continue;

            if (Constants.SERVICE_PID.equals(key))
                continue;

            String[] bsnver = key.split(":");
            if (bsnver.length != 2)
                continue;

            try {
                Version ver = Version.valueOf(bsnver[1]);

                String value = getStringPlus(properties.get(key));
                AbstractMap.SimpleEntry<String, Version> newKey = new AbstractMap.SimpleEntry<>(bsnver[0], ver);
                bundleIDFeatures.put(newKey, value);
                props.put(key, value);
            } catch (IllegalArgumentException iae) {
                // TODO log
            }
        }

        FeaturesServiceImpl fs = new FeaturesServiceImpl(bundleIDFeatures);
        reg = bundleContext.registerService(Features.class, fs, props);
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
