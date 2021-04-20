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
import org.apache.sling.ddr.api.DeclarativeDynamicResourceListener;
import org.apache.sling.ddr.api.DeclarativeDynamicResourceManager;
import org.apache.sling.ddr.api.DeclarativeDynamicResourceProvider;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.query.Query;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.apache.sling.ddr.api.Constants.CONFIGURATION_ROOT_PATH;
import static org.apache.sling.ddr.api.Constants.DDR_NODE_TYPE;
import static org.apache.sling.ddr.api.Constants.DDR_REF_PROPERTY_NAME;
import static org.apache.sling.ddr.api.Constants.DDR_TARGET_PROPERTY_NAME;
import static org.apache.sling.ddr.api.Constants.DYNAMIC_COMPONENTS_SERVICE_USER;
import static org.apache.sling.ddr.api.Constants.EQUALS;

@Component(
    service = { DeclarativeDynamicResourceManager.class, EventListener.class },
    immediate = true
)
@Designate(ocd = DeclarativeDynamicResourceManagerService.Configuration.class, factory = false)
public class DeclarativeDynamicResourceManagerService
    implements DeclarativeDynamicResourceManager, EventListener
{
    @ObjectClassDefinition(
        name = "Declarative Dynamic Component Resource Manager",
        description = "Configuration of the Dynamic Component Resource Manager")
    public @interface Configuration {
        @AttributeDefinition(
            name = "Allowed DDR Filter",
            description="If not empty filters out any resource without a single match, format: <property name>=<property value>")
        String[] allowed_ddr_filter();
        @AttributeDefinition(
            name = "Prohibited DDR Filter",
            description="Filters out any resource with a single match, format: <property name>=<property value>")
        String[] prohibited_ddr_filter();
        @AttributeDefinition(
            name = "Followed Link Names",
            description="Property Names of links to be followed")
        String[] followed_link_names() default DDR_REF_PROPERTY_NAME;
    }

    public static final int EVENT_TYPES =
        Event.NODE_ADDED |
        Event.NODE_REMOVED |
        Event.NODE_MOVED |
        Event.PROPERTY_ADDED |
        Event.PROPERTY_CHANGED |
        Event.PROPERTY_REMOVED;

    public static final String[] NODE_TYPES = new String[] { DDR_NODE_TYPE };

    @Reference
    ResourceResolverFactory resourceResolverFactory;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    DeclarativeDynamicResourceListener dynamicComponentFilterNotifier;

    // Make sure that the Service User Mapping is available before obtaining the Service Resource Resolver
    @Reference(policyOption= ReferencePolicyOption.GREEDY, target="(" + ServiceUserMapped.SUBSERVICENAME + EQUALS + DYNAMIC_COMPONENTS_SERVICE_USER + ")")
    private ServiceUserMapped serviceUserMapped;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Map<String, DeclarativeDynamicResourceProvider> registeredServicesByProvider = new HashMap<>();
    private Map<String, DeclarativeDynamicResourceProvider> registeredServicesByTarget = new HashMap<>();
    private BundleContext bundleContext;
    // Keep the Resource Resolver around otherwise the Event Listener will not work anymore
    private ResourceResolver resourceResolver;

    private Map<String, List<String>> allowedFilter = new HashMap<>();
    private Map<String, List<String>> prohibitedFilter = new HashMap<>();
    private List<String> followedLinkNames = new ArrayList<>();

    private Map<String, ReferenceEventListener> referenceListeners = new HashMap<>();

    @Activate
    void activate(BundleContext bundleContext, Configuration configuration) {
        this.bundleContext = bundleContext;
        log.info("Activate Started, bundle context: '{}'", bundleContext);
        // Parsing the Allowed / Prohibited DDR Filters
        parseDDRFilter(configuration.allowed_ddr_filter(), allowedFilter);
        parseDDRFilter(configuration.prohibited_ddr_filter(), prohibitedFilter);
        followedLinkNames.addAll(Arrays.asList(configuration.followed_link_names()));
        try {
            // The Resource Resolver needs to be kept alive until this Service is deactivated due to the Event Listeners
            resourceResolver = resourceResolverFactory.getServiceResourceResolver(
                new HashMap<String, Object>() {{ put(ResourceResolverFactory.SUBSERVICE, DYNAMIC_COMPONENTS_SERVICE_USER); }}
            );
            log.info("Service Resource Resolver: '{}'", resourceResolver);
            // Register an Event Listener to get informed when
            Session session = resourceResolver.adaptTo(Session.class);
            if (session != null) {
                log.info("Register Event Listener on Path: '{}'", CONFIGURATION_ROOT_PATH);
                session.getWorkspace().getObservationManager().addEventListener(
                    this, EVENT_TYPES, CONFIGURATION_ROOT_PATH,
                    true, null, null, false
                );
            } else {
                log.warn("Resource Resolver could not be adapted to Session");
            }
            // To make sure we get all we will query all existing nodes
            Resource root = resourceResolver.getResource(CONFIGURATION_ROOT_PATH);
            log.info("Manual Check for Existing Nodes in: '{}', root-res: '{}'", CONFIGURATION_ROOT_PATH, root);
            if(root != null) {
                Iterator<Resource> i = resourceResolver.findResources(
                    "SELECT * FROM [" + DDR_NODE_TYPE + "]",
                    Query.JCR_SQL2
                );
                log.info("DDR Nodes by Type: '{}', has next: '{}'", i, i.hasNext());
                while(i.hasNext()) {
                    Resource item = i.next();
                    log.info("Handle Found DDR Resource: '{}'", item);
                    handleDDRSource(item);
                }
            }
        } catch (LoginException e) {
            log.error("Unable to obtain our Service Resource Resolver --> DDR disabled", e);
        } catch (RepositoryException e) {
            log.error("Failed to Obtain the Observation Manager to register for Resource Events --> DDR disabled", e);
        }
    }

    private void parseDDRFilter(String[] filters, Map<String, List<String>> filterMap) {
        if(filters != null && filters.length > 0) {
            for(String filter: filters) {
                int index = filter.indexOf('=');
                if(index > 0 && index < filter.length() - 1) {
                    String name = filter.substring(0, index);
                    String value = filter.substring(index + 1);
                    List<String> values = filterMap.get(name);
                    if(values == null) {
                        values = new ArrayList<>();
                        filterMap.put(name, values);
                    }
                    if(!values.contains(value)) {
                        values.add(value);
                    }
                }
            }
        }
    }

    private void handleDDRSource(Resource resource) {
        if(resource != null) {
            // Find the Provider Root Resource
            Resource ddrProvider = findDDRSource(resource);
            if(ddrProvider != null) {
                ValueMap properties = ddrProvider.getValueMap();
                String ddrTargetPath = properties.get(DDR_TARGET_PROPERTY_NAME, String.class);
                log.info("Found DDR Target Path: '{}'", ddrTargetPath);
                Resource ddrTargetResource = resource.getResourceResolver().getResource(ddrTargetPath);
                if(ddrTargetResource != null) {
                    // Check if we already registered that service and if so update it instead of creating a new one
                    DeclarativeDynamicResourceProvider resourceProvider = registeredServicesByTarget.get(ddrTargetPath);
                    if (resourceProvider == null) {
                        DeclarativeDynamicResourceProviderHandler service = new DeclarativeDynamicResourceProviderHandler();
                        log.info("Dynamic Target: '{}', Dynamic Provider: '{}'", ddrTargetResource, ddrProvider);
                        long id = service.registerService(
                            bundleContext.getBundle(), ddrTargetPath, ddrProvider.getPath(), resourceResolver,
                            this,
                            allowedFilter, prohibitedFilter, followedLinkNames
                        );
                        log.info("After Registering Tenant RP: service: '{}', id: '{}'", service, id);
                        registeredServicesByTarget.put(ddrTargetResource.getPath(), service);
                        registeredServicesByProvider.put(ddrProvider.getPath(), service);
                        if (dynamicComponentFilterNotifier != null) {
                            dynamicComponentFilterNotifier.addDeclarativeDynamicResource(
                                ddrTargetPath, ddrProvider
                            );
                        }
                    } else {
                        resourceProvider.update(ddrTargetPath);
                    }
                }
            } else {
                // Provider Resource found -> check that this resource was previous target and if so remove it
                DeclarativeDynamicResourceProvider toBeRemoved = null;
                for(Entry<String, DeclarativeDynamicResourceProvider> entry: registeredServicesByProvider.entrySet()) {
                    if(resource.getPath().startsWith(entry.getKey())) {
                        toBeRemoved = entry.getValue();
                        break;
                    }
                }
                if(toBeRemoved != null) {
                    registeredServicesByProvider.remove(toBeRemoved.getProviderRootPath());
                    registeredServicesByTarget.remove(toBeRemoved.getTargetRootPath());
                    toBeRemoved.unregisterService();
                }
            }
        }
    }

    /**
     * Find the Resource in the Tree that contains the Target Path
     * @param resource The resource where the search starts
     * @return The resource containing the DDR Target Path or null if not found
     */
    private Resource findDDRSource(Resource resource) {
        Resource answer = null;
        if (resource != null) {
            ValueMap properties = resource.getValueMap();
            String ddrTargetPath = properties.get(DDR_TARGET_PROPERTY_NAME, String.class);
            if (ddrTargetPath == null) {
                answer = findDDRSource(resource.getParent());
            } else {
                answer = resource;
            }
        }
        return answer;
    }

    @Override
    public void update(String dynamicProviderPath) {
        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(
            new HashMap<String, Object>() {{ put(ResourceResolverFactory.SUBSERVICE, DYNAMIC_COMPONENTS_SERVICE_USER); }}
        )) {
            Resource dynamicProvider = resourceResolver.getResource(dynamicProviderPath);
            handleDDRSource(dynamicProvider);
        } catch (LoginException e) {
            log.error("Was not able to obtain Service Resource Resolver", e);
        }
    }

    @Override
    public void addReference(String sourcePath, String targetPath) {
        ReferenceEventListener referenceEventListener = null;
        for(Entry<String, ReferenceEventListener> entry: referenceListeners.entrySet()) {
            // If there is already a registered Event Listener for the given target or one of its parents when we ignore it
            if(targetPath.startsWith(entry.getValue().getReferencedPath())) {
                referenceEventListener = entry.getValue();
                break;
            }
            //AS TODO: this should be handled by the caller (Resource Provider) as he knows the Provider Root
//            //AS TODO: Should we only limit this to /conf/.../settings/dynamic ?
//            if(sourcePath.startsWith(CONFIGURATION_ROOT_PATH)) {
//                // A reference is pointing inside the Dynamic Folder which is already handled so we ignore it
//                referenceEventListener = entry.getValue();
//                break;
//            }
        }
        if(referenceEventListener == null) {
            referenceEventListener = new ReferenceEventListener();
            referenceEventListener.registerListener(sourcePath, targetPath, resourceResolver);
            referenceListeners.put(sourcePath, referenceEventListener);
        }
    }

    @Deactivate
    private void deactivate() {
        for(Entry<String, DeclarativeDynamicResourceProvider> entry: registeredServicesByTarget.entrySet()) {
            log.info("Before UnRegistering Tenant RP, service: '{}'", entry.getValue());
            entry.getValue().unregisterService();
            if(dynamicComponentFilterNotifier != null) {
                dynamicComponentFilterNotifier.removeDynamicDeclarativeResource(entry.getKey());
            }
            log.info("After UnRegistering Tenant RP, service: '{}'", entry.getValue());
        }
        registeredServicesByProvider.clear();
        registeredServicesByTarget.clear();
        for(Entry<String, ReferenceEventListener> entry: referenceListeners.entrySet()) {
            entry.getValue().unregisterListener(resourceResolver);
        }
        Session session = resourceResolver.adaptTo(Session.class);
        if (session != null) {
            log.info("Register Event Listener on Path: '{}'", CONFIGURATION_ROOT_PATH);
            try {
                session.getWorkspace().getObservationManager().removeEventListener(this);
            } catch (RepositoryException e) {
                // Ignore
            }
        }
        if(resourceResolver != null) {
            resourceResolver.close();
        }
    }

    @Override
    public void onEvent(EventIterator events) {
        log.info("Handle Events: '{}'", events);
        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(
            new HashMap<String, Object>() {{ put(ResourceResolverFactory.SUBSERVICE, DYNAMIC_COMPONENTS_SERVICE_USER); }}
        )) {
            while (events.hasNext()) {
                Event event = events.nextEvent();
                String path = event.getPath();
                log.info("Handle Event: '{}', path: '{}', type: '{}'", event, path, event.getType());
                switch (event.getType()) {
                    case Event.PROPERTY_ADDED:
                    case Event.PROPERTY_CHANGED:
                        int index = path.lastIndexOf('/');
                        if(index > 0) {
                            path = path.substring(0, index);
                        }
                        log.info("Property Added or Changed, path: '{}'", path);
                        handleNodeChange(path, true);
                        break;
                    case Event.NODE_ADDED:
                        handleNodeChange(path, true);
                        break;
                    case Event.NODE_REMOVED:
                        handleNodeRemoved(path);
                        break;
                    case Event.NODE_MOVED:
                        // The only thing to handle here is when the location changed
                        Map info = event.getInfo();
                        Object temp = info.get("srcAbsPath");
                        if(temp instanceof String) {
                            // Source found -> get target and remove it
                            String sourcePath = temp.toString();
                            handleNodeRemoved(sourcePath);
                        }
                        temp = info.get("destAbsPath");
                        if(temp instanceof String) {
                            // Destination found -> get target and add it
                            String destPath = temp.toString();
                            handleNodeChange(destPath, true);
                        }
                        break;
                    case Event.PROPERTY_REMOVED:
                        index = path.lastIndexOf('/');
                        if(index > 0) {
                            String resourcePath = path.substring(0, index);
                            handleNodeChange(resourcePath, false);
                        }
                }
            }
        } catch (LoginException | RepositoryException e) {
            log.error("Failed to Handle Events", e);
        }
    }

    private void handleNodeChange(String path, boolean added) {
        Resource source = resourceResolver.getResource(path);
        log.info("Source Resource found: '{}'", source);
        if(source != null) {
            handleDDRSource(source);
        }
    }

    private void handleNodeRemoved(String path) {
        DeclarativeDynamicResourceProvider toBeRemoved = null;
        for(Entry<String, DeclarativeDynamicResourceProvider> entry: registeredServicesByProvider.entrySet()) {
            if(entry.getKey().equals(path)) {
                // Provider to be removed found
                toBeRemoved = entry.getValue();
                break;
            } else if(path.startsWith(entry.getKey())) {
                // Sub Provider Node removed -> update Resource Provider
                entry.getValue().update(path);
                break;
            }
        }
        if(toBeRemoved != null) {
            registeredServicesByProvider.remove(toBeRemoved.getProviderRootPath());
            registeredServicesByTarget.remove(toBeRemoved.getTargetRootPath());
            toBeRemoved.unregisterService();
        }
    }

    Map<String, DeclarativeDynamicResourceProvider> getRegisteredServicesByTarget() {
        return Collections.unmodifiableMap(registeredServicesByTarget);
    }

    class ReferenceEventListener implements EventListener {

        private String sourcePath;
        private String referencedPath;

        /** @return The path of where the reference is found **/
        public String getSourcePath() {
            return sourcePath;
        }

        /** @return The path to where the reference points to **/
        public String getReferencedPath() {
            return referencedPath;
        }

        void registerListener(String sourcePath, String referencedPath, ResourceResolver resourceResolver) {
            this.sourcePath = sourcePath;
            this.referencedPath = referencedPath;
            Session session = resourceResolver.adaptTo(Session.class);
            if (session != null) {
                log.info("Register Event Listener on Path: '{}'", sourcePath);
                try {
                    session.getWorkspace().getObservationManager().addEventListener(
                        this, EVENT_TYPES, referencedPath,
                        true, null, null, false
                    );
                } catch (RepositoryException e) {
                    // Ignore
                }
            } else {
                log.warn("Resource Resolver could not be adapted to Session");
            }
        }

        void unregisterListener(ResourceResolver resourceResolver) {
            Session session = resourceResolver.adaptTo(Session.class);
            if (session != null) {
                log.info("Unregister Event Listener on Path: '{}'", sourcePath);
                try {
                    session.getWorkspace().getObservationManager().removeEventListener(this);
                } catch (RepositoryException e) {
                    // Ignore
                }
            } else {
                log.warn("Resource Resolver could not be adapted to Session");
            }
        }

        @Override
        public void onEvent(EventIterator events) {
            handleNodeChange(sourcePath, false);
        }
    }
}

