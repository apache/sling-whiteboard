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
package org.apache.sling.sitemap.builder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

import java.time.Instant;

/**
 * A builder-like object that allows to add details to a location added to a {@link Sitemap}.
 */
@ProviderType
public interface Url {

    enum ChangeFrequency {
        ALWAYS, HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY, NEVER
    }

    /**
     * Sets the change frequency of the url.
     *
     * @param changeFrequency
     * @return
     */
    @NotNull
    Url setChangeFrequency(@NotNull ChangeFrequency changeFrequency);

    /**
     * Sets the last modified time of the url.
     *
     * @param pointInTime
     * @return
     */
    @NotNull
    Url setLastModified(@NotNull Instant pointInTime);

    /**
     * Sets the priority of the url. According to the sitemap protocol the priority must be a number between 0.0 and
     * 1.0. Values smaller or greater will be corrected to the lower and upper bound respectively.
     *
     * @param priority
     * @return
     */
    @NotNull
    Url setPriority(double priority);

    /**
     * Adds an extension to the url.
     *
     * @param extensionInterface the interface of the extension to add
     * @param <T>                the type of the extension
     * @return an instance of the given interface, or null when no
     * {@link org.apache.sling.sitemap.builder.extensions.ExtensionProvider} is registered for the given interface
     */
    @Nullable <T extends Extension> T addExtension(Class<T> extensionInterface);

}
