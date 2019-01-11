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
package org.apache.sling.types.data.commons.impl;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.types.TypeException;
import org.apache.sling.types.data.Property;
import org.apache.sling.types.data.commons.PropertyService;
import org.apache.sling.types.data.spi.PropertyHandler;
import org.apache.sling.types.data.validation.ValidationError;
import org.apache.sling.types.spi.Context;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class PropertyServiceImpl implements PropertyService {

	@Reference
    private volatile Collection<ServiceReference<PropertyHandler<Property>>> handlers;

    private BundleContext bundleContext;

    @Activate
    protected void activate(BundleContext ctx) {
        this.bundleContext = ctx;
    }

    @Override
    @NotNull
    public Optional<?> getValue(@NotNull Context<Resource> ctx, @NotNull Property prop) throws TypeException {
        return findPropertyHandler(prop)
            .orElseThrow(() -> new TypeException("PropertyHandler cannot be found: " + prop.getType()))
            .getValue(ctx, prop);
    }

    @Override
    @NotNull
    public List<@NotNull ValidationError> setValue(@NotNull Context<Resource> ctx, @NotNull Property prop,
            RequestParameter... params) throws TypeException {
        PropertyHandler<Property> handler = findPropertyHandler(prop)
            .orElseThrow(() -> new TypeException("PropertyHandler cannot be found: " + prop.getType()));

        List<@NotNull ValidationError> errors = handler.validate(ctx, prop, params);

        if (errors.isEmpty()) {
            handler.setValue(ctx, prop, params);
        }

        return errors;
    }

    @SuppressWarnings("null")
	@NotNull
    private Optional<PropertyHandler<Property>> findPropertyHandler(@NotNull Property property) {
        return handlers.stream()
            .filter(r -> {
                String type = PropertiesUtil.toString(r.getProperty(PropertyHandler.PROPERTY_TYPE), null);
                return property.getType().equals(type);
            })
            .findFirst()
            .map(bundleContext::getService);
    }
}
