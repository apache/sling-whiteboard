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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.query.Query;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.apache.sling.ddr.api.Constants.DYNAMIC_COMPONENTS_SERVICE_USER;
import static org.apache.sling.ddr.api.Constants.EQUALS;
import static org.apache.sling.ddr.api.Constants.JCR_PRIMARY_TYPE;
import static org.apache.sling.ddr.api.Constants.REP_POLICY;

@Component(
    service = { DeclarativeDynamicResourceManager.class, EventListener.class },
    immediate = true
)
public class DeclarativeDynamicResourceManagerService
    implements DeclarativeDynamicResourceManager, EventListener
{
    public static final int EVENT_TYPES =
        Event.NODE_ADDED |
        Event.NODE_REMOVED |
        Event.NODE_MOVED |
        Event.PROPERTY_ADDED |
        Event.PROPERTY_CHANGED |
        Event.PROPERTY_REMOVED;

    public static final String DDR_NODE_TYPE = "slingddr:Folder";
    public static final String DDR_TARGET_PROPERTY_NAME = "slingddr:target";
    public static final String[] NODE_TYPES = new String[] { DDR_NODE_TYPE };
    public static final String CONFIGURATION_ROOT_PATH = "/conf";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private DeclarativeDynamicResourceListener dynamicComponentFilterNotifier;

    // Make sure that the Service User Mapping is available before obtaining the Service Resource Resolver
    @Reference(policyOption= ReferencePolicyOption.GREEDY, target="(" + ServiceUserMapped.SUBSERVICENAME + EQUALS + DYNAMIC_COMPONENTS_SERVICE_USER + ")")
    private ServiceUserMapped serviceUserMapped;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Map<String, DeclarativeDynamicResourceProvider> registeredServices = new HashMap<>();
    private BundleContext bundleContext;
//    private String dynamicTargetPath;

    @Activate
    private void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        log.info("Activate Started, bundle context: '{}'", bundleContext);
        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(
            new HashMap<String, Object>() {{ put(ResourceResolverFactory.SUBSERVICE, DYNAMIC_COMPONENTS_SERVICE_USER); }}
        )) {
            // Register an Event Listener to get informed when
            Session session = resourceResolver.adaptTo(Session.class);
            if (session != null) {
                log.info("Register Event Listener on Path: '{}'", CONFIGURATION_ROOT_PATH);
                session.getWorkspace().getObservationManager().addEventListener(
                    this, EVENT_TYPES, CONFIGURATION_ROOT_PATH,
                    true, null, null, true
                );
            } else {
                log.warn("Resource Resolver could not be adapted to Session");
            }
            // To make sure we get all we will query all existing nodes
            Resource root = resourceResolver.getResource(CONFIGURATION_ROOT_PATH);
            log.info("Manual Check for Existing Nodes in: '{}'", CONFIGURATION_ROOT_PATH);
            if(root != null) {
                Iterator<Resource> i = resourceResolver.findResources(
                    "SELECT * FROM [slingddr:Folder]",
                    Query.JCR_SQL2
                );
                while(i.hasNext()) {
                    Resource item = i.next();
                    log.info("Handle Found DDR Resource: '{}'", item);
                    handleDDRResource(item);
                }
            }
        } catch (LoginException e) {
            log.error("Failed to Activation Resource", e);
        } catch (UnsupportedRepositoryOperationException e) {
            log.error("Failed to Activation Resource", e);
        } catch (RepositoryException e) {
            log.error("Failed to Activation Resource", e);
        }
    }

    private void handleDDRResource(Resource ddrSourceResource) {
        if(ddrSourceResource != null) {
            ValueMap properties = ddrSourceResource.getValueMap();
            String ddrTargetPath = properties.get(DDR_TARGET_PROPERTY_NAME, String.class);
            log.info("Found DDR Target Path: '{}'", ddrTargetPath);
            if (ddrTargetPath != null) {
                Resource ddrTargetResource = ddrSourceResource.getResourceResolver().getResource(ddrTargetPath);
                if(ddrTargetResource != null) {
                    DeclarativeDynamicResourceProviderHandler service = new DeclarativeDynamicResourceProviderHandler();
                    log.info("Dynamic Target: '{}', Dynamic Provider: '{}'", ddrSourceResource, ddrSourceResource);
                    long id = service.registerService(bundleContext.getBundle(), ddrTargetPath, ddrSourceResource.getPath(), resourceResolverFactory);
                    log.info("After Registering Tenant RP: service: '{}', id: '{}'", service, id);
                    registeredServices.put(ddrTargetResource.getPath(), service);
                    Iterator<Resource> i = ddrSourceResource.listChildren();
                    while (i.hasNext()) {
                        Resource provided = i.next();
                        String componentName = provided.getName();
                        if (componentName.equals(REP_POLICY)) {
                            continue;
                        }
                        log.info("Provided Dynamic: '{}'", provided);
                        ValueMap childProperties = provided.getValueMap();
                        String primaryType = childProperties.get(JCR_PRIMARY_TYPE, String.class);
                        log.info("Dynamic Child Source: '{}', Primary Type: '{}'", componentName, primaryType);
                        if (componentName != null && !componentName.isEmpty() && dynamicComponentFilterNotifier != null) {
                            dynamicComponentFilterNotifier.addDeclarativeDynamicResource(
                                ddrTargetPath + '/' + componentName, provided
                            );
                        }
                    }
                }
            }
        }
    }

    public void update(String dynamicProviderPath) {
        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(
            new HashMap<String, Object>() {{ put(ResourceResolverFactory.SUBSERVICE, DYNAMIC_COMPONENTS_SERVICE_USER); }}
        )) {
            Resource dynamicProvider = resourceResolver.getResource(dynamicProviderPath);
            handleDDRResource(dynamicProvider);
        } catch (LoginException e) {
            log.error("Was not able to obtain Service Resource Resolver", e);
        }
    }

    @Deactivate
    private void deactivate() {
        for(DeclarativeDynamicResourceProvider service: registeredServices.values()) {
            log.info("Before UnRegistering Tenant RP, service: '{}'", service);
            service.unregisterService();
            log.info("After UnRegistering Tenant RP, service: '{}'", service);
        }
    }

    @Override
    public void onEvent(EventIterator events) {
        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(
            new HashMap<String, Object>() {{ put(ResourceResolverFactory.SUBSERVICE, DYNAMIC_COMPONENTS_SERVICE_USER); }}
        )) {
            while (events.hasNext()) {
                Event event = events.nextEvent();
                String path = event.getPath();
                switch (event.getType()) {
                    case Event.PROPERTY_ADDED:
                    case Event.PROPERTY_CHANGED:
                        int index = path.lastIndexOf('/');
                        if(index > 0) {
                            path = path.substring(0, index -1);
                        }
                    case Event.NODE_ADDED:
                        Resource source = resourceResolver.getResource(path);
                        if(source != null) {
                            handleDDRResource(source);
                        }
                        break;
                    case Event.NODE_MOVED:
                        //AS TODO: Handle later
                        break;
                    case Event.PROPERTY_REMOVED:
                        //AS TODO: Handle later
//                        break;
                }
            }
        } catch (LoginException | RepositoryException e) {
            log.error("Failed to Handle Events", e);
        }
    }
}

