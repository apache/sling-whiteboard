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

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.sitemap.SitemapException;
import org.apache.sling.sitemap.builder.Sitemap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ConsumerType;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.apache.sling.sitemap.common.SitemapUtil.isSitemapRoot;

/**
 * A default implementation of {@link SitemapGenerator} that traverses a resource tree.
 * <p>
 * Implementations may change the traversal behaviour by overriding
 * {@link ResourceTreeSitemapGenerator#shouldFollow(Resource)} or
 * {@link ResourceTreeSitemapGenerator#shouldInclude(Resource)} but it is recommended to always consider the default
 * implementation. The default implementation includes only {@link Resource}s that have a "jcr:content" child and
 * follows through only on content that is not below the "jcr:content" or any other sitemap root.
 * <p>
 * This implementation keeps track of the traversal's state in the
 * {@link SitemapGenerator.GenerationContext}. It is capable to continue from a previous
 * persisted state, when the generation got aborted.
 */
@ConsumerType
public abstract class ResourceTreeSitemapGenerator implements SitemapGenerator {

    protected static final String PROPERTY_LAST_PATH = "lastPath";

    @Override
    public final void generate(@NotNull Resource sitemapRoot, @NotNull String name, @NotNull Sitemap sitemap,
                               @NotNull GenerationContext context) throws SitemapException {
        String lastPath = context.getProperty(PROPERTY_LAST_PATH, String.class);
        for (Resource descendant : (Iterable<? extends Resource>) traverse(sitemapRoot, lastPath)::iterator) {
            addResource(name, sitemap, descendant);
            context.setProperty(PROPERTY_LAST_PATH, descendant.getPath());
        }
    }

    /**
     * Implementations add the given {@link Resource} to the given {@link Sitemap}.
     *
     * @param name the name of the sitemap currently being generated
     * @param sitemap
     * @param resource
     * @throws SitemapException
     */
    protected abstract void addResource(@NotNull String name, @NotNull Sitemap sitemap, @NotNull Resource resource)
            throws SitemapException;

    /**
     * When implementations return true, the given resource will be passed to
     * {@link ResourceTreeSitemapGenerator#addResource(String, Sitemap, Resource)} to be added to the {@link Sitemap}.
     *
     * @param resource
     * @return
     */
    protected boolean shouldInclude(@NotNull Resource resource) {
        return resource.getChild(JcrConstants.JCR_CONTENT) != null;
    }

    /**
     * When implementations return true, the children of the given {@link Resource} will be followed through by the
     * traversal.
     *
     * @param resource
     * @return
     */
    protected boolean shouldFollow(@NotNull Resource resource) {
        return !JcrConstants.JCR_CONTENT.equals(resource.getName()) && !isSitemapRoot(resource);
    }

    private Stream<Resource> traverse(@NotNull Resource sitemapRoot, @Nullable String skipTo) {
        Stream<Resource> children = StreamSupport.stream(sitemapRoot.getChildren().spliterator(), false)
                .filter(this::shouldFollow);

        if (skipTo != null) {
            AtomicBoolean found = new AtomicBoolean(false);
            // advance children until skipTo starts either with the child's path or it is equal to it
            return children.flatMap(child -> {
                if (found.get()) {
                    return traverse(child, null);
                } else if (skipTo.equals(child.getPath())) {
                    found.set(true);
                    return traverse(child, null);
                } else if (skipTo.startsWith(child.getPath() + '/')) {
                    found.set(true);
                    return traverse(child, skipTo);
                } else {
                    return Stream.empty();
                }
            });
        } else {
            return Stream.concat(
                    shouldInclude(sitemapRoot) ? Stream.of(sitemapRoot) : Stream.empty(),
                    children.flatMap(child -> traverse(child, null))
            );
        }
    }
}
