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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractBundledRenderUnit implements Executable {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractBundledRenderUnit.class.getName());

    private final Bundle bundle;
    private final BundleContext bundleContext;
    private List<ServiceReference> references;
    private Map<String, Object> services;

    AbstractBundledRenderUnit(@NotNull Bundle bundle) {
        this.bundle = bundle;
        bundleContext = bundle.getBundleContext();
    }

    @Override
    @NotNull
    public Bundle getBundle() {
        return bundle;
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <ServiceType> ServiceType getService(@NotNull String className) {
        LOG.debug("Attempting to load class {} as an OSGi service.", className);
        ServiceType result = (this.services == null ? null : (ServiceType) this.services.get(className));
        if (result == null) {
            final ServiceReference ref = this.bundleContext.getServiceReference(className);
            if (ref != null) {
                result = (ServiceType) this.bundleContext.getService(ref);
                if (result != null) {
                    if (this.services == null) {
                        this.services = new HashMap<>();
                    }
                    if (this.references == null) {
                        this.references = new ArrayList<>();
                    }
                    this.references.add(ref);
                    this.services.put(className, result);
                    return result;
                }
            }
        }
        return null;
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <ServiceType> ServiceType[] getServices(@NotNull String className, @NotNull String filter) {
        ServiceType[] result = null;
        try {
            final ServiceReference[] refs = this.bundleContext.getServiceReferences(className, filter);

            if (refs != null) {
                // sort by service ranking (lowest first) (see ServiceReference#compareTo(Object))
                List<ServiceReference> references = Arrays.asList(refs);
                Collections.sort(references);
                // get the highest ranking first
                Collections.reverse(references);

                final List<ServiceType> objects = new ArrayList<>();
                for (ServiceReference reference : references) {
                    final ServiceType service = (ServiceType) this.bundleContext.getService(reference);
                    if (service != null) {
                        if (this.references == null) {
                            this.references = new ArrayList<>();
                        }
                        this.references.add(reference);
                        objects.add(service);
                    }
                }
                if (objects.size() > 0) {
                    ServiceType[] srv = (ServiceType[]) Array.newInstance(bundle.loadClass(className), objects.size());
                    result = objects.toArray(srv);
                }
            }
        } catch (Exception e) {
            LOG.error(String.format("Unable to retrieve the services of type %s.", className), e);
        }
        return result;
    }

    @Override
    public void releaseDependencies() {
        if (references != null) {
            for (ServiceReference reference : this.references) {
                bundleContext.ungetService(reference);
            }
            references.clear();
        }
        if (services != null) {
            services.clear();
        }
    }

}
