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

import org.apache.sling.types.data.Property;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface WritableProperty<T extends Property> extends Property {

    @NotNull
    T withTitle(@NotNull String title);

    @NotNull
    default T withMultiple() {
        return withMultiple(true);
    }

    @NotNull
    T withMultiple(boolean multiple);

    @NotNull
    default T withReadonly() {
        return withReadonly(true);
    }

    @NotNull
    T withReadonly(boolean readonly);

    @NotNull
    default T withRequired() {
        return withRequired(true);
    }

    @NotNull
    T withRequired(boolean required);

    @NotNull
    T withValidations(String... validations);
}
