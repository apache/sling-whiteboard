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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.Servlet;

import aQute.bnd.annotation.headers.ProvideCapability;
import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        service = {}
)
@ProvideCapability(ns = "osgi.extender", name = BundledScriptTracker.NS_SLING_SCRIPTING_EXTENDER, version = "1.0.0")
public class BundledScriptTracker implements BundleTrackerCustomizer<List<ServiceRegistration<Servlet>>> {
    static final String NS_SLING_SCRIPTING_EXTENDER = "sling.scripting";
    private static final String NS_SLING_RESOURCE_TYPE = "sling.resourceType";
    private static final Logger LOGGER = LoggerFactory.getLogger(BundledScriptTracker.class);
    private static final String AT_SLING_RESOURCE_TYPE_ONLY = "sling.resourceType.standalone";
    private static final String AT_SLING_SELECTORS = "sling.resourceType.selectors";
    private static final String AT_SLING_EXTENSIONS = "sling.resourceType.extensions";

    @Reference
    private BundledScriptFinder bundledScriptFinder;

    @Reference
    private ScriptContextProvider scriptContextProvider;


    private volatile BundleContext m_context;
    private volatile BundleTracker<List<ServiceRegistration<Servlet>>> m_tracker;

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
    public List<ServiceRegistration<Servlet>> addingBundle(Bundle bundle, BundleEvent event) {
        BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
        if (bundleWiring.getRequiredWires("osgi.extender").stream().map(BundleWire::getProvider).map(BundleRevision::getBundle)
            .anyMatch(m_context.getBundle()::equals))
        {
            LOGGER.debug("Inspecting bundle {} for {} capability.", bundle.getSymbolicName(), NS_SLING_RESOURCE_TYPE);
            List<BundleCapability> capabilities = bundleWiring.getCapabilities(NS_SLING_RESOURCE_TYPE);

            if (!capabilities.isEmpty())
            {
                BundledScriptServlet servlet = new BundledScriptServlet(bundledScriptFinder, bundle, scriptContextProvider);
                Hashtable<String, Object> baseProperties = new Hashtable<>();
                baseProperties.put("sling.servlet.methods", new String[]{"TRACE", "OPTIONS", "GET", "HEAD", "POST", "PUT", "DELETE"});

                return capabilities.stream().flatMap(cap ->
                {
                    Hashtable<String, Object> properties = new Hashtable<>(baseProperties);

                    List<ServiceRegistration<Servlet>> result = new ArrayList<>();

                    Map<String, Object> attributes = cap.getAttributes();

                    String resourceType = (String) attributes.get(NS_SLING_RESOURCE_TYPE);

                    Version version = (Version) attributes.get("version");

                    if (version != null)
                    {
                        resourceType += "/" + version;
                    }

                    properties.put("sling.servlet.resourceTypes", resourceType);

                    Object selectors = attributes.get(AT_SLING_SELECTORS);
                    Object extensions = attributes.get(AT_SLING_EXTENSIONS);


                    if ("true".equalsIgnoreCase(attributes.containsKey(AT_SLING_RESOURCE_TYPE_ONLY) ? attributes.get(AT_SLING_RESOURCE_TYPE_ONLY).toString() : null)
                        || (selectors == null && extensions == null))
                    {
                        result.add(m_context.registerService(Servlet.class, servlet, properties));
                    }


                    if (selectors != null)
                    {
                        properties.put("sling.servlet.selectors", selectors);
                    }

                    if (extensions != null)
                    {
                        properties.put("sling.servlet.extensions", extensions);
                    }

                    if (selectors != null || extensions != null)
                    {
                        m_context.registerService(Servlet.class, servlet, properties);
                    }
                    return result.stream();
                }).collect(Collectors.toList());
            }
            else
            {
                return null;
            }
        }
        else
        {
            return null;
        }
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, List<ServiceRegistration<Servlet>> regs) {
        LOGGER.warn(String.format("Unexpected modified event: %s for bundle %s", event.toString(), bundle.toString()));
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, List<ServiceRegistration<Servlet>> regs) {
        LOGGER.debug("Bundle {} removed", bundle.getSymbolicName());
        regs.forEach(ServiceRegistration::unregister);
    }
}
