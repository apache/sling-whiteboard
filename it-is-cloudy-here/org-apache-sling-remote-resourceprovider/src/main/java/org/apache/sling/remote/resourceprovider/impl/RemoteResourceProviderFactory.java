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
package org.apache.sling.remote.resourceprovider.impl;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.contentparser.api.ContentParser;
import org.apache.sling.remote.resourceprovider.RemoteStorageProvider;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;

@Component(immediate = true)
@Designate(ocd = RemoteResourceProviderFactoryConfiguration.class)
public class RemoteResourceProviderFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteResourceProviderFactory.class);

    private BundleContext bundleContext;
    private ConcurrentHashMap<String, RegistrationMapping> mappings = new ConcurrentHashMap<>();
    private RemoteResourceProviderFactoryConfiguration configuration;
    private static final Set<String> RESOURCE_PROVIDER_ACCEPTED_AUTH_VALUES =
            Set.of(ResourceProvider.AUTHENTICATE_NO, ResourceProvider.AUTHENTICATE_LAZY, ResourceProvider.AUTHENTICATE_REQUIRED);

    @Reference(target = "(" + ContentParser.SERVICE_PROPERTY_CONTENT_TYPE + "=json)")
    private ContentParser jsonParser;

    @Reference
    private ThreadPoolManager threadPoolManager;

    @Reference(cardinality = MULTIPLE,
               policy = DYNAMIC)
    private synchronized void bindRemoteStorageProvider(RemoteStorageProvider remoteStorageProvider, Map<String, Object> properties) {
        String resourceProviderRoot = (String) properties.get(RemoteStorageProvider.PROP_RESOURCE_PROVIDER_ROOT);
        if (StringUtils.isNotEmpty(resourceProviderRoot)) {
            if (!mappings.containsKey(resourceProviderRoot)) {
                Hashtable<String, Object> resourceProviderRegistrationProperties = new Hashtable<>();
                String providerAuthentication = (String) properties.get(RemoteStorageProvider.PROP_RESOURCE_PROVIDER_AUTHENTICATE);
                if (RESOURCE_PROVIDER_ACCEPTED_AUTH_VALUES.contains(providerAuthentication)) {
                    resourceProviderRegistrationProperties.put(ResourceProvider.PROPERTY_ROOT, resourceProviderRoot);
                    resourceProviderRegistrationProperties.put(ResourceProvider.PROPERTY_AUTHENTICATE, providerAuthentication);
                    resourceProviderRegistrationProperties.put(ResourceProvider.PROPERTY_NAME, remoteStorageProvider.getClass().getName());
                    mappings.put(resourceProviderRoot, new RegistrationMapping(remoteStorageProvider,
                            resourceProviderRegistrationProperties));
                } else {
                    LOGGER.warn("Invalid value for {}. Accepted values: {}.", RemoteStorageProvider.PROP_RESOURCE_PROVIDER_AUTHENTICATE,
                            RESOURCE_PROVIDER_ACCEPTED_AUTH_VALUES);
                }
            } else {
                LOGGER.warn("Cannot replace existing mapping for {}.", resourceProviderRoot);
            }
        } else {
            LOGGER.warn("Missing value for {}.", RemoteStorageProvider.PROP_RESOURCE_PROVIDER_ROOT);
        }
        if (bundleContext != null) {
            for (RegistrationMapping mapping : mappings.values()) {
                mapping.registerResourceProviderIfNeeded(bundleContext);
            }
        }
    }

    private void unbindRemoteStorageProvider(RemoteStorageProvider remoteStorageProvider, Map<String, Object> properties) {
        String resourceProviderRoot = (String) properties.get(RemoteStorageProvider.PROP_RESOURCE_PROVIDER_ROOT);
        if (StringUtils.isNotEmpty(resourceProviderRoot)) {
            RegistrationMapping mapping = mappings.get(resourceProviderRoot);
            if (mapping != null && mapping.remoteStorageProvider.equals(remoteStorageProvider)) {
                mappings.remove(resourceProviderRoot);
                mapping.unregisterResourceProvider();
            }
        }
    }

    @Activate
    private void activate(RemoteResourceProviderFactoryConfiguration configuration, ComponentContext componentContext) {
        this.configuration = configuration;
        bundleContext = componentContext.getBundleContext();
        for (RegistrationMapping mapping : mappings.values()) {
            mapping.registerResourceProviderIfNeeded(bundleContext);
        }
    }

    @Deactivate
    private void deactivate() {
        for (RegistrationMapping mapping : mappings.values()) {
            mapping.resourceProviderServiceRegistration.unregister();
        }
        mappings.clear();
    }

    private class RegistrationMapping {
        private final RemoteStorageProvider remoteStorageProvider;
        private final Hashtable<String, Object> resourceProviderRegistrationProperties;
        private ServiceRegistration<?> resourceProviderServiceRegistration;
        private RemoteResourceProvider resourceProvider;

        RegistrationMapping(RemoteStorageProvider remoteStorageProvider,
                            Hashtable<String, Object> resourceProviderRegistrationProperties) {
            this.remoteStorageProvider = remoteStorageProvider;
            this.resourceProviderRegistrationProperties = resourceProviderRegistrationProperties;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof RegistrationMapping) {
                RegistrationMapping other = (RegistrationMapping) obj;
                return remoteStorageProvider.equals(other.remoteStorageProvider);
            }
            return false;
        }

        void registerResourceProviderIfNeeded(BundleContext bundleContext) {
            if (resourceProviderServiceRegistration == null) {
                int cacheSize = configuration.cacheSize() >= 100 ? configuration.cacheSize() : 0;
                int lastAccessedExpirationTime = configuration.lastAccessedExpirationTime() >= 0 ?
                        configuration.lastAccessedExpirationTime() : 0;
                resourceProvider = new RemoteResourceProvider(threadPoolManager, jsonParser, new InMemoryResourceCache(cacheSize,
                        lastAccessedExpirationTime),
                        remoteStorageProvider, !ResourceProvider.AUTHENTICATE_NO
                        .equals(resourceProviderRegistrationProperties.get(ResourceProvider.PROPERTY_AUTHENTICATE)));
                resourceProviderServiceRegistration = bundleContext.registerService(ResourceProvider.class,
                        resourceProvider,
                        resourceProviderRegistrationProperties);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Registered a Resource Provider for: {}.", resourceProviderRegistrationProperties);
                }
            }
        }

        void unregisterResourceProvider() {
            if (resourceProviderServiceRegistration != null) {
                try {
                    resourceProvider.cleanup();
                    resourceProviderServiceRegistration.unregister();
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("The resource provider with the following properties was unregistered: {}",
                                resourceProviderRegistrationProperties);
                    }
                } catch (IllegalStateException e) {
                    LOGGER.warn("The resource provider with the following properties was unregistered before: {}.",
                            resourceProviderRegistrationProperties);
                }
            }
        }

        @Override
        public int hashCode() {
            return remoteStorageProvider.hashCode();
        }
    }

}
