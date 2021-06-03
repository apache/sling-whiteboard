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

import org.apache.sling.sitemap.SitemapException;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

/**
 * A builder-like object that allows to add locations. For each added location an {@link Url} object is returned which
 * again allows to add more details to the added location.
 * <p>
 * Implementations may build an in-memory data structure when {@link Sitemap#addUrl(String)} is called, or write each
 * location on an underlying stream immediately.
 * </p>
 */
@ProviderType
public interface Sitemap {

    /**
     * Adds a location to the {@link Sitemap}. The returned {@link Url} can be used to add more details to the so
     * created object.
     *
     * @param location the required location of the entry to add.
     * @return an {@link Url} object giving access to the location's details
     * @throws SitemapException if any internal operation of the Sitemap fails
     */
    @NotNull
    Url addUrl(@NotNull String location) throws SitemapException;

}
