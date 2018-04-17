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

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.GenericServlet;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
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

import aQute.bnd.annotation.headers.ProvideCapability;

@Component(
        service = {}
)
@ProvideCapability(ns = "osgi.extender", name = BundledScriptTracker.NS_SLING_SCRIPTING_EXTENDER, version = "1.0.0")
public class BundledScriptTracker implements BundleTrackerCustomizer<List<ServiceRegistration<Servlet>>> {
    static final String NS_SLING_SCRIPTING_EXTENDER = "sling.scripting";
    static final String NS_SLING_RESOURCE_TYPE = "sling.resourceType";
    private static final Logger LOGGER = LoggerFactory.getLogger(BundledScriptTracker.class);
    private static final String AT_SLING_SELECTORS = "sling.resourceType.selectors";
    private static final String AT_SLING_EXTENSIONS = "sling.resourceType.extensions";

    static final String AT_VERSION = "version";
    private static final String AT_EXTENDS = "extends";

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
                .anyMatch(m_context.getBundle()::equals)) {
            LOGGER.debug("Inspecting bundle {} for {} capability.", bundle.getSymbolicName(), NS_SLING_RESOURCE_TYPE);
            List<BundleCapability> capabilities = bundleWiring.getCapabilities(NS_SLING_RESOURCE_TYPE);

            if (!capabilities.isEmpty()) {

                return capabilities.stream().flatMap(cap ->
                {
                    Hashtable<String, Object> properties = new Hashtable<>();

                    Map<String, Object> attributes = cap.getAttributes();
                    String resourceType = (String) attributes.get(NS_SLING_RESOURCE_TYPE);

                    Version version = (Version) attributes.get(AT_VERSION);

                    if (version != null) {
                        resourceType += "/" + version;
                    }

                    properties.put(ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES, resourceType);

                    Object selectors = attributes.get(AT_SLING_SELECTORS);
                    Set<String> extensions = new HashSet<>(
                            Arrays.asList(PropertiesUtil.toStringArray(attributes.get(AT_SLING_EXTENSIONS), new String[0]))
                    );
                    extensions.add("html");
                    properties.put(ServletResolverConstants.SLING_SERVLET_EXTENSIONS, extensions);

                    if (selectors != null) {
                        properties.put(ServletResolverConstants.SLING_SERVLET_SELECTORS, selectors);
                    }

                    Set<String> methods = new HashSet<>(Arrays.asList(PropertiesUtil.toStringArray(attributes.get(ServletResolverConstants.SLING_SERVLET_METHODS), new String[0])));
                    if (!methods.isEmpty())
                    {
                        properties.put(ServletResolverConstants.SLING_SERVLET_METHODS, String.join(",",methods));
                    }

                    String extendsRT = (String) attributes.get(AT_EXTENDS);
                    Optional<BundleWire> optionalWire = Optional.empty();

                    if (StringUtils.isNotEmpty(extendsRT)) {

                        LOGGER.debug("Bundle {} extends resource type {} through {}.", bundle.getSymbolicName(), extendsRT, resourceType);
                        optionalWire = bundleWiring.getRequiredWires(NS_SLING_RESOURCE_TYPE).stream().filter(
                                bundleWire -> extendsRT.equals(bundleWire.getCapability().getAttributes().get(NS_SLING_RESOURCE_TYPE)) &&
                                    !bundleWire.getCapability().getAttributes().containsKey(AT_SLING_SELECTORS)
                        ).findFirst();
                    }

                    List<ServiceRegistration<Servlet>> regs = new ArrayList<>();

                    if (optionalWire.isPresent()) {
                        BundleWire extendsWire = optionalWire.get();
                        Map<String, Object> wireCapabilityAttributes = extendsWire.getCapability().getAttributes();
                        String wireResourceType = (String) wireCapabilityAttributes.get(NS_SLING_RESOURCE_TYPE);
                        Version wireResourceTypeVersion = (Version) wireCapabilityAttributes.get(AT_VERSION);
                        regs.add(bundle.getBundleContext().registerService(
                            Servlet.class,
                            new BundledScriptServlet(bundledScriptFinder, optionalWire.get().getProvider().getBundle(),
                                scriptContextProvider, wireResourceType + (wireResourceTypeVersion != null ? "/" +
                                wireResourceTypeVersion.toString() : ""), getWiredResourceTypes(
                                    new HashSet<>(Arrays.asList((String) attributes.get(NS_SLING_RESOURCE_TYPE), wireResourceType)),
                                    new HashSet<>(Arrays.asList(resourceType, wireResourceType + (wireResourceTypeVersion != null ? "/" +
                                        wireResourceTypeVersion.toString() : ""))),
                                    bundle,optionalWire.get().getProvider().getBundle())),
                            properties
                        ));
                    }
                    else
                    {
                        regs.add(bundle.getBundleContext()
                            .registerService(Servlet.class, new BundledScriptServlet(bundledScriptFinder, bundle, scriptContextProvider,
                                    getWiredResourceTypes(new HashSet<>(Arrays.asList((String) attributes.get(NS_SLING_RESOURCE_TYPE))),
                                        new HashSet<>(Arrays.asList(resourceType)),bundle)),
                                properties));
                    }
                    if (version != null)
                    {
                        properties.put(ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES, attributes.get(NS_SLING_RESOURCE_TYPE));
                        regs.add(m_context.registerService(Servlet.class, new DispatcherServlet((String) attributes.get(NS_SLING_RESOURCE_TYPE)), properties));
                    }
                    return regs.stream();
                }).collect(Collectors.toList());
            } else {
                return null;
            }
        } else {
            return null;
        }
    }



    private Set<String> getWiredResourceTypes(Set<String> rts, Set<String> initial, Bundle... bundles) {
        Set<String> wiredResourceTypes = new HashSet<>();
        wiredResourceTypes.addAll(initial);
        for (Bundle bundle : bundles)
        {
            BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
            bundleWiring.getCapabilities(BundledScriptTracker.NS_SLING_RESOURCE_TYPE).forEach(
                cap ->
                {
                    String resourceType = (String) cap.getAttributes().get(BundledScriptTracker
                        .NS_SLING_RESOURCE_TYPE);
                    Version version = (Version) cap.getAttributes().get(BundledScriptTracker
                        .AT_VERSION);
                    if (!rts.contains(resourceType))
                    {
                        wiredResourceTypes.add(resourceType + (version == null ? "" : "/" + version.toString()));
                    }
                }
            );
            bundleWiring.getRequiredWires(BundledScriptTracker.NS_SLING_RESOURCE_TYPE).forEach(
                bundleWire ->
                {
                    String resourceType = (String) bundleWire.getCapability().getAttributes().get(BundledScriptTracker
                        .NS_SLING_RESOURCE_TYPE);
                    Version version = (Version) bundleWire.getCapability().getAttributes().get(BundledScriptTracker
                        .AT_VERSION);
                    if (!rts.contains(resourceType))
                    {
                        wiredResourceTypes.add(resourceType + (version == null ? "" : "/" + version.toString()));
                    }
                }
            );
        }
        return wiredResourceTypes;
    }

    private Hashtable<String, Object> toProperties(ServiceRegistration<?> reg)
    {
        Hashtable<String, Object> result = new Hashtable<>();
        ServiceReference<?> ref = reg.getReference();

        set(ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES, ref, result);
        set(ServletResolverConstants.SLING_SERVLET_EXTENSIONS, ref, result);
        set(ServletResolverConstants.SLING_SERVLET_SELECTORS, ref, result);
        set(ServletResolverConstants.SLING_SERVLET_METHODS, ref, result);

        return result;
    }

    private String getResourceType(Hashtable<String, Object> props)
    {
        String[] values = PropertiesUtil.toStringArray(props.get(ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES));
        int idx = values[0].indexOf("/");
        if (idx != -1)
        {
            return values[0].substring(0, idx);
        }
        else
        {
            return values[0];
        }
    }

    private String getResourceTypeVersion(ServiceReference<?>ref )
    {
        String[] values = PropertiesUtil.toStringArray(ref.getProperty(ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES));
        int idx = values[0].indexOf("/");
        if (idx != -1)
        {
            return values[0].substring(idx + 1);
        }
        else
        {
            return null;
        }
    }

    private void set(String key, ServiceReference<?> ref, Hashtable<String, Object> props)
    {
        Object value = ref.getProperty(key);
        if (value != null)
        {
            props.put(key, value);
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

    private class DispatcherServlet extends GenericServlet
    {
        private final String m_rt;

        DispatcherServlet(String rt)
        {
            m_rt = rt;
        }

        @Override
        public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException
        {
            SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) req;

            Optional<ServiceRegistration<Servlet>> target = m_tracker.getTracked().values().stream().flatMap(List::stream)
                .filter(
                    ((Predicate<ServiceRegistration<Servlet>>) reg -> reg.getReference().getBundle().equals(m_context.getBundle())).negate()
                )
                .filter(reg -> getResourceTypeVersion(reg.getReference()) != null)
                .filter(reg ->
                {
                    Hashtable<String, Object> props = toProperties(reg);
                    if (getResourceType(props).equals(m_rt))
                    {
                        if (Arrays.asList(PropertiesUtil.toStringArray(props.get(ServletResolverConstants.SLING_SERVLET_SELECTORS), new String[0]))
                            .containsAll(Arrays.asList(slingRequest.getRequestPathInfo().getSelectors()))
                            &&
                            Arrays.asList(PropertiesUtil.toStringArray(props.get(ServletResolverConstants.SLING_SERVLET_METHODS), new String[]{"GET", "HEAD"}))
                                .contains(slingRequest.getMethod())
                            &&
                            Arrays.asList(PropertiesUtil.toStringArray(props.get(ServletResolverConstants.SLING_SERVLET_EXTENSIONS), new String[]{"html"}))
                                .contains(slingRequest.getRequestPathInfo().getExtension() == null ? "html" : slingRequest.getRequestPathInfo().getExtension() ))
                        {
                            return true;
                        }
                    }
                    return false;
                })
                .sorted(Comparator.comparing(reg -> new Version(getResourceTypeVersion(reg.getReference())), Comparator.reverseOrder()))
                .findFirst();

            if (target.isPresent())
            {
                String rt = (String) target.get().getReference().getProperty(ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES);
                RequestDispatcherOptions options = new RequestDispatcherOptions();
                options.setForceResourceType(rt);

                System.out.println("found: " + rt);
                RequestDispatcher dispatcher = slingRequest.getRequestDispatcher(slingRequest.getResource(), options);
                if (dispatcher != null)
                {
                    dispatcher.forward(req, res);
                }
                else
                {
                    ((SlingHttpServletResponse) res).sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            }
            else
            {
                System.out.println("Not found");
                ((SlingHttpServletResponse) res).sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }
}
