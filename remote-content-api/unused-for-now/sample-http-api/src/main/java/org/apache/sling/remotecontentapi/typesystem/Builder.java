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
import org.apache.sling.experimental.typesystem.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Temporary implementation for this prototype, until we
 *  have the actual type system
 */

class Builder {
    private final String resourceType;
    private final Set<Property> properties = new HashSet<>();
    private final Set<Annotation> annotations = new HashSet<>();

    private Builder(String resourceType) {
        this.resourceType = resourceType;
    }

    static Builder forResourceType(String resourceType) {
        return new Builder(resourceType);
    }

    Builder withAnnotation(String name, String value) {
        annotations.add(new AnnotationImpl(name, value));
        return this;
    }

    Type build() {
        return new Type() {

            @Override
            public @NotNull String getResourceType() {
                return resourceType;
            }

            @Override
            public @Nullable String getResourceSuperType() {
                return null;
            }

            @Override
            public @NotNull Set<Property> getProperties() {
                return properties;
            }

            @Override
            public @NotNull Set<Annotation> getAnnotations() {
                return annotations;
            }
        };
    }
}