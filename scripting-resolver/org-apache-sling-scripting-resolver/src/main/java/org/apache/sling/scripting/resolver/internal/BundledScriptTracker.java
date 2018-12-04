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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
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
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
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
@ProvideCapability(ns = "osgi.extender",
                   name = BundledScriptTracker.NS_SLING_SCRIPTING_EXTENDER,
                   version = "1.0.0")
public class BundledScriptTracker implements BundleTrackerCustomizer<List<ServiceRegistration<Servlet>>> {
    static final String NS_SLING_SCRIPTING_EXTENDER = "sling.scripting";

    static final String NS_SLING_RESOURCE_TYPE = "sling.resourceType";
    private static final Logger LOGGER = LoggerFactory.getLogger(BundledScriptTracker.class);
    private static final String AT_SLING_SELECTORS = "sling.resourceType.selectors";
    private static final String AT_SLING_EXTENSIONS = "sling.resourceType.extensions";
    private static final String REGISTERING_BUNDLE = "org.apache.sling.scripting.resolver.internal.BundledScriptTracker.registering_bundle";
    static final String AT_VERSION = "version";
    private static final String AT_EXTENDS = "extends";

    @Reference
    private BundledScriptFinder bundledScriptFinder;

    @Reference
    private ScriptContextProvider scriptContextProvider;


    private volatile BundleContext m_context;
    private volatile BundleTracker<List<ServiceRegistration<Servlet>>> m_tracker;
    private volatile Map<String, ServiceRegistration<Servlet>> m_dispatchers = new HashMap<>();

    @Activate
    protected void activate(BundleContext context) {
        m_context = context;
        m_tracker = new BundleTracker<>(context, Bundle.ACTIVE, this);
        m_tracker.open();
    }

    @Deactivate
    protected void deactivate() {
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
                boolean precompiled = Boolean.parseBoolean(bundle.getHeaders().get("Sling-ResourceType-Precompiled"));
                List<ServiceRegistration<Servlet>> serviceRegistrations = capabilities.stream().flatMap(cap ->
                {
                    Hashtable<String, Object> properties = new Hashtable<>();
                    properties.put(ServletResolverConstants.SLING_SERVLET_NAME, BundledScriptServlet.class.getName());
                    properties.put(Constants.SERVICE_DESCRIPTION, cap.toString());

                    Map<String, Object> attributes = cap.getAttributes();
                    String resourceType = (String) attributes.get(NS_SLING_RESOURCE_TYPE);
                    String resourceTypeString = resourceType;

                    Version version = (Version) attributes.get(AT_VERSION);

                    if (version != null) {
                        resourceTypeString += "/" + version;
                    }

                    properties.put(ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES, resourceTypeString);

                    Object selectors = attributes.get(AT_SLING_SELECTORS);
                    Set<String> extensions = new HashSet<>(
                            Arrays.asList(PropertiesUtil.toStringArray(attributes.get(AT_SLING_EXTENSIONS), new String[0]))
                    );
                    extensions.add("html");
                    properties.put(ServletResolverConstants.SLING_SERVLET_EXTENSIONS, extensions);

                    if (selectors != null) {
                        properties.put(ServletResolverConstants.SLING_SERVLET_SELECTORS, selectors);
                    }

                    Set<String> methods = new HashSet<>(Arrays.asList(
                            PropertiesUtil.toStringArray(attributes.get(ServletResolverConstants.SLING_SERVLET_METHODS), new String[0])));
                    if (!methods.isEmpty()) {
                        properties.put(ServletResolverConstants.SLING_SERVLET_METHODS, String.join(",", methods));
                    }

                    String extendsRT = (String) attributes.get(AT_EXTENDS);
                    Optional<BundleWire> optionalWire = Optional.empty();

                    if (StringUtils.isNotEmpty(extendsRT)) {

                        LOGGER.debug("Bundle {} extends resource type {} through {}.", bundle.getSymbolicName(), extendsRT,
                                resourceTypeString);
                        optionalWire = bundleWiring.getRequiredWires(NS_SLING_RESOURCE_TYPE).stream().filter(
                                bundleWire -> extendsRT.equals(bundleWire.getCapability().getAttributes().get(NS_SLING_RESOURCE_TYPE)) &&
                                        !bundleWire.getCapability().getAttributes().containsKey(AT_SLING_SELECTORS)
                        ).findFirst();
                    }

                    List<ServiceRegistration<Servlet>> regs = new ArrayList<>();
                    LinkedHashSet<TypeProvider> wiredProviders = new LinkedHashSet<>();
                    wiredProviders.add(new TypeProvider(resourceType, bundle));
                    wiredProviders.add(new TypeProvider(resourceTypeString, bundle));
                    if (optionalWire.isPresent()) {
                        BundleWire extendsWire = optionalWire.get();
                        Bundle providerBundle = extendsWire.getProvider().getBundle();
                        Map<String, Object> wireCapabilityAttributes = extendsWire.getCapability().getAttributes();
                        String wireResourceType = (String) wireCapabilityAttributes.get(NS_SLING_RESOURCE_TYPE);
                        Version wireResourceTypeVersion = (Version) wireCapabilityAttributes.get(AT_VERSION);
                        String wireResourceTypeString = wireResourceType + (wireResourceTypeVersion != null ? "/" +
                                wireResourceTypeVersion.toString() : "");

                        wiredProviders.add(new TypeProvider(wireResourceType, providerBundle));
                        wiredProviders.add(new TypeProvider(wireResourceTypeString, providerBundle));

                    }
                    populateWiredProviders(wiredProviders);
                    regs.add(
                        bundle.getBundleContext().registerService(
                                Servlet.class,
                                new BundledScriptServlet(bundledScriptFinder, scriptContextProvider, wiredProviders, precompiled),
                                properties
                        )
                    );
                    return regs.stream();
                }).collect(Collectors.toList());
                refreshDispatcher(serviceRegistrations);
                return serviceRegistrations;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }


    private void populateWiredProviders(LinkedHashSet<TypeProvider> initialProviders) {
        Set<String> initialResourceTypes = new HashSet<>(initialProviders.size());
        Set<Bundle> bundles = new HashSet<>(initialProviders.size());
        for (TypeProvider provider : initialProviders) {
            initialResourceTypes.add(provider.getType());
            bundles.add(provider.getBundle());
        }
        for (Bundle bundle : bundles) {
            BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
            bundleWiring.getRequiredWires(BundledScriptTracker.NS_SLING_RESOURCE_TYPE).forEach(
                    bundleWire ->
                    {
                        String resourceType = (String) bundleWire.getCapability().getAttributes().get(BundledScriptTracker
                                .NS_SLING_RESOURCE_TYPE);
                        Version version = (Version) bundleWire.getCapability().getAttributes().get(BundledScriptTracker
                                .AT_VERSION);
                        if (!initialResourceTypes.contains(resourceType)) {
                            initialProviders.add(new TypeProvider(resourceType + (version == null ? "" : "/" + version.toString()),
                                    bundleWire.getProvider().getBundle()));
                        }
                    }
            );
        }
    }

    private void refreshDispatcher(List<ServiceRegistration<Servlet>> regs) {
        Map<String, ServiceRegistration<Servlet>> dispatchers = new HashMap<>();
        Stream.concat(m_tracker.getTracked().values().stream(), Stream.of(regs)).flatMap(List::stream).map(this::toProperties).collect(
                Collectors.groupingBy(this::getResourceType)).forEach((rt, propList) -> {
            Hashtable<String, Object> properties = new Hashtable<>();
            properties.put(ServletResolverConstants.SLING_SERVLET_NAME, DispatcherServlet.class.getName());
            properties.put(ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES, rt);
            Set<String> methods = propList.stream()
                    .map(props -> props.getOrDefault(ServletResolverConstants.SLING_SERVLET_METHODS, new String[]{"GET", "HEAD"}))
                    .map(PropertiesUtil::toStringArray).map(Arrays::asList).flatMap(List::stream).collect(Collectors.toSet());
            Set<String> extensions = propList.stream().map(props -> props.getOrDefault(ServletResolverConstants
                    .SLING_SERVLET_EXTENSIONS, new String[]{"html"})).map(PropertiesUtil::toStringArray).map(Arrays::asList).flatMap
                    (List::stream).collect(Collectors.toSet());
            properties.put(ServletResolverConstants.SLING_SERVLET_EXTENSIONS, extensions.toArray(new String[0]));
            if (!methods.equals(new HashSet<>(Arrays.asList("GET", "HEAD")))) {
                properties.put(ServletResolverConstants.SLING_SERVLET_METHODS, methods.toArray(new String[0]));
            }
            ServiceRegistration<Servlet> reg = m_dispatchers.remove(rt);
            if (reg == null) {
                Optional<BundleContext> registeringBundle = propList.stream().map(props -> {
                    Bundle bundle = (Bundle) props.get(REGISTERING_BUNDLE);
                    if (bundle != null) {
                        return bundle.getBundleContext();
                    }
                    return null;
                }).findFirst();
                properties.put(Constants.SERVICE_DESCRIPTION, ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + rt + "; " +
                        ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=" + extensions + "; " +
                        ServletResolverConstants.SLING_SERVLET_METHODS + "=" + methods);
                reg = registeringBundle.orElse(m_context).registerService(Servlet.class, new DispatcherServlet(rt), properties);
            } else {
                if (!new HashSet<>(Arrays.asList(PropertiesUtil
                        .toStringArray(reg.getReference().getProperty(ServletResolverConstants.SLING_SERVLET_METHODS), new String[0])))
                        .equals(methods)) {
                    reg.setProperties(properties);
                }
            }
            dispatchers.put(rt, reg);
        });
        m_dispatchers.values().forEach(ServiceRegistration::unregister);
        m_dispatchers = dispatchers;
    }

    private Hashtable<String, Object> toProperties(ServiceRegistration<?> reg) {
        Hashtable<String, Object> result = new Hashtable<>();
        ServiceReference<?> ref = reg.getReference();

        set(ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES, ref, result);
        set(ServletResolverConstants.SLING_SERVLET_EXTENSIONS, ref, result);
        set(ServletResolverConstants.SLING_SERVLET_SELECTORS, ref, result);
        set(ServletResolverConstants.SLING_SERVLET_METHODS, ref, result);
        result.put(REGISTERING_BUNDLE, reg.getReference().getBundle());

        return result;
    }

    private String getResourceType(Hashtable<String, Object> props) {
        String[] values = PropertiesUtil.toStringArray(props.get(ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES));
        int idx = values[0].indexOf("/");
        if (idx != -1) {
            return values[0].substring(0, idx);
        } else {
            return values[0];
        }
    }

    private String getResourceTypeVersion(ServiceReference<?> ref) {
        String[] values = PropertiesUtil.toStringArray(ref.getProperty(ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES));
        int idx = values[0].indexOf("/");
        if (idx != -1) {
            return values[0].substring(idx + 1);
        } else {
            return null;
        }
    }

    private void set(String key, ServiceReference<?> ref, Hashtable<String, Object> props) {
        Object value = ref.getProperty(key);
        if (value != null) {
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
        refreshDispatcher(Collections.EMPTY_LIST);
    }

    private class DispatcherServlet extends GenericServlet {
        private final String m_rt;

        DispatcherServlet(String rt) {
            m_rt = rt;
        }

        @Override
        public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {

            SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) req;

            Optional<ServiceRegistration<Servlet>> target = m_tracker.getTracked().values().stream().flatMap(List::stream)
                    .filter(
                            ((Predicate<ServiceRegistration<Servlet>>) reg -> reg.getReference().getBundle().equals(m_context.getBundle()))
                                    .negate()
                    )
                    .filter(reg -> getResourceTypeVersion(reg.getReference()) != null)
                    .filter(reg ->
                    {
                        Hashtable<String, Object> props = toProperties(reg);
                        return getResourceType(props).equals(m_rt) &&
                                Arrays.asList(PropertiesUtil
                                        .toStringArray(props.get(ServletResolverConstants.SLING_SERVLET_METHODS),
                                                new String[]{"GET", "HEAD"}))
                                        .contains(slingRequest.getMethod()) &&
                                Arrays.asList(PropertiesUtil
                                        .toStringArray(props.get(ServletResolverConstants.SLING_SERVLET_EXTENSIONS), new String[]{"html"}))
                                        .contains(slingRequest.getRequestPathInfo().getExtension() == null ? "html" :
                                                slingRequest.getRequestPathInfo().getExtension());
                    })
                    .sorted((left, right) ->
                    {
                        boolean la = Arrays.asList(PropertiesUtil
                                .toStringArray(toProperties(left).get(ServletResolverConstants.SLING_SERVLET_SELECTORS), new String[0]))
                                .containsAll(Arrays.asList(slingRequest.getRequestPathInfo().getSelectors()));
                        boolean ra = Arrays.asList(PropertiesUtil
                                .toStringArray(toProperties(right).get(ServletResolverConstants.SLING_SERVLET_SELECTORS), new String[0]))
                                .containsAll(Arrays.asList(slingRequest.getRequestPathInfo().getSelectors()));
                        if ((la && ra) || (!la && !ra)) {
                            return new Version(getResourceTypeVersion(right.getReference()))
                                    .compareTo(new Version(getResourceTypeVersion(left.getReference())));
                        } else if (la) {
                            return -1;
                        } else {
                            return 1;
                        }

                    })
                    .findFirst();

            if (target.isPresent()) {
                String rt = (String) target.get().getReference().getProperty(ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES);
                RequestDispatcherOptions options = new RequestDispatcherOptions();
                options.setForceResourceType(rt);

                RequestDispatcher dispatcher = slingRequest.getRequestDispatcher(slingRequest.getResource(), options);
                if (dispatcher != null) {
                    dispatcher.forward(req, res);
                } else {
                    ((SlingHttpServletResponse) res).sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            } else {
                ((SlingHttpServletResponse) res).sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }
}
