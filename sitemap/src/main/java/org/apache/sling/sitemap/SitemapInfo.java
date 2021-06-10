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
package org.apache.sling.sitemap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

/**
 * An info object with details about a sitemap/sitemap-index.
 */
@ProviderType
public interface SitemapInfo {

    String SITEMAP_INDEX_NAME = "<sitemap-index>";

    /**
     * Returns a resource path to the node the sitemap is stored. May return null if the sitemap or sitemap-index is
     * served on-demand.
     *
     * @return
     */
    @Nullable
    String getStoragePath();

    /**
     * Returns the absolute, external url for the sitemap/sitemap-index.
     *
     * @return
     */
    @NotNull
    String getUrl();

    /**
     * Returns the name of the sitemap. This returns null when the sitemap has the default name or it is a sitemap
     * index.
     *
     * @return
     */
    @Nullable
    String getName();

    /**
     * Returns the size of the sitemap in bytes. -1 for sitemap-index or sitemaps served on-demand.
     *
     * @return
     */
    int getSize();

    /**
     * Returns the number of urls in the sitemap. -1 for sitemap-index or sitemaps served on-demand.
     *
     * @return
     */
    int getEntries();

    /**
     * Returns true when the url is pointing to a sitemap-index.
     *
     * @return
     */
    boolean isIndex();

    /**
     * Returns true if both size and entries are within the configured limits.
     *
     * @return
     */
    boolean isWithinLimits();
}
