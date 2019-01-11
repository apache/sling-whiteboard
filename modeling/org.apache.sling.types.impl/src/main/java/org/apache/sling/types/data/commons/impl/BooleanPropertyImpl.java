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

import java.util.Arrays;
import java.util.Optional;

import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.types.TypeException;
import org.apache.sling.types.attributes.Attributes;
import org.apache.sling.types.attributes.commons.AttributesFactory;
import org.apache.sling.types.data.commons.BooleanProperty;
import org.apache.sling.types.data.commons.SimpleProperty;
import org.apache.sling.types.data.commons.SimplePropertyHandler;
import org.apache.sling.types.data.spi.PropertyHandler;
import org.apache.sling.types.data.validation.commons.Errors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

class BooleanPropertyImpl extends SimpleProperty<BooleanProperty> implements BooleanProperty {
    @NotNull
    private static final String TYPE = "sling:boolean";

    public BooleanPropertyImpl(@NotNull AttributesFactory attrsFactory, @NotNull String id, @NotNull String name) {
        super(attrsFactory, id, name, TYPE);
    }

    @SuppressWarnings("null")
    @Override
    @NotNull
    public Optional<String> getCheckedValue() {
        String value = attrs.get("checkedValue", String.class);
        return Optional.ofNullable(value);
    }

    @Override
    @NotNull
    public BooleanProperty withCheckedValue(@NotNull String checkedValue) {
        attrs.put("checkedValue", checkedValue);
        return this;
    }

    @Component(
        service = PropertyHandler.class,
        property = {
                PropertyHandler.PROPERTY_TYPE + "=" + TYPE
        }
    )
    public static class BooleanPropertyHandler extends SimplePropertyHandler<BooleanProperty> {
        @Reference
        private Errors errors;

        @Override
        protected Errors getErrors() {
            return errors;
        }

        @Override
        @Nullable
        protected Object getValue(@NotNull BooleanProperty property, @NotNull ValueMap vm) throws TypeException {
            Attributes attrs = property.getAttributes();
            String checkedValue = attrs.get("checkedValue", "true");
            String uncheckedValue = attrs.get("uncheckedValue", "false");

            String value = vm.get(property.getName(), String.class);

            if (value == null) {
                return null;
            }
            if (checkedValue.equals(value)) {
                return true;
            }
            if (uncheckedValue.equals(value)) {
                return false;
            }
            throw new TypeException("Mismatched value: " + value);
        }

        @Override
        protected Object[] convertParams(@NotNull BooleanProperty property, RequestParameter... params)
                throws TypeException {
            Attributes attrs = property.getAttributes();
            String checkedValue = attrs.get("checkedValue", "true");
            String uncheckedValue = attrs.get("uncheckedValue", "false");

            return Arrays.stream(params)
                .map(RequestParameter::getString)
                .map(value -> {
                    if (checkedValue.equals(value)) {
                        return true;
                    } else if (uncheckedValue.equals(value)) {
                        return false;
                    }
                    throw new TypeException("Mismatched value: " + value);
                })
                .toArray(Boolean[]::new);
        }
    }
}
