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
import org.apache.sling.sitemap.SitemapException;
import org.apache.sling.sitemap.builder.Sitemap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ConsumerType;

import java.util.Collections;
import java.util.Set;

/**
 * Implementations are responsible to generate one or many sitemaps for a given sitemap root {@link Resource}.
 * When a {@link SitemapGenerator} generates multiple sitemaps for a given {@link Resource} it has to return
 * their names using {@link SitemapGenerator#getNames(Resource)} (Resource)}.
 * <p>
 * {@link SitemapGenerator#generate(Resource, String, Sitemap, GenerationContext)} may be called for each name and
 * each sitemap root {@link Resource}, the {@link SitemapGenerator} returned an non-empty {@link Set} of names for.
 * <p>
 * It is possible to register multiple {@link SitemapGenerator}s for a single name. In this case the one with the
 * highest ranking according to the OSGI specification is used.
 */
@ConsumerType
public interface SitemapGenerator {

    String DEFAULT_SITEMAP = "<default>";

    /**
     * The background generation will send events with that topic right after a generated sitemap was persisted.
     */
    String EVENT_TOPIC_SITEMAP_UPDATED = "org/apache/sling/sitemap/UPDATED";
    /**
     * The background cleanup will send events with that topic right after an obsolete sitemap file was purged.
     */
    String EVENT_TOPIC_SITEMAP_PURGED = "org/apache/sling/sitemap/PURGED";
    /**
     * The event property storing the generated sitemap's root path.
     */
    String EVENT_PROPERTY_SITEMAP_ROOT = "sitemap.root";
    /**
     * The event property storing the generated sitemap's name.
     */
    String EVENT_PROPERTY_SITEMAP_NAME = "sitemap.name";
    /**
     * The event property storing the generated sitemap's count of urls.
     */
    String EVENT_PROPERTY_SITEMAP_URLS = "sitemap.urls";
    /**
     * The event property storing a flag whether the generated sitemap exceeds the configured limits.
     */
    String EVENT_PROPERTY_SITEMAP_EXCEEDS_LIMITS = "sitemap.exceedsLimits";
    /**
     * The event property storing the generated sitemap's storage path.
     */
    String EVENT_PROPERTY_SITEMAP_STORAGE_PATH = "sitemap.storagePath";
    /**
     * The event property storing the generated sitemap's binary size.
     */
    String EVENT_PROPERTY_SITEMAP_STORAGE_SIZE = "sitemap.storageSize";

    /**
     * Returns a {@link Set} of sitemap names this {@link SitemapGenerator} can generate for a particular sitemap
     * root {@link Resource}. If the implementation does not want to generate a sitemap for a particular root it must
     * return an empty {@link Set}, if it does but does not differentiate by name, it must return a {@link Set}
     * containing only the {@link SitemapGenerator#DEFAULT_SITEMAP}.
     * <p>
     * The character '@' (U+0040) is reserved. Names contain with int will be filtered out.
     * <p>
     * The default implementation returns a {@link Set} of only {@link SitemapGenerator#DEFAULT_SITEMAP}.
     *
     * @return a {@link Set} of names
     */
    @NotNull
    default Set<String> getNames(@NotNull Resource sitemapRoot) {
        return Collections.singleton(SitemapGenerator.DEFAULT_SITEMAP);
    }

    /**
     * Generates a {@link Sitemap} with the given name at the given {@link Resource}.
     * <p>
     * This process may be stateful and the given {@link GenerationContext} can be used to keep track of the state. For
     * example a traversal that keeps track on the last {@link Resource} added to the {@link Sitemap}.
     *
     * @param sitemapRoot the root at which the sitemap should be created
     * @param name        the name, one of the names returned by {@link SitemapGenerator#getNames(Resource)} for the given
     *                    sitemapRoot
     * @param sitemap     the {@link Sitemap} object to add locations to
     * @param context     the context under which the sitemap is generated
     * @throws SitemapException may be thrown in unrecoverable exceptional cases
     */
    void generate(@NotNull Resource sitemapRoot, @NotNull String name, @NotNull Sitemap sitemap,
                  @NotNull GenerationContext context) throws SitemapException;

    /**
     * A context object that gives the {@link SitemapGenerator} access to additional configurations and methods to
     * track state.
     */
    interface GenerationContext {

        @Nullable <T> T getProperty(@NotNull String name, @NotNull Class<T> cls);

        @NotNull <T> T getProperty(@NotNull String name, @NotNull T defaultValue);

        void setProperty(@NotNull String name, @Nullable Object data);

    }
}
