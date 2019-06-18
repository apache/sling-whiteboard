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
package org.apache.sling.feature.r2f.impl;

import static org.apache.felix.inventory.Format.JSON;
import static org.apache.felix.inventory.InventoryPrinter.FORMAT;
import static org.apache.felix.inventory.InventoryPrinter.NAME;
import static org.apache.felix.inventory.InventoryPrinter.TITLE;
import static org.osgi.framework.Constants.BUNDLE_VENDOR;
import static org.osgi.framework.Constants.SERVICE_DESCRIPTION;
import static org.osgi.framework.Constants.SERVICE_VENDOR;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import org.apache.felix.inventory.InventoryPrinter;
import org.apache.sling.feature.r2f.RuntimeEnvironment2FeatureModel;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public final class RuntimeEnvironment2FeatureModelActivator implements BundleActivator {

    private static final String SERVICE_TITLE = "Apache Sling Runtime Environment to Feature Model converter";

    private static final String SERVICE_NAME = "r2f";

    private static final String RUNTIME_GENERATOR = " - Runtime Generator";

    private static final String BASE_2_RUNTIME_DIFF_GENERATOR = " - Base 2 Runtime diff Generator";

    private final List<ServiceRegistration<?>> registrations = new LinkedList<>();

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        RuntimeEnvironment2FeatureModel generator = new RuntimeEnvironment2FeatureModelService();
        registerService(bundleContext, RuntimeEnvironment2FeatureModel.class, generator, null);

        InventoryPrinter runtimePrinter = new RuntimeEnvironment2FeatureModelPrinter(generator, bundleContext);
        registerService(bundleContext, InventoryPrinter.class, runtimePrinter, RUNTIME_GENERATOR);

        InventoryPrinter base2RuntimePrinter = new BaseFeature2CurrentRuntimePrinter(generator, bundleContext);
        registerService(bundleContext, InventoryPrinter.class, base2RuntimePrinter, BASE_2_RUNTIME_DIFF_GENERATOR);
    }

    private <S> void registerService(BundleContext bundleContext, Class<S> type, S service, String classifier) {
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(SERVICE_VENDOR, bundleContext.getBundle().getHeaders(BUNDLE_VENDOR));
        putProperty(SERVICE_DESCRIPTION, SERVICE_TITLE, classifier, properties);
        putProperty(SERVICE_DESCRIPTION, SERVICE_TITLE, classifier, properties);

        if (InventoryPrinter.class.isAssignableFrom(type)) {
            putProperty(NAME, SERVICE_NAME, classifier, properties);
            putProperty(TITLE, SERVICE_TITLE, classifier, properties);
            putProperty(FORMAT, JSON.toString(), classifier, properties);
        }

        registrations.add(bundleContext.registerService(type, service, properties));
    }

    private static void putProperty(String key, String value, String classifier, Dictionary<String, Object> properties) {
        String finalValue;

        if (classifier != null && !classifier.isEmpty()) {
            finalValue = value.concat(classifier);
        } else {
            finalValue = value;
        }

        properties.put(key, finalValue);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        for (ServiceRegistration<?> registration : registrations) {
            registration.unregister();
        }

        registrations.clear();
    }

}
