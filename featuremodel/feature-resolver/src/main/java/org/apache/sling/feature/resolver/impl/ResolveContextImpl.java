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
package org.apache.sling.feature.resolver.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wiring;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolveContext;

/**
 * Implementation of the OSGi ResolveContext for use with the OSGi Resolver.
 */
public class ResolveContextImpl extends ResolveContext {
    private final Resource bundle;
    private final Collection<Resource> availableResources;

    /**
     * Constructor.
     * @param mainResource The main resource to resolve.
     * @param available The available resources to provide dependencies.
     */
    public ResolveContextImpl(Resource mainResource, Collection<Resource> available) {
        bundle = mainResource;
        availableResources = available;
    }

    @Override
    public Collection<Resource> getMandatoryResources() {
        return Collections.singleton(bundle);
    }

    @Override
    public List<Capability> findProviders(Requirement requirement) {
        List<Capability> providers = new ArrayList<>();

        String f = requirement.getDirectives().get("filter");
        try {
            Filter filter = FrameworkUtil.createFilter(f);
            for (Resource r : availableResources) {
                for (Capability c : r.getCapabilities(requirement.getNamespace())) {
                    if (filter.matches(c.getAttributes())) {
                        providers.add(c);
                    }
                }
            }
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException("Invalid filter " + f + " in requirement " + requirement);
        }

        return providers;
    }

    @Override
    public int insertHostedCapability(List<Capability> capabilities, HostedCapability hostedCapability) {
        capabilities.add(0, hostedCapability);
        return 0;
    }

    @Override
    public boolean isEffective(Requirement requirement) {
        String eff = requirement.getDirectives().get("effective");
        if (eff == null)
            return true; // resolve is the default
        return "resolve".equals(eff.trim());
    }

    @Override
    public Map<Resource, Wiring> getWirings() {
        return Collections.emptyMap();
    }
}
