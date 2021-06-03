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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.sitemap.common.Externalizer;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

import java.util.Collection;

/**
 * A service that gives consumers access to minimal information about sitemaps.
 */
@ProviderType
public interface SitemapService {

    String PROPERTY_SITEMAP_ROOT = "sitemapRoot";

    /**
     * Returns the urls to the given {@link Resource}'s sitemaps, if any.
     * <p>
     * The returned urls may point to a sitemap index when there are multiple sitemaps generated for the given sitemap
     * root {@link Resource}. Or it may point to another sitemap root, if the sitemap is nested below a top level
     * sitemap root.
     * <p>
     * Numbers for size and entries can only be provided for sitemaps served from storage. For sitemap index or
     * on-demand sitemaps {@code -1} will be returned.
     * <p>
     * The default implementation uses {@link Externalizer#externalize(Resource)} to create absolute urls.
     *
     * @param sitemapRoot a {@link Resource} having {@link SitemapService#PROPERTY_SITEMAP_ROOT} set to true
     * @return the url, or null
     * @see SitemapService#isSitemapGenerationPending(Resource)
     */
    @NotNull
    Collection<SitemapInfo> getSitemapInfo(@NotNull Resource sitemapRoot);

    /**
     * Returns true when the background generator is still generating the any sitemap for the given {@link Resource}.
     * This may return always false for {@link Resource} which sitemaps are all served on-demand or if the url to the
     * {@link Resource}'s sitemap points to a sitemap index.
     *
     * @param sitemapRoot
     * @return
     */
    boolean isSitemapGenerationPending(@NotNull Resource sitemapRoot);

}
