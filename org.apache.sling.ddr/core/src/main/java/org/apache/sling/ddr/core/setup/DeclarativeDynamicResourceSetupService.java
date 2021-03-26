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
package org.apache.sling.ddr.core.setup;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.ddr.api.DeclarativeDynamicResourceSetup;
import org.apache.sling.ddr.api.DeclarativeDynamicResourceManager;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.apache.sling.ddr.api.Constants.CLOSING_MULTIPLE;
import static org.apache.sling.ddr.api.Constants.COMPONENT_GROUP;
import static org.apache.sling.ddr.api.Constants.DYNAMIC_COMPONENTS_SERVICE_USER;
import static org.apache.sling.ddr.api.Constants.DYNAMIC_COMPONENT_FOLDER_NAME;
import static org.apache.sling.ddr.api.Constants.EQUALS;
import static org.apache.sling.ddr.api.Constants.JCR_PRIMARY_TYPE;
import static org.apache.sling.ddr.api.Constants.JCR_TITLE;
import static org.apache.sling.ddr.api.Constants.MULTI_SEPARATOR;
import static org.apache.sling.ddr.api.Constants.NT_UNSTRUCTURED;
import static org.apache.sling.ddr.api.Constants.OPENING_MULTIPLE;
import static org.apache.sling.ddr.api.Constants.REFERENCE_PROPERTY_NAME;
import static org.apache.sling.ddr.api.Constants.REP_POLICY;
import static org.apache.sling.ddr.api.Constants.SLING_FOLDER;
import static org.apache.sling.ddr.api.Constants.SLING_RESOURCE_SUPER_TYPE_PROPERTY;
import static org.apache.sling.ddr.api.Constants.VERTICAL_LINE;

@Component(
    service= { DeclarativeDynamicResourceSetup.class, EventListener.class },
    immediate = true
)
@Designate(ocd = DeclarativeDynamicResourceSetupService.Configuration.class, factory = true)
public class DeclarativeDynamicResourceSetupService
    implements DeclarativeDynamicResourceSetup, EventListener
{

    @ObjectClassDefinition(
        name = "Declarative Dynamic Resource Setup",
        description = "Configuration of the Setup for Declarative Dynamic Resource Provider")
    public @interface Configuration {
        @AttributeDefinition(
            name = "Dynamic Target Path",
            description = "The Location where the DDR are made available")
        String dynamic_component_target_path() default "/apps/wknd/components";
        @AttributeDefinition(
            name = "Dynamic Source Root Path",
            description = "The Location of the Source for the DDRs")
        String dynamic_component_root_path() default "/conf/wknd/settings";
        @AttributeDefinition(
            name = "Component Group of the Dynamic Components",
            description = "Component Group Name")
        String dynamic_component_group() default "WKND.Content";
        @AttributeDefinition(
            name = "Primary Group",
            description = "Component Primary Type")
        String dynamic_component_primary_type() default "cq:Component";
        @AttributeDefinition(
            name = "List of Dynamic Components",
            description = "Dynamic Component Definitions in format: <name>=<title>:<super resource type>")
        String[] dynamic_component_names() default "button=Button-default:core/wcm/components/button/v1/button";
        @AttributeDefinition(
            name = "Additional Properties for Dynamic Components",
            description = "Dynamic Component Additional Properties in format: <name>=<property name>|<property value>")
        String[] dynamic_component_additional_properties() default "";
        @AttributeDefinition(
            name = "References for Dynamic Components",
            description = "Dynamic Component Reference in format: <name>=<path>")
        String[] dynamic_component_refs() default "";
    }

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    DeclarativeDynamicResourceManager dynamicComponentResourceManager;

    // Make sure that the Service User Mapping is available before obtaining the Service Resource Resolver
    @Reference(policyOption= ReferencePolicyOption.GREEDY, target="(" + ServiceUserMapped.SUBSERVICENAME + EQUALS + DYNAMIC_COMPONENTS_SERVICE_USER + ")")
    private ServiceUserMapped serviceUserMapped;

    private BundleContext bundleContext;
    private String rootPath;
    private String targetRootPath;

    @Activate
    private void activate(BundleContext bundleContext, Configuration configuration) {
        log.info("Activate Started, bundle context: '{}'", bundleContext);
        this.bundleContext = bundleContext;
        rootPath = configuration.dynamic_component_root_path();
        targetRootPath = configuration.dynamic_component_target_path();
        final String group = configuration.dynamic_component_group();
        final String primaryType = configuration.dynamic_component_primary_type();
        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(
            new HashMap<String, Object>() {{ put(ResourceResolverFactory.SUBSERVICE, DYNAMIC_COMPONENTS_SERVICE_USER); }}
        )) {
            Resource root = resourceResolver.getResource(rootPath);
            if(root == null) {
                throw new IllegalArgumentException("Root Path: '" + rootPath + "' does not exist");
            }
            Resource target = root.getChild(DYNAMIC_COMPONENT_FOLDER_NAME);
            log.info("Dynamic Folder looked up: '{}'", target);
            if(target == null) {
                target = resourceResolver.create(root, DYNAMIC_COMPONENT_FOLDER_NAME, new HashMap<String, Object>() {{
                        put(JCR_PRIMARY_TYPE, SLING_FOLDER);
                    }}
                );
                resourceResolver.commit();
                log.info("Dynamic Folder created: '{}'", target);
            } else {
                // Remove any existing children
                Iterator<Resource> i = target.listChildren();
                while(i.hasNext()) {
                    Resource child = i.next();
                    if(!child.getName().equals(REP_POLICY)) {
                        log.info("Delete Existing Resource: '{}'", child);
                        resourceResolver.delete(child);
                    }
                }
                resourceResolver.commit();
            }
            Map<String, List<Property>> additionalProperties = new HashMap<>();
            for(String additionalProperty: configuration.dynamic_component_additional_properties()) {
                Property component = new Property(additionalProperty, "Dynamic Additional Property");
                if(!component.isComponent()) {
                    throw new IllegalArgumentException("Addition Properties is not a component: '" + additionalProperty + "'");
                }
                addItemToListMap(additionalProperties, component);
            }
            Map<String, List<Property>> dynamicRefs = new HashMap<>();
            for(String ref: configuration.dynamic_component_refs()) {
                Property component = new Property(ref, "Dynamic Ref");
                if(!component.isComponent()) {
                    throw new IllegalArgumentException("Dynamic Ref is not a component: '" + ref + "'");
                }
                addItemToListMap(dynamicRefs, component);
            }
            log.info("Dynamic Refs: '{}'", dynamicRefs);
            for (String dynamicComponentName : configuration.dynamic_component_names()) {
                final Property dynamicComponent = new Property(dynamicComponentName, "Dynamic Component");
                if(!dynamicComponent.isComponent()) {
                    throw new IllegalArgumentException("Dynamic Configuration Name is invalid (split on = does not yield 2 tokens): " + dynamicComponentName);
                }
                Map<String, Object> props = new HashMap<String, Object>() {{
                    put(COMPONENT_GROUP, group);
                    put(JCR_PRIMARY_TYPE, primaryType);
                    put(JCR_TITLE, dynamicComponent.getName());
                    put(SLING_RESOURCE_SUPER_TYPE_PROPERTY, dynamicComponent.getValue());
                }};
                List<Property> propertyList = additionalProperties.get(dynamicComponent.getComponent());
                log.info("Component: '{}', property list: '{}'", dynamicComponent.getComponent(), propertyList);
                if(propertyList != null) {
                    for (Property property : propertyList) {
                        if(property.isSingle()) {
                            props.put(property.getName(), property.getValue());
                        } else {
                            log.info("Add Property as Multi-Value: '{}'", property.getValues());
                            props.put(property.getName(), property.getValues().toArray());
                        }
                    }
                }
                log.info("Props for to be created Node: '{}'", props);
                Resource newTarget = resourceResolver.create(target, dynamicComponent.getComponent(), props);
                log.info("Newly Created Target: '{}'", newTarget);
                // Add Dynamic Refs
                List<Property> refs = dynamicRefs.get(dynamicComponent.getComponent());
                if(refs != null) {
                    for (final Property ref : refs) {
                        Map<String, Object> refProps = new HashMap<String, Object>() {{
                            put(JCR_PRIMARY_TYPE, NT_UNSTRUCTURED);
                            put(JCR_TITLE, ref.getName());
                            put(REFERENCE_PROPERTY_NAME, ref.getValue());
                        }};
                        Resource newRef = resourceResolver.create(
                            newTarget, ref.getName(), refProps
                        );
                    }
                }
            }
            resourceResolver.commit();
            Resource componentResource = resourceResolver.getResource(targetRootPath);
            log.info("Target Root Path: '{}', resource: '{}'", targetRootPath, componentResource);
            if(componentResource == null) {
                Session session = resourceResolver.adaptTo(Session.class);
                if (session != null) {
                    final int eventTypes = Event.NODE_ADDED;
                    final boolean isDeep = false;
                    final boolean noLocal = true;
                    session.getWorkspace().getObservationManager().addEventListener(
                        this, eventTypes, targetRootPath,
                        isDeep, null, null, noLocal
                    );
                } else {
                    log.warn("Resource Resolver could not be adapted to Session");
                }
            } else {
                targetAvailable();
            }
        } catch (LoginException e) {
            log.error("2. Cannot Access Resource Resolver", e);
        } catch (PersistenceException e) {
            log.error("Failed to create Dynamic Component", e);
        } catch (UnsupportedRepositoryOperationException e) {
            log.error("Could not register Event Listener", e);
        } catch (RepositoryException e) {
            log.error("Could not get Observation Manager to register Event Listener", e);
        }
    }

    @Override
    public void onEvent(EventIterator events) {
        targetAvailable();
    }

    private void targetAvailable() {
        log.info("Target Available Called");
        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(
            new HashMap<String, Object>() {{ put(ResourceResolverFactory.SUBSERVICE, DYNAMIC_COMPONENTS_SERVICE_USER); }}
        )) {
            Resource root = resourceResolver.getResource(rootPath);
            if (root == null) {
                throw new IllegalArgumentException("Root Path: '" + rootPath + "' does not exist");
            }
            Resource target = root.getChild(DYNAMIC_COMPONENT_FOLDER_NAME);
            log.info("Update the Dynamic Component Resource Manager with Provider Path: '{}'", target);
            dynamicComponentResourceManager.update(target.getPath());
            log.info("Update the Dynamic Component Resource Manager done");

            // Now test the setup
            Resource button1 = resourceResolver.getResource(targetRootPath + "/" + "button1");
            log.info("Dynamic Button 1: '{}'", button1);
            Resource container1 = resourceResolver.getResource(targetRootPath + "/" + "container1");
            log.info("Dynamic Container 1: '{}'", container1);
            if(container1 != null) {
                Iterator<Resource> i = resourceResolver.listChildren(container1.getParent());
                int index = 0;
                while (i.hasNext()) {
                    log.info("{}. Entry: '{}'", index++, i.next());
                }
            }
        } catch (LoginException e) {
            log.error("2. Cannot Access Resource Resolver", e);
        }
    }

    private void addItemToListMap(Map<String, List<Property>> target, Property value) {
        String componentName = value.getComponent();
        List<Property> propertyList = target.get(componentName);
        if(propertyList == null) {
            propertyList = new ArrayList<>();
            target.put(componentName, propertyList);
        }
        propertyList.add(value);
    }

    private static class Property {
        private String component;
        private String name;
        private List<String> values = new ArrayList<>();

        public Property(String line, String messageTitle) {
            String[] split = line.split(EQUALS);
            if(split.length != 2) {
                throw new IllegalArgumentException(messageTitle + " is invalid (split on '" + EQUALS + "' does not yield 2 tokens): " + line);
            }
            String tempName = split[0];
            String tempValue = split[1];
            int index = tempValue.indexOf(VERTICAL_LINE);
            if( index > 0 && index < tempValue.length() - 1) {
                String[] splitTemp = tempValue.split("\\" + VERTICAL_LINE);
                this.component = tempName;
                this.name = splitTemp[0];
                tempValue = splitTemp[1];
                if(tempValue.charAt(0) == OPENING_MULTIPLE && tempValue.charAt(tempValue.length() - 1) == CLOSING_MULTIPLE) {
                    tempValue = tempValue.substring(1, tempValue.length() - 1);
                    splitTemp = tempValue.split(MULTI_SEPARATOR);
                    values.addAll(Arrays.asList(splitTemp));
                } else {
                    values.add(tempValue);
                }
            } else {
                this.name = tempName;
                if(tempValue.charAt(0) == OPENING_MULTIPLE && tempValue.charAt(tempValue.length() - 1) == CLOSING_MULTIPLE) {
                    tempValue = tempValue.substring(1, tempValue.length() - 1);
                    String[] splitTemp = tempValue.split(MULTI_SEPARATOR);
                    values.addAll(Arrays.asList(splitTemp));
                } else {
                    values.add(tempValue);
                }
            }
        }

        public boolean isComponent() { return component != null; }
        public boolean isEmpty() { return values.isEmpty(); }
        public boolean isSingle() { return values.size() == 1; }

        public String getComponent() {
            return component;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return values.isEmpty() ? null : values.get(0);
        }

        public List<String> getValues() {
            return new ArrayList<>(values);
        }

        @Override
        public String toString() {
            return "Property{" +
                "component-name='" + component + '\'' +
                ", name='" + name + '\'' +
                ", values=" + values +
                '}';
        }
    }
}