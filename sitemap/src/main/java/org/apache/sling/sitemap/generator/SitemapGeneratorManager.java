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
package org.apache.sling.sitemap.generator;

import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * A service that manages all registered {@link SitemapGenerator} services.
 */
@ProviderType
public interface SitemapGeneratorManager {

    /**
     * Returns all names of all {@link SitemapGenerator}s for the given sitemap root.
     *
     * @param sitemapRoot
     * @return
     */
    Set<String> getNames(@NotNull Resource sitemapRoot);

    /**
     * Returns all names of all {@link SitemapGenerator}s for the given sitemap root, limited to those that are
     * configured to be served on demand.
     *
     * @param sitemapRoot
     * @return
     */
    @NotNull
    Set<String> getOnDemandNames(@NotNull Resource sitemapRoot);

    /**
     * Returns the {@link SitemapGenerator} for the given sitemap root {@link Resource} and name. This may be null
     * when no {@link SitemapGenerator} service exists that generates a sitemap with the name for the given root.
     *
     * @param sitemapRoot
     * @param name
     * @return
     */
    @Nullable
    SitemapGenerator getGenerator(@NotNull Resource sitemapRoot, @NotNull String name);

    /**
     * Returns a {@link Map} of {@link SitemapGenerator}s for each name returned by
     * {@link SitemapGeneratorManager#getNames(Resource)}.
     *
     * @param sitemapRoot
     * @return
     */
    @NotNull
    Map<String, SitemapGenerator> getGenerators(@NotNull Resource sitemapRoot);

}
