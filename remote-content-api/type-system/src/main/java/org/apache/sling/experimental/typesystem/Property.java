/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Licensed to the Apache Software Foundation (ASF) under one
 ~ or more contributor license agreements.  See the NOTICE file
 ~ distributed with this work for additional information
 ~ regarding copyright ownership.  The ASF licenses this file
 ~ to you under the Apache License, Version 2.0 (the
 ~ "License"); you may not use this file except in compliance
 ~ with the License.  You may obtain a copy of the License at
 ~
 ~   http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package org.apache.sling.experimental.typesystem;

import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The {@code Property} class encapsulates the information regarding {@link Type}s properties, defining their names, types, optionality,
 * etc.
 *
 * @param <T> the type of the property
 */
@ProviderType
public interface Property<T> {

    /**
     * If a property is namespaced, this method will return the corresponding namespace.
     *
     * @return the property's namespace or {@code null}
     */
    @Nullable
    String getNamespace();

    /**
     * Returns the property's name.
     *
     * @return the property's name
     */
    @NotNull
    String getName();

    /**
     * Returns the type of the property.
     *
     * @return the type of the property
     */
    @NotNull
    Class<T> getType();

    /**
     * Returns the value of this property.
     *
     * @return the value of this property of {@code null}
     */
    @Nullable
    T getValue();

    /**
     * Returns {@code true} if this is a required property, {@code false} otherwise.
     *
     * @return {@code true} if this is a required property, {@code false} otherwise
     */
    boolean isRequired();

    /**
     * Returns the set of annotations {@code this} {@code Property} has.
     *
     * @return the set of annotations
     */
    @NotNull
    Set<Annotation> getAnnotations();
}
