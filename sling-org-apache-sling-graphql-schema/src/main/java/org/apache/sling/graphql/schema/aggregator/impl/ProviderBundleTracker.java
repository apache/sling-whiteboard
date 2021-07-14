/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Licensed to the Apache Software Foundation (ASF) under one
 ~ or more contributor license agreements.  See the NOTICE file
 ~ distributed with this work for additional information
 ~ regarding copyright ownership.  The ASF licenses this file
 ~ to you under the Apache License, Version 2.0 (the
 ~ "License"); you may not use this file except in compliance
 ~ with the License.  You may obtain a copy of the License at
 ~
 ~   http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

package org.apache.sling.graphql.schema.aggregator.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.annotation.bundle.Capability;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Tracks bundles which provide partial schemas and collects the corresponding set of schemas.
 */
@Component(
        service = {ProviderBundleTracker.class}
)
@Capability(
        namespace = ExtenderNamespace.EXTENDER_NAMESPACE,
        name = "sling.graphql-schema-aggregator",
        version = "0.1"
)
public class ProviderBundleTracker implements BundleTrackerCustomizer<Object> {

    public static final String SCHEMA_PATH_HEADER = "Sling-GraphQL-Schema";

    private final Logger log = LoggerFactory.getLogger(getClass().getName());
    private final Map<String, BundleEntryPartialProvider> schemaProviders = new ConcurrentHashMap<>();

    private BundleContext bundleContext;

    @Activate
    public void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        BundleTracker<?> bt = new BundleTracker<>(bundleContext, Bundle.ACTIVE, this);
        bt.open();
    }

    @Override
    public Object addingBundle(Bundle bundle, BundleEvent event) {
        BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
        Bundle us = bundleContext.getBundle();
        if (bundleWiring.getRequiredWires(ExtenderNamespace.EXTENDER_NAMESPACE).stream().map(BundleWire::getProvider)
                .map(BundleRevision::getBundle).anyMatch(us::equals)) {
            final String providersPath = bundle.getHeaders().get(SCHEMA_PATH_HEADER);
            if (providersPath == null) {
                log.debug("Bundle {} has no {} header, ignored", bundle.getSymbolicName(), SCHEMA_PATH_HEADER);
            } else {
                // For now we only support file entries which are directly under providersPath
                final Enumeration<String> paths = bundle.getEntryPaths(providersPath);
                if (paths != null) {
                    while (paths.hasMoreElements()) {
                        final String path = paths.nextElement();
                        try {
                            addIfNotPresent(BundleEntryPartialProvider.forBundle(bundle, path));
                        } catch (IOException ioe) {
                            // TODO save errors and refuse to work if any happended?
                            log.error("Error reading partial " + path, ioe);
                        }
                    }
                }
            }
        }
        return bundle;
    }

    private void addIfNotPresent(BundleEntryPartialProvider a) {
        if(a != null) {
            if(schemaProviders.containsKey(a.getName())) {
                log.warn("Partial provider with name {} already present, new one will be ignored", a.getName());
            } else {
                log.info("Registering {}", a);
                schemaProviders.put(a.getName(), a);
            }
        }
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
        final long id = bundle.getBundleId();
        schemaProviders.forEach((key, value) -> {
            if (id == value.getBundleId()) {
                log.info("Removing {}", value);
                schemaProviders.remove(key);
            }
        });
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {
        // do nothing
    }

    Map<String, Partial> getSchemaProviders() {
        return Collections.unmodifiableMap(schemaProviders);
    }
}
