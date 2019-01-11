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
package org.apache.sling.types.data;

import java.util.Optional;

import org.apache.sling.types.attributes.AttributesProvider;
import org.apache.sling.types.attributes.annotations.Attribute;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ConsumerType;

@ConsumerType
public interface Property extends AttributesProvider {

    @Attribute("sling:id")
    @NotNull
    default String getId() {
        String id = getAttributes().get("sling:id", String.class);

        if (id != null) {
            return id;
        } else {
            throw new IllegalStateException("The required `sling:id` attribute is not found");
        }
    }

    @Attribute("sling:name")
    @NotNull
    default String getName() {
        String name = getAttributes().get("sling:name", String.class);

        if (name != null) {
            return name;
        } else {
            throw new IllegalStateException("The required `sling:name` attribute is not found");
        }
    }

    @Attribute("sling:type")
    @NotNull
    default String getType() {
        String type = getAttributes().get("sling:type", String.class);

        if (type != null) {
            return type;
        } else {
            throw new IllegalStateException("The required `sling:type` attribute is not found");
        }
    }

    @SuppressWarnings("null")
    @Attribute("sling:title")
    @NotNull
    default Optional<String> getTitle() {
        return Optional.<String>ofNullable(getAttributes().get("sling:title", String.class));
    }

    @Attribute("sling:multiple")
    default boolean isMultiple() {
        return getAttributes().get("sling:multiple", false);
    }

    @Attribute("sling:readonly")
    default boolean isReadonly() {
        return getAttributes().get("sling:readonly", false);
    }

    @Attribute("sling:required")
    default boolean isRequired() {
        return getAttributes().get("sling:required", false);
    }

    @SuppressWarnings("null")
    @Attribute("sling:validations")
    @NotNull
    default String[] getValidations() {
        return getAttributes().get("sling:validations", new String[0]);
    }

    @Override
    boolean equals(Object o);

    @Override
    int hashCode();
}
