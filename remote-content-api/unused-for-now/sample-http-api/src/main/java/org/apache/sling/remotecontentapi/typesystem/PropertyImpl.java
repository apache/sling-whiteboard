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

package org.apache.sling.remotecontentapi.typesystem;

import java.util.HashSet;
import java.util.Set;

import org.apache.sling.experimental.typesystem.Annotation;
import org.apache.sling.experimental.typesystem.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Temporary implementation for this prototype, until we
 *  have the actual type system
 */
class PropertyImpl<T> implements Property<T> {

    private final String name;
    private final T value;
    private final Set<Annotation> annotations = new HashSet<>();

    PropertyImpl(String name, T value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public @Nullable String getNamespace() {
        return null;
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public @NotNull Class<T> getType() {
        return (Class<T>)value.getClass();
    }

    @Override
    public @Nullable T getValue() {
        return value;
    }

    @Override
    public boolean isRequired() {
        throw new UnsupportedOperationException("do we really need this method");
    }

    @Override
    public @NotNull Set<Annotation> getAnnotations() {
        return annotations;
    }
}