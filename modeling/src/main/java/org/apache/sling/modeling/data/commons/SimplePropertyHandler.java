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
package org.apache.sling.modeling.data.commons;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.modeling.ModelException;
import org.apache.sling.modeling.data.Property;
import org.apache.sling.modeling.data.spi.PropertyHandler;
import org.apache.sling.modeling.data.validation.ValidationError;
import org.apache.sling.modeling.data.validation.commons.Errors;
import org.apache.sling.modeling.spi.Context;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SimplePropertyHandler<T extends Property> implements PropertyHandler<T> {
    private static final Logger log = LoggerFactory.getLogger(SimplePropertyHandler.class);

    @SuppressWarnings("null")
    @Override
    @NotNull
    public Optional<?> getValue(@NotNull Context ctx, @NotNull T property) throws ModelException {
        String name = property.getName();

        log.debug("Getting value with name: " + name);

        ValueMap vm = ctx.getResource().getValueMap();
        return Optional.ofNullable(getValue(property, vm));
    }

    @Nullable
    protected abstract Object getValue(@NotNull T property, @NotNull ValueMap vm) throws ModelException;

    @Override
    public void setValue(@NotNull Context ctx, @NotNull T property, RequestParameter... params) throws ModelException {
        String name = property.getName();

        log.debug("Setting value with name: " + name);

        if (property.isReadonly()) {
            return;
        }

        // name must be normalized relative path
        String normalizedName = ResourceUtil.normalize(name);
        if (normalizedName == null) {
            throw new AssertionError("Illegal property name: " + name);
        }
        assert !normalizedName.startsWith("/");
        assert !normalizedName.endsWith("/");

        String relPath = ResourceUtil.getParent(normalizedName);
        String propName = ResourceUtil.getName(normalizedName);

        Resource parent = ctx.getResource();
        if (relPath != null) {
            String parentPath = ctx.getResource().getPath() + "/" + relPath;
            ResourceResolver resolver = ctx.getResource().getResourceResolver();
            try {
                parent = ResourceUtil.getOrCreateResource(resolver, parentPath, (String) null, null, false);
            } catch (PersistenceException e) {
                throw new ModelException("Error occurs creating resource: " + parentPath, e);
            }
        }

        ModifiableValueMap vm = parent.adaptTo(ModifiableValueMap.class);

        if (vm == null) {
            throw new ModelException("The resource is not adaptable to ModifiableValueMap: " + parent.getPath());
        }

        Object[] values = convertParams(property, params);

        if (property.isMultiple()) {
            vm.put(propName, values);
        } else {
            vm.put(propName, values[0]);
        }
    }

    @SuppressWarnings("null")
    @Override
    @NotNull
    public List<@NotNull ValidationError> validate(@NotNull Context ctx, @NotNull T property,
            RequestParameter... params) throws ModelException {
        if (property.isReadonly()) {
            return Collections.emptyList();
        }

        if (property.isRequired()) {
            if (params.length == 0 || params[0].getSize() == 0) {
                return Arrays.asList(getErrors().required(property, params));
            }
        }

        return Collections.emptyList();
    }

    protected abstract Object[] convertParams(@NotNull T property, RequestParameter... params) throws ModelException;

    protected abstract Errors getErrors();
}
