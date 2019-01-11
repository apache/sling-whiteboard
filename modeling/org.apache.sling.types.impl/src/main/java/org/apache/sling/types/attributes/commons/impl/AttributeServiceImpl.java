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
package org.apache.sling.types.attributes.commons.impl;

import java.util.Collection;
import java.util.Optional;

import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.types.TypeException;
import org.apache.sling.types.attributes.AttributeContext;
import org.apache.sling.types.attributes.AttributeDefinition;
import org.apache.sling.types.attributes.commons.AttributeService;
import org.apache.sling.types.attributes.spi.AttributeHandler;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class AttributeServiceImpl implements AttributeService {

    @Reference
    private volatile Collection<ServiceReference<AttributeHandler<AttributeDefinition>>> handlers;

    private BundleContext bundleContext;

    @SuppressWarnings("null")
    @Override
    @NotNull
    public Object process(@NotNull AttributeContext ctx, @NotNull AttributeDefinition def, @NotNull Object value)
            throws TypeException {
        return findAttributeHandler(def).map(h -> h.process(ctx, def, value)).orElse(value);
    }

    @SuppressWarnings("null")
    @NotNull
    private Optional<AttributeHandler<AttributeDefinition>> findAttributeHandler(@NotNull AttributeDefinition def) {
        return handlers.stream().filter(r -> {
            String type = PropertiesUtil.toString(r.getProperty(AttributeHandler.PROPERTY_TYPE), null);
            return def.getType().equals(type);
        }).findFirst().map(bundleContext::getService);
    }

    @Activate
    protected void activate(BundleContext ctx) {
        this.bundleContext = ctx;
    }
}
