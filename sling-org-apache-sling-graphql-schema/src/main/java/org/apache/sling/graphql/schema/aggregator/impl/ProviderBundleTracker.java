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

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Tracks bundles which provide partial schemas, and registers
 *  a {@PartialSchemaProvider} service for each partial schema
 */
public class ProviderBundleTracker extends BundleTracker<Object> {

    public static final String SCHEMA_PATH_HEADER = "Sling-GraphQL-Schema";

    private final Logger log = LoggerFactory.getLogger(getClass().getName());
    private final Map<String, BundleEntryPartialProvider> schemaProviders;
    
    public ProviderBundleTracker(BundleContext context) {
        super(context, Bundle.ACTIVE, null);
        schemaProviders = new ConcurrentHashMap<>();
    }

    @Override
    public Object addingBundle(Bundle bundle, BundleEvent event) {
        final String providersPath = bundle.getHeaders().get(SCHEMA_PATH_HEADER);
        if(providersPath == null) {
            log.debug("Bundle {} has no {} header, ignored", bundle.getSymbolicName(), SCHEMA_PATH_HEADER);
        } else {
            // For now we only support file entries which are directly under providersPath
            final Enumeration<String> paths = bundle.getEntryPaths(providersPath);
            if(paths != null) {
                while(paths.hasMoreElements()) {
                    addIfNotPresent(BundleEntryPartialProvider.forBundle(bundle, paths.nextElement()));
                }
            }
        }
        return super.addingBundle(bundle, event);
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
        schemaProviders.entrySet().forEach(entry -> {
            if(id == entry.getValue().getBundleId()) {
                log.info("Removing {}", entry.getValue());
                schemaProviders.remove(entry.getKey());
            }
        });
    }

    Map<String, PartialSchemaProvider> getSchemaProviders() {
        return Collections.unmodifiableMap(schemaProviders);
    }
}
