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

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.inventory.InventoryPrinter;
import org.apache.sling.feature.r2f.RuntimeEnvironment2FeatureModel;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

public final class RuntimeEnvironment2FeatureModelActivator implements BundleActivator {

    private static final String SERVICE_TITLE = "Apache Sling Runtime Environment to Feature Model converter";

    private static final String SERVICE_NAME = "r2f";

    private ServiceRegistration<RuntimeEnvironment2FeatureModel> converterRegistration;

    private ServiceRegistration<InventoryPrinter> printerRegistration;

    @Override
    public void start(BundleContext context) throws Exception {
        final Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(Constants.SERVICE_VENDOR, context.getBundle().getHeaders(Constants.BUNDLE_VENDOR));
        properties.put(Constants.SERVICE_DESCRIPTION, SERVICE_TITLE);

        RuntimeEnvironment2FeatureModel converter = new RuntimeEnvironment2FeatureModelService();

        properties.put(InventoryPrinter.NAME, SERVICE_NAME);
        properties.put(InventoryPrinter.TITLE, SERVICE_TITLE);
        InventoryPrinter printer = new RuntimeEnvironment2FeatureModelPrinter(converter, context);

        converterRegistration = context.registerService(RuntimeEnvironment2FeatureModel.class, converter, properties);
        printerRegistration = context.registerService(InventoryPrinter.class, printer, properties);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        converterRegistration.unregister();
        printerRegistration.unregister();
    }

}
