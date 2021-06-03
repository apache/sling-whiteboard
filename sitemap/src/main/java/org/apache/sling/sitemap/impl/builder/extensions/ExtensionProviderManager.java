/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.sitemap.impl.builder.extensions;

import org.apache.sling.sitemap.builder.Extension;
import org.apache.sling.sitemap.builder.extensions.ExtensionProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Component(
        service = ExtensionProviderManager.class,
        reference = {
                @Reference(
                        service = ExtensionProvider.class,
                        name = "providers",
                        bind = "bindExtensionProvider",
                        unbind = "unbindExtensionProvider",
                        cardinality = ReferenceCardinality.OPTIONAL,
                        policyOption = ReferencePolicyOption.GREEDY
                )
        })
public class ExtensionProviderManager {

    private static final Logger LOG = LoggerFactory.getLogger(ExtensionProviderManager.class);

    private final Map<ServiceReference<?>, Holder> providers = new TreeMap<>(Collections.reverseOrder());
    private Map<String, String> namespaces;
    private BundleContext bundleContext;

    @Activate
    protected void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    protected void bindExtensionProvider(ServiceReference<ExtensionProvider> ref) {
        try {
            namespaces = null;
            providers.put(ref, new Holder(ref));
        } catch (ClassCastException ex) {
            LOG.warn("Failed to register ExtensionProvider '{}' as on of the mandatory properties if not of type String.", ref, ex);
        }
    }

    protected void unbindExtensionProvider(ServiceReference<ExtensionProvider> ref) {
        Holder holder = providers.remove(ref);

        if (holder != null && holder.provider != null) {
            bundleContext.ungetService(ref);
        }
    }

    /**
     * Returns an unique mapping from namespace to prefix.
     *
     * @return
     */
    @NotNull
    public Map<String, String> getNamespaces() {
        if (namespaces == null) {
            namespaces = new HashMap<>();
            for (Holder holder : providers.values()) {
                namespaces.putIfAbsent(holder.namespace, holder.prefix);
            }
        }

        return namespaces;
    }

    @Nullable
    public ExtensionFactory getExtensionFactory(Class<? extends Extension> extensionInterface) {
        for (Holder holder : providers.values()) {
            if (holder.extensionInterface.equals(extensionInterface.getName())) {
                // get the right prefix for the namespace. this may be different then the holder's prefix as there may
                // be many providers for one namespace defining different prefixes. the one with the highest service
                // ranking wins.
                return new ExtensionFactory(holder.getProvider(), holder.namespace,
                        getNamespaces().get(holder.namespace), holder.localName, holder.emptyTag);
            }
        }
        return null;
    }

    private class Holder {
        private final ServiceReference<ExtensionProvider> ref;
        private final String extensionInterface;
        private final String prefix;
        private final String namespace;
        private final String localName;
        private final boolean emptyTag;

        private ExtensionProvider provider;

        private Holder(ServiceReference<ExtensionProvider> ref) {
            this.ref = ref;
            prefix = Objects
                    .requireNonNull((String) ref.getProperty(ExtensionProvider.PROPERTY_PREFIX), "prefix missing");
            namespace = Objects
                    .requireNonNull((String) ref.getProperty(ExtensionProvider.PROPERTY_NAMESPACE), "namespace missing");
            localName = Objects
                    .requireNonNull((String) ref.getProperty(ExtensionProvider.PROPERTY_LOCAL_NAME), "local name missing");
            extensionInterface = Objects
                    .requireNonNull((String) ref.getProperty(ExtensionProvider.PROPERTY_INTERFACE), "prefix missing");

            Object emptyTagProp = ref.getProperty(ExtensionProvider.PROPERTY_EMPTY_TAG);

            if (emptyTagProp instanceof Boolean) {
                emptyTag = (Boolean) emptyTagProp;
            } else if (emptyTagProp instanceof String) {
                emptyTag = Boolean.parseBoolean((String) emptyTagProp);
            } else {
                emptyTag = false;
                LOG.debug("Unknown type for emptyTag: " + emptyTagProp);
            }
        }

        private ExtensionProvider getProvider() {
            if (provider == null) {
                provider = bundleContext.getService(ref);
            }
            return provider;
        }
    }
}
