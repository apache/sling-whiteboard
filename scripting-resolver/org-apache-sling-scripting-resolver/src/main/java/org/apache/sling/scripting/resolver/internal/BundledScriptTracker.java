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

import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component(
        service = {}
)
public class BundledScriptTracker implements BundleTrackerCustomizer<ServiceRegistration<Servlet>>
{

    private static final Logger LOGGER = LoggerFactory.getLogger(BundledScriptTracker.class);

    public static final String NS_JAVAX_SCRIPT_CAPABILITY = "javax.script";
    public static final String AT_SLING_RESOURCE_TYPE = "sling.resourceType";
    public static final String AT_SLING_RESOURCE_TYPE_VERSION = "sling.resourceType.version";

    private volatile BundleContext m_context;
    private volatile BundleTracker<ServiceRegistration<Servlet>> m_tracker;

    @Activate
    private void activate(BundleContext context) {
        m_context = context;
        m_tracker = new BundleTracker<>(context, Bundle.ACTIVE, this);
        m_tracker.open();
    }

    @Deactivate
    private void deactivate() {
        m_tracker.close();
    }

    @Override
    public ServiceRegistration<Servlet> addingBundle(Bundle bundle, BundleEvent event) {

        BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
        List<BundleCapability> capabilities = bundleWiring.getCapabilities(NS_JAVAX_SCRIPT_CAPABILITY);
        LOGGER.debug("Inspecting bundle {} for {} capability.", bundle.getSymbolicName(), NS_JAVAX_SCRIPT_CAPABILITY);
        String[] resourceTypes = capabilities.stream().map(cap -> {
            Map<String, Object> attributes = cap.getAttributes();
            String resourceType = (String) attributes.get(AT_SLING_RESOURCE_TYPE);
            Version version = (Version) attributes.get(AT_SLING_RESOURCE_TYPE_VERSION);
            if (StringUtils.isNotEmpty(resourceType) && version != null) {
                return resourceType + "/" + version;
            } else if (StringUtils.isNotEmpty(resourceType)) {
                return resourceType;
            }
            return null;
        }).filter(Objects::nonNull).toArray(String[]::new);
        if (resourceTypes.length > 0) {
            Hashtable<String, Object> properties = new Hashtable<>();
            properties.put("sling.servlet.resourceTypes", resourceTypes);
            properties.put("sling.servlet.methods", new String[]{"TRACE", "OPTIONS", "GET", "HEAD", "POST", "PUT", "DELETE"});
            LOGGER.debug("Registering bundle {} for {} resourceTypes.", bundle.getSymbolicName(), Arrays.asList(resourceTypes));
            return m_context.registerService(Servlet.class, new BundledScriptServlet(bundle), properties);
        } else {
            return null;
        }
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, ServiceRegistration<Servlet> reg) {
        LOGGER.warn(String.format("Unexpected modified event: %s for bundle %s", event.toString(), bundle.toString()));
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, ServiceRegistration<Servlet> reg) {
        LOGGER.debug("Bundle {} removed", bundle.getSymbolicName());
        reg.unregister();
    }
}
