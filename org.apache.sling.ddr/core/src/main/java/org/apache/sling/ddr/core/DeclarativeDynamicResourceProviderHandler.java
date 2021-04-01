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
package org.apache.sling.ddr.core;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.ddr.api.DeclarativeDynamicResourceProvider;
import org.apache.sling.spi.resource.provider.ProviderContext;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.apache.sling.api.resource.Resource.RESOURCE_TYPE_NON_EXISTING;
import static org.apache.sling.ddr.api.Constants.DYNAMIC_COMPONENTS_SERVICE_USER;
import static org.apache.sling.ddr.api.Constants.LABEL;
import static org.apache.sling.ddr.api.Constants.REFERENCE_PROPERTY_NAME;
import static org.apache.sling.ddr.api.Constants.REP_POLICY;
import static org.apache.sling.ddr.api.Constants.SlASH;
import static org.apache.sling.ddr.core.DeclarativeDynamicResourceImpl.createSyntheticFromResource;
import static org.osgi.framework.Constants.SERVICE_DESCRIPTION;
import static org.osgi.framework.Constants.SERVICE_VENDOR;

/**
 * This a Resource Provider that provides a Dynamic Component that is not available in
 * the given source folder. It will then create a Synthetic Resource that points a component
 * in the provider folder to the source folder.
 */
public class DeclarativeDynamicResourceProviderHandler
    extends ResourceProvider
    implements DeclarativeDynamicResourceProvider
{
    private final Logger log = LoggerFactory.getLogger(DeclarativeDynamicResourceProviderHandler.class);

    @SuppressWarnings("rawtypes")
    private volatile ServiceRegistration serviceRegistration;

    private String targetRootPath;
    private String providerRootPath;
    private boolean active;
    private ResourceResolverFactory resourceResolverFactory;
    private Map<String, List<String>> allowedDDRFilter;
    private Map<String, List<String>> prohibitedDDRFilter;

    //---------- Service Registration

    public long registerService(
        Bundle bundle, String targetRootPath, String providerRootPath, ResourceResolverFactory resourceResolverFactory,
        Map<String, List<String>> allowedDDRFilter, Map<String, List<String>> prohibitedDDRFilter
    ) {
        this.targetRootPath = targetRootPath;
        this.providerRootPath = providerRootPath;
        this.resourceResolverFactory = resourceResolverFactory;
        this.allowedDDRFilter = allowedDDRFilter;
        this.prohibitedDDRFilter = prohibitedDDRFilter;
        log.info("Target Root Path: '{}', Provider Root Paths: '{}'", targetRootPath, providerRootPath);

        final Dictionary<String, Object> props = new Hashtable<>();
        props.put(LABEL, "Dynamic Component Resource: '" + targetRootPath + "'");
        props.put(SERVICE_DESCRIPTION, "Provides the Dynamic Component for '" + targetRootPath + "' resources as synthetic resources");
        props.put(SERVICE_VENDOR, "The Apache Software Foundation");
        props.put(ResourceProvider.PROPERTY_ROOT, targetRootPath);
        props.put(getClass().getName(), bundle.getBundleId());

        log.info("Before Register RARPS with props: '{}'", props);
        serviceRegistration = bundle.getBundleContext().registerService(
            new String[] {ResourceProvider.class.getName(), DeclarativeDynamicResourceProvider.class.getName()}, this, props
        );
        log.info("After Register RARPS, service registration: '{}'", serviceRegistration);
        active = true;
        return (Long) serviceRegistration.getReference().getProperty(Constants.SERVICE_ID);
    }

    public void unregisterService() {
        if (serviceRegistration != null) {
            try {
                serviceRegistration.unregister();
            } catch ( final IllegalStateException ise ) {
                // this might happen on shutdown, so ignore
            }
            active = false;
            serviceRegistration = null;
        }
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public String getTargetRootPath() {
        return targetRootPath;
    }

    @Override
    public Resource getResource(ResolveContext ctx, String path, ResourceContext resourceContext, Resource parent) {
        ResourceResolver resourceResolver = ctx.getResourceResolver();
        log.info("Get Resource, path: '{}', parent: '{}', provider root: '{}'", path, parent, providerRootPath);
        String resourcePath;
        if(path.startsWith(SlASH)) {
            resourcePath = path;
        } else {
            resourcePath = parent.getPath() + SlASH + path;
        }
        Resource answer = null;
        if(resourcePath.startsWith(providerRootPath)) {
            answer = resourceResolver.getResource(resourcePath);
        } else if(resourcePath.equals(targetRootPath)) {
            log.info("1. Before Getting Resource from Parent, path: '{}', context: '{}'", resourcePath, ctx);
            ResourceProvider parentResourceProvider = ctx.getParentResourceProvider();
            ResolveContext parentResolveContext = ctx.getParentResolveContext();
            log.info("Parent Resource Provider: '{}'", parentResourceProvider);
            log.info("Parent Resolve Context: '{}'", parentResolveContext);
            if (parentResourceProvider != null && parentResolveContext != null) {
                answer = parentResourceProvider.getResource(parentResolveContext, resourcePath, resourceContext, parent);
            }
            log.info("1. After Getting Resource from Parent, path: '{}', resource: '{}'", resourcePath, answer);
            if(answer == null) {
                Resource source = resourceResolver.getResource(providerRootPath);
                if(filterSource(source)) {
                    answer = createSyntheticFromResource(resourceResolver, source, resourcePath);
                }
            }
        } else if(resourcePath.startsWith(targetRootPath)) {
            log.info("2. Before Getting Resource from Parent, path: '{}'", resourcePath);
            ResourceProvider parentResourceProvider = ctx.getParentResourceProvider();
            ResolveContext parentResolveContext = ctx.getParentResolveContext();
            log.info("Parent Resource Provider: '{}'", parentResourceProvider);
            log.info("Parent Resolve Context: '{}'", parentResolveContext);
            if (parentResourceProvider != null && parentResolveContext != null) {
                answer = parentResourceProvider.getResource(parentResolveContext, resourcePath, resourceContext, parent);
            }
            log.info("2. After Getting Resource from Parent, path: '{}', resource: '{}'", resourcePath, answer);
            if(answer == null) {
                int index = resourcePath.lastIndexOf('/');
                if (index > 0 && index < (resourcePath.length() - 1)) {
                    String name = resourcePath.substring(index + 1);
                    Resource providerRoot = resourceResolver.getResource(providerRootPath);
                    if(providerRoot == null) {
                        try (ResourceResolver resourceResolver1 = resourceResolverFactory.getServiceResourceResolver(
                            new HashMap<String, Object>() {{ put(ResourceResolverFactory.SUBSERVICE, DYNAMIC_COMPONENTS_SERVICE_USER); }}
                        )) {
                            providerRoot = resourceResolver1.getResource(providerRootPath);
                            Resource source = providerRoot.getChild(name);
                            if (source != null && !source.isResourceType(RESOURCE_TYPE_NON_EXISTING)) {
                                if(filterSource(source)) {
                                    answer = createSyntheticFromResource(resourceResolver, source, resourcePath);
                                }
                            }
                        } catch (LoginException e) {
                            log.error("Was not able to obtain Service Resource Resolver", e);
                        }
                    } else {
                        Resource source = providerRoot.getChild(name);
                        if (source != null && !source.isResourceType(RESOURCE_TYPE_NON_EXISTING)) {
                            if(filterSource(source)) {
                                answer = createSyntheticFromResource(resourceResolver, source, resourcePath);
                            }
                        }
                    }
                }
            }
        } else {
            answer = resourceResolver.getResource(resourcePath);
        }
        log.info("Return resource: '{}'", answer);
        return answer;
    }

    @Override
    public Iterator<Resource> listChildren(ResolveContext ctx, Resource parent) {
        Iterator<Resource> answer = null;
        log.info("List Children, resolve-context: '{}', parent: '{}'", ctx, parent);
        String resourcePath = parent.getPath();
        ResourceResolver resourceResolver = ctx.getResourceResolver();
        if(resourcePath.equals(providerRootPath)) {
            answer = parent.listChildren();
        } else if(resourcePath.startsWith(targetRootPath)) {
            ResourceProvider parentResourceProvider = ctx.getParentResourceProvider();
            ResolveContext parentResolveContext = ctx.getParentResolveContext();
            log.info("Parent Resource Provider: '{}'", parentResourceProvider);
            log.info("Parent Resolve Context: '{}'", parentResolveContext);
            List<Resource> items = new ArrayList<>();
            Iterator<Resource> i = null;
            if (parentResourceProvider != null && parentResolveContext != null) {
                i = parentResourceProvider.listChildren(parentResolveContext, parent);
                if (i != null) {
                    while (i.hasNext()) {
                        items.add(i.next());
                    }
                }
            }
            String postfix = resourcePath.substring(targetRootPath.length());
            if(!postfix.isEmpty() && postfix.charAt(0) == '/') {
                postfix = postfix.substring(1);
            }
            String targetPath = providerRootPath + SlASH + postfix;
            Resource provider = resourceResolver.getResource(targetPath);
            log.info("Provider, Path: '{}', Resource: '{}'", targetPath, provider);
            if(provider != null) {
                i = provider.listChildren();
                while (i.hasNext()) {
                    Resource child = i.next();
                    // Ignore Policy Nodes
                    if(child.getName().equals(REP_POLICY)) {
                        continue;
                    }
                    // Check if this entry is a reference and if so get that one instead
                    ValueMap properties = child.getValueMap();
                    String referencePath = properties.get(REFERENCE_PROPERTY_NAME, String.class);
                    if(referencePath != null && !referencePath.isEmpty()) {
                        Resource reference = resourceResolver.getResource(referencePath);
                        if(reference != null) {
                            items.add(reference);
                        } else {
                            log.warn("Reference: '{}' provided by does not resolve to a resource", referencePath);
                        }
                    } else {
                        if(filterSource(child)) {
                            items.add(createSyntheticFromResource(resourceResolver, child, targetRootPath + SlASH + child.getName()));
                        }
                    }
                }
            }
            answer = items.iterator();
        } else {
            ResourceProvider parentResourceProvider = ctx.getParentResourceProvider();
            ResolveContext parentResolveContext = ctx.getParentResolveContext();
            log.info("Parent Resource Provider: '{}'", parentResourceProvider);
            log.info("Parent Resolve Context: '{}'", parentResolveContext);
            if (parentResourceProvider != null && parentResolveContext != null) {
                answer = parentResourceProvider.listChildren(parentResolveContext, parent);
            }
        }
        return answer != null && answer.hasNext() ? answer : null;
    }

    @Override
    public void start(ProviderContext ctx) {
        log.info("Provider Start, context: '{}'", ctx);
        super.start(ctx);
    }

    @Override
    public void stop() {
        log.info("Provider Stop");
        super.stop();
    }

    boolean filterSource(Resource source) {
        if(allowedDDRFilter.isEmpty() && prohibitedDDRFilter.isEmpty()) { return true; }
        if(source == null) { return false; }
        ValueMap properties = source.getValueMap();
        if(!allowedDDRFilter.isEmpty()) {
            for (Entry<String, List<String>> filter : allowedDDRFilter.entrySet()) {
                String propertyValue = properties.get(filter.getKey(), String.class);
                if (propertyValue != null) {
                    if (!filter.getValue().contains(propertyValue)) {
                        return false;
                    }
                }
            }
        }
        for(Entry<String,List<String>> filter: prohibitedDDRFilter.entrySet()) {
            String propertyValue = properties.get(filter.getKey(), String.class);
            if(propertyValue != null) {
                if(filter.getValue().contains(propertyValue)) {
                    return false;
                }
            }
        }
        return true;
    }
}
