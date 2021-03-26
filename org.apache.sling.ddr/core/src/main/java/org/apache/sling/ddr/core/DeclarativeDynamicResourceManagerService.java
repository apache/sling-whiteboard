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
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.apache.sling.ddr.api.Constants.DYNAMIC_COMPONENTS_SERVICE_USER;
import static org.apache.sling.ddr.api.Constants.EQUALS;
import static org.apache.sling.ddr.api.Constants.JCR_PRIMARY_TYPE;
import static org.apache.sling.ddr.api.Constants.REP_POLICY;

@Component(
    service= DeclarativeDynamicResourceManager.class,
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = DeclarativeDynamicResourceManagerService.Configuration.class, factory = true)
public class DeclarativeDynamicResourceManagerService
    implements DeclarativeDynamicResourceManager
{
    @ObjectClassDefinition(
        name = "Declarative Dynamic Component Resource Provider",
        description = "Configuration of the Dynamic Component Resource Provider")
    public @interface Configuration {
        @AttributeDefinition(
            name = "Dynamic Component Target Path",
            description="Path to the Folder where the Dynamic Components will added to dynamically")
        String dynamic_component_target_path();
    }

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
    private String dynamicTargetPath;

    @Activate
    private void activate(BundleContext bundleContext, Configuration configuration) {
        log.info("Activate Started, bundle context: '{}'", bundleContext);
        this.bundleContext = bundleContext;
        dynamicTargetPath = configuration.dynamic_component_target_path();
        log.info("Dynamic Target Path: '{}'", dynamicTargetPath);
    }

    public void update(String dynamicProviderPath) {
        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(
            new HashMap<String, Object>() {{ put(ResourceResolverFactory.SUBSERVICE, DYNAMIC_COMPONENTS_SERVICE_USER); }}
        )) {
            Resource dynamicProvider = resourceResolver.getResource(dynamicProviderPath);
            Resource dynamicTarget = resourceResolver.getResource(dynamicTargetPath);
            log.info("Dynamic Resource Provider: '{}', Target: '{}'", dynamicProvider, dynamicTarget);
            if(dynamicProvider != null) {
                DeclarativeDynamicResourceProviderHandler service = new DeclarativeDynamicResourceProviderHandler();
                log.info("Dynamic Target: '{}', Dynamic Provider: '{}'", dynamicTarget, dynamicProvider);
                long id = service.registerService(bundleContext.getBundle(), dynamicTargetPath, dynamicProviderPath, resourceResolverFactory);
                log.info("After Registering Tenant RP: service: '{}', id: '{}'", service, id);
                registeredServices.put(dynamicTarget.getPath(), service);
                Iterator<Resource> i = dynamicProvider.listChildren();
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
                            dynamicTargetPath + '/' + componentName, provided
                        );
                    }
                }
            }
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
}

