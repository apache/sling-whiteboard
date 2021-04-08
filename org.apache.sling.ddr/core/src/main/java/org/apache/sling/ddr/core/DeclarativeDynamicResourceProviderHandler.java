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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.ddr.api.DeclarativeDynamicResourceManager;
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
import static org.apache.sling.ddr.api.Constants.LABEL;
import static org.apache.sling.ddr.api.Constants.REP_POLICY;
import static org.apache.sling.ddr.api.Constants.SLASH;
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
    private ResourceResolver resourceResolver;
    private DeclarativeDynamicResourceManager declarativeDynamicResourceManager;
    private Map<String, List<String>> allowedDDRFilter;
    private Map<String, List<String>> prohibitedDDRFilter;
    private List<String> followedLinkNames;

    private Object lock = new Object();
    private Map<String,Reference> mappings = new HashMap<>();
    private Map<String,List<Reference>> childrenMappings = new HashMap<>();

    //---------- Service Registration

    public long registerService(
        Bundle bundle, String targetRootPath, String providerRootPath, ResourceResolver resourceResolver,
        DeclarativeDynamicResourceManager declarativeDynamicResourceManager,
        Map<String, List<String>> allowedDDRFilter, Map<String, List<String>> prohibitedDDRFilter, List<String> followedLinkNames
    ) {
        this.targetRootPath = targetRootPath;
        this.providerRootPath = providerRootPath;
        this.resourceResolver = resourceResolver;
        this.declarativeDynamicResourceManager = declarativeDynamicResourceManager;
        this.allowedDDRFilter = allowedDDRFilter == null ? new HashMap<String, List<String>>(): allowedDDRFilter;
        this.prohibitedDDRFilter = prohibitedDDRFilter == null ? new HashMap<String, List<String>>(): prohibitedDDRFilter;
        this.followedLinkNames = followedLinkNames == null ? new ArrayList<String>() : followedLinkNames;
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
    public String getProviderRootPath() {
        return providerRootPath;
    }

    public void update(String path) {
        synchronized (lock) {
            mappings.clear();
            childrenMappings.clear();
        }
    }

    @Override
    public Resource getResource(ResolveContext ctx, String path, ResourceContext resourceContext, Resource parent) {
        ResourceResolver contextResourceResolver = ctx.getResourceResolver();
        log.info("Get Resource, path: '{}', parent: '{}', provider root: '{}'", path, parent, providerRootPath);
        String resourcePath;
        if(path.startsWith(SLASH)) {
            resourcePath = path;
        } else {
            resourcePath = parent.getPath() + SLASH + path;
        }
        Resource answer = null;
        if(resourcePath.startsWith(providerRootPath)) {
            answer = getResource(contextResourceResolver, resourcePath);
        } else if(resourcePath.startsWith(targetRootPath)) {
            log.info("Before Getting Resource from Parent, path: '{}'", resourcePath);
            ResourceProvider parentResourceProvider = ctx.getParentResourceProvider();
            ResolveContext parentResolveContext = ctx.getParentResolveContext();
            log.info("Parent Resource Provider: '{}'", parentResourceProvider);
            log.info("Parent Resolve Context: '{}'", parentResolveContext);
            if (parentResourceProvider != null && parentResolveContext != null) {
                answer = parentResourceProvider.getResource(parentResolveContext, resourcePath, resourceContext, parent);
            }
            log.info("After Getting Resource from Parent, path: '{}', resource: '{}'", resourcePath, answer);
            if(answer == null) {
                synchronized (lock) {
                    Reference mappedPath = mappings.get(resourcePath);
                    if (mappedPath == null) {
                        // Obtain parent path and list children then try to re-obtain the mapping, if not found then there is no mapping
                        int index = resourcePath.lastIndexOf('/');
                        if (index > 0 && index < resourcePath.length() - 1) {
                            String parentPath = resourcePath.substring(0, index);
                            obtainChildren(contextResourceResolver, parentPath, false);
                            mappedPath = mappings.get(resourcePath);
                        }
                    }
                    if (mappedPath != null) {
                        Resource source = getResource(
                            contextResourceResolver, mappedPath.isRef() ?
                                mappedPath.getReference():
                                mappedPath.getSource()
                        );
                        int index = resourcePath.lastIndexOf('/');
                        String parentPath = "";
                        if (index > 0 && index < resourcePath.length() - 1) {
                            parentPath = resourcePath.substring(0, index);
                        }
                        answer = createSyntheticFromResource(
                            source, resourcePath, parentPath.equals(targetRootPath)
                        );
                    }
                }
            }
        } else {
            answer = getResource(contextResourceResolver, resourcePath);
        }
        log.info("Return resource: '{}'", answer);
        return answer;
    }

    @Override
    public Iterator<Resource> listChildren(ResolveContext ctx, Resource parent) {
        Iterator<Resource> answer = null;
        log.info("List Children, resolve-context: '{}', parent: '{}'", ctx, parent);
        String resourcePath = parent.getPath();
        ResourceResolver contextResourceResolver = ctx.getResourceResolver();
        if(resourcePath.equals(providerRootPath)) {
            // Handle the Source / Provider Path -> no DDRs here
            answer = parent.listChildren();
        } else if(resourcePath.startsWith(targetRootPath)) {
            // Handle the dynamic path
            ResourceProvider parentResourceProvider = ctx.getParentResourceProvider();
            ResolveContext parentResolveContext = ctx.getParentResolveContext();
            List<Resource> items = new ArrayList<>();
            Iterator<Resource> i;
            if (parentResourceProvider != null && parentResolveContext != null) {
                // First obtain the Children from the Parent Resource Provider (JCR)
                i = parentResourceProvider.listChildren(parentResolveContext, parent);
                if (i != null) {
                    while (i.hasNext()) {
                        items.add(i.next());
                    }
                }
            }
            synchronized (lock) {
                // Obtain the matching resource from the provider
                List<Reference> childrenList = childrenMappings.get(resourcePath);
                log.info("Resource Path: '{}', Children List: '{}'", resourcePath, childrenList);
                if (childrenList != null) {
                    for (Reference childPath : childrenList) {
                        Resource child = getResource(contextResourceResolver, childPath.getReference());
                        int index = childPath.getSource().lastIndexOf('/');
                        String childName = childPath.getSource().substring(index);
                        items.add(
                            createSyntheticFromResource(
                                child, resourcePath + childName,
                                resourcePath.equals(targetRootPath)
                            )
                        );
                    }
                } else {
                    items = obtainChildren(contextResourceResolver, resourcePath, true);
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
            boolean found = allowedDDRFilter.isEmpty();
            for (Entry<String, List<String>> filter : allowedDDRFilter.entrySet()) {
                String propertyValue = properties.get(filter.getKey(), String.class);
                if (propertyValue != null) {
                    if (!filter.getValue().contains(propertyValue)) {
                        found = true;
                        break;
                    }
                }
            }
            if(!found) {
                return false;
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

    private List<Resource> obtainChildren(ResourceResolver contextResourceResolver, String resourcePath, boolean returnChildren) {
        List<Resource> answer = new ArrayList<>();
        String postfix = resourcePath.substring(targetRootPath.length());
        if (!postfix.isEmpty() && postfix.charAt(0) == '/') {
            postfix = postfix.substring(1);
        }
        List<Reference> childrenList = new ArrayList<>();
        childrenMappings.put(resourcePath, childrenList);
        String targetPath = providerRootPath + SLASH + postfix;
        Reference ref = mappings.get(resourcePath);
        Resource provider =
            getResource(
                contextResourceResolver,
                ref == null || !ref.isRef() ?
                    targetPath:
                    ref.getReference()
            );
        log.info("Provider, Path: '{}', Resource: '{}'", targetPath, provider);
        if (provider != null) {
            Iterator<Resource> i = provider.listChildren();
            while (i.hasNext()) {
                Resource child = i.next();
                // Ignore Policy Nodes
                if (child.getName().equals(REP_POLICY)) {
                    continue;
                }
                if (filterSource(child)) {
                    // Check if this entry is a reference and if so get that one instead
                    ValueMap properties = child.getValueMap();
                    String referencePath = null;
                    boolean handled = false;
                    for (String followedLinkName : followedLinkNames) {
                        referencePath = properties.get(followedLinkName, String.class);
                        if (referencePath != null && !referencePath.isEmpty()) {
                            Resource reference = getResource(contextResourceResolver, referencePath);
                            if (reference != null && !reference.isResourceType(RESOURCE_TYPE_NON_EXISTING)) {
                                log.info("Add Path: '{}' to children list", resourcePath);
                                childrenList.add(new Reference(child.getPath(), referencePath));
                                String parentPath = targetRootPath + (postfix.isEmpty() ? "" : SLASH + postfix);
                                mappings.put(parentPath + SLASH + child.getName(), new Reference(child.getPath(), referencePath));
                                declarativeDynamicResourceManager.addReference(child.getPath(), referencePath);
                                if(returnChildren) {
                                    answer.add(
                                        createSyntheticFromResource(
                                            reference,
                                            parentPath + SLASH + child.getName(),
                                            parentPath.equals(targetRootPath)
                                        )
                                    );
                                }
                                handled = true;
                            } else {
                                log.warn("Reference: '{}' provided by does not resolve to a resource", referencePath);
                            }
                        }
                    }
                    if(!handled) {
                        // Not a reference
                        log.info("Add Path: '{}' to children list", child.getPath());
                        Reference newRef;
                        if(ref != null && ref.isRef()) {
                            newRef = new Reference(ref.getSource() + SLASH + child.getName(), child.getPath());
                        } else {
                            newRef = new Reference(child.getPath());
                        }
                        childrenList.add(newRef);
                        String parentPath = targetRootPath + (postfix.isEmpty() ? "" : SLASH + postfix);
                        mappings.put(parentPath + SLASH + child.getName(), newRef);
                        if(returnChildren) {
                            answer.add(
                                createSyntheticFromResource(
                                    child, parentPath + SLASH + child.getName(),
                                    parentPath.equals(targetRootPath)
                                )
                            );
                        }
                    }
                }
            }
        }
        return answer;
    }

    private Resource getResource(ResourceResolver contextResourceResolver, String path) {
        Resource answer = null;
        if(contextResourceResolver != null) {
            answer = contextResourceResolver.getResource(path);
        }
        if(answer == null) {
            answer = resourceResolver.getResource(path);
        }
        return answer;
    }

    private class Reference {
        private String source;
        private String reference;
        private boolean ref;

        public Reference(String source) {
            this(source, null);
        }

        public Reference(String source, String reference) {
            this.source = source;
            this.reference = reference;
            this.ref = reference != null && !this.reference.equals(this.source);
        }

        public String getSource() {
            return source;
        }

        public String getReference() {
            return reference == null ? source : reference;
        }

        public boolean isRef() {
            return ref;
        }
    }
}

