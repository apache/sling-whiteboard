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
package org.apache.sling.modeling.attributes.commons.impl;

import org.apache.sling.modeling.attributes.AttributeDefinition;
import org.jetbrains.annotations.NotNull;

class SimpleDefinition<T extends AttributeDefinition> implements AttributeDefinition {

    private @NotNull String name;
    private @NotNull String type;
    private @NotNull Class<?> typeClass;
    private boolean multiple;
    private boolean required;

    public SimpleDefinition(@NotNull String name, @NotNull String type, @NotNull Class<?> typeClass) {
        this.name = name;
        this.type = type;
        this.typeClass = typeClass;
    }

    @Override
    @NotNull
    public String getName() {
        return this.name;
    }

    @Override
    @NotNull
    public String getType() {
        return type;
    }

    @Override
    @NotNull
    public Class<?> getTypeClass() {
        return typeClass;
    }

    @Override
    public boolean isMultiple() {
        return multiple;
    }

    @Override
    public boolean isRequired() {
        return required;
    }

    @NotNull
    public T withMultiple() {
        return withMultiple(true);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public T withMultiple(boolean multiple) {
        this.multiple = multiple;
        return (T) this;
    }

    @NotNull
    public T withRequired() {
        return withRequired(true);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public T withRequired(boolean required) {
        this.required = required;
        return (T) this;
    }
}
