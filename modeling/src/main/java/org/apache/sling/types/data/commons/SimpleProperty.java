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
package org.apache.sling.types.data.commons;

import org.apache.sling.types.attributes.Attributes;
import org.apache.sling.types.attributes.commons.AttributesFactory;
import org.apache.sling.types.attributes.commons.WritableAttributes;
import org.apache.sling.types.data.Property;
import org.jetbrains.annotations.NotNull;

public class SimpleProperty<T extends Property> implements WritableProperty<T> {

    @SuppressWarnings("rawtypes")
    @NotNull
    protected WritableAttributes<? extends SimpleProperty> attrs;

    public SimpleProperty(@NotNull AttributesFactory attrsFactory, @NotNull String id, @NotNull String name,
            @NotNull String type) {
        this.attrs = attrsFactory.getWritable(getClass());

        attrs.put("sling:id", id);
        attrs.put("sling:name", name);
        attrs.put("sling:type", type);
    }

    @SuppressWarnings("unchecked")
    @Override
    @NotNull
    public T withTitle(@NotNull String title) {
        attrs.put("sling:title", title);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    @NotNull
    public T withRequired(boolean required) {
        attrs.put("sling:required", required);
        return (T) this;
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    @NotNull
    public T withValidations(String... validations) {
        attrs.put("sling:validations", validations);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    @NotNull
    public T withMultiple(boolean multiple) {
        attrs.put("sling:multiple", multiple);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    @NotNull
    public T withReadonly(boolean readonly) {
        attrs.put("sling:readonly", readonly);
        return (T) this;
    }

    @Override
    @NotNull
    public Attributes getAttributes() {
        return attrs;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Property) {
            return ((Property) o).getId().equals(getId());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
