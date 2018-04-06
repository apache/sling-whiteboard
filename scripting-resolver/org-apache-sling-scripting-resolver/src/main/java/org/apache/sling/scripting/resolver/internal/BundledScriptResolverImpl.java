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
package org.apache.sling.scripting.resolver.internal;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        service = {}
)
public class BundledScriptResolverImpl implements BundleListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(BundledScriptResolverImpl.class);

    public static final String NS_JAVAX_SCRIPT_CAPABILITY = "javax.script";
    public static final String AT_SLING_RESOURCE_TYPE = "sling.resourceType";
    public static final String AT_SLING_RESOURCE_TYPE_VERSION = "sling.resourceType.version";

    private Map<String, Map<Version, Bundle>> bundleRTMappings = new ConcurrentHashMap<>();

    @Activate
    private void activate(ComponentContext componentContext) {
        BundleContext bundleContext = componentContext.getBundleContext();
        for (Bundle bundle : bundleContext.getBundles()) {
            if (bundle.getState() == Bundle.ACTIVE) {
                addMappings(bundle);
            }
        }
        bundleContext.addBundleListener(this);
    }

    @Deactivate
    private void deactivate(ComponentContext componentContext) {
        componentContext.getBundleContext().removeBundleListener(this);
    }
    
    @Override
    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.STARTED) {
            addMappings(event.getBundle());
        } else if (event.getType() == BundleEvent.STOPPED) {
            clearMappings(event.getBundle());
        }
    }

    private void addMappings(Bundle bundle) {
        BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
        List<BundleCapability> capabilities = bundleWiring.getCapabilities(NS_JAVAX_SCRIPT_CAPABILITY);
        LOGGER.debug("Inspecting bundle {} for {} capability.", bundle.getSymbolicName(), NS_JAVAX_SCRIPT_CAPABILITY);
        for (BundleCapability capability : capabilities) {
            Map<String, Object> attributes = capability.getAttributes();
            if (attributes.containsKey(AT_SLING_RESOURCE_TYPE) && attributes.containsKey(AT_SLING_RESOURCE_TYPE_VERSION)) {
                String resourceType = (String) attributes.get(AT_SLING_RESOURCE_TYPE);
                Map<Version, Bundle> rtVersionMappings = bundleRTMappings.get(resourceType);
                if (rtVersionMappings == null) {
                    rtVersionMappings = new ConcurrentSkipListMap<>();
                    bundleRTMappings.put(resourceType, rtVersionMappings);
                }
                Version version = (Version) attributes.get(AT_SLING_RESOURCE_TYPE_VERSION);
                Bundle existingBundle = rtVersionMappings.putIfAbsent(version, bundle);
                if (existingBundle == null) {
                    LOGGER.debug("Resource type mapping {}:{} is provided by bundle {}.", resourceType, version, bundle
                            .getSymbolicName());
                } else {
                    LOGGER.debug("Bundle {} attempted to override resource type mapping {}:{} already provided by bundle {}.", bundle,
                            resourceType, version, existingBundle);
                }
            }
        }
    }

    private void clearMappings(Bundle bundle) {
        BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
        List<BundleCapability> capabilities = bundleWiring.getCapabilities(NS_JAVAX_SCRIPT_CAPABILITY);
        LOGGER.debug("Inspecting bundle {} for {} capability.", bundle.getSymbolicName(), NS_JAVAX_SCRIPT_CAPABILITY);
        for (BundleCapability capability : capabilities) {
            Map<String, Object> attributes = capability.getAttributes();
            if (attributes.containsKey(AT_SLING_RESOURCE_TYPE) && attributes.containsKey(AT_SLING_RESOURCE_TYPE_VERSION)) {
                String resourceType = (String) attributes.get(AT_SLING_RESOURCE_TYPE);
                Version version = (Version) attributes.get(AT_SLING_RESOURCE_TYPE_VERSION);
                Map<Version, Bundle> rtVersionMappings = bundleRTMappings.get(resourceType);
                if (rtVersionMappings != null) {
                    Bundle mappedBundle = rtVersionMappings.get(version);
                    if (mappedBundle != null && mappedBundle.equals(bundle)) {
                        rtVersionMappings.remove(version);
                        LOGGER.debug("Removed resource type mapping {}:{} for bundle {}.", resourceType, version, bundle.getSymbolicName());
                        if (rtVersionMappings.isEmpty()) {
                            bundleRTMappings.remove(resourceType);
                            LOGGER.debug("Removed all mappings for resource type {}.", resourceType);
                        }
                    }
                }
            }
        }
    }

}
