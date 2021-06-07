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
package org.apache.sling.sitemap.impl;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.sitemap.SitemapService;
import org.apache.sling.sitemap.generator.SitemapGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.jcr.query.Query;
import java.util.*;

public class SitemapUtil {

    private static final String JCR_SYSTEM_PATH = "/" + JcrConstants.JCR_SYSTEM + "/";

    private SitemapUtil() {
        super();
    }

    /**
     * Returns the {@link Resource} marked as sitemap root that is closest to the repository root starting with the
     * given {@link Resource}.
     *
     * @param sitemapRoot
     * @return
     */
    @NotNull
    public static Resource getTopLevelSitemapRoot(@NotNull Resource sitemapRoot) {
        Resource topLevelSitemapRoot = sitemapRoot;
        Resource parent = sitemapRoot.getParent();

        while (parent != null) {
            if (isSitemapRoot(parent)) {
                topLevelSitemapRoot = parent;
            }
            parent = parent.getParent();
        }

        return topLevelSitemapRoot;
    }

    @Nullable
    public static Resource normalizeSitemapRoot(@Nullable Resource resource) {
        if (!isSitemapRoot(resource)) {
            return null;
        }
        if (resource.getName().equals(JcrConstants.JCR_CONTENT)) {
            return resource.getParent();
        } else {
            return resource;
        }
    }

    public static boolean isSitemapRoot(@Nullable Resource resource) {
        if (resource == null) {
            return false;
        }

        Boolean sitemapRoot = resource.getValueMap().get(SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.class);
        if (sitemapRoot == null) {
            Resource content = resource.getChild(JcrConstants.JCR_CONTENT);
            if (content != null) {
                sitemapRoot = content.getValueMap().get(SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.FALSE);
            } else {
                sitemapRoot = Boolean.FALSE;
            }
        }
        return sitemapRoot;
    }

    public static boolean isTopLevelSitemapRoot(@Nullable Resource resource) {
        return isSitemapRoot(resource) && getTopLevelSitemapRoot(resource).getPath().equals(resource.getPath());
    }

    @NotNull
    public static String getSitemapSelector(@NotNull Resource sitemapRoot, @NotNull String name) {
        return getSitemapSelector(sitemapRoot, getTopLevelSitemapRoot(sitemapRoot), name);
    }

    @NotNull
    public static String getSitemapSelector(@NotNull Resource sitemapRoot, @NotNull Resource topLevelSitemapRoot, @NotNull String name) {
        name = SitemapGenerator.DEFAULT_SITEMAP.equals(name) ? "sitemap" : name + "-sitemap";

        if (!sitemapRoot.getPath().equals(topLevelSitemapRoot.getPath())) {
            String sitemapRootSubpath = sitemapRoot.getPath().substring(topLevelSitemapRoot.getPath().length() + 1);
            name = sitemapRootSubpath.replace('/', '-') + '-' + name;
        }

        return name;
    }

    @NotNull
    public static Map<Resource, String> resolveSitemapRoots(@NotNull Resource sitemapRoot, @NotNull String sitemapSelector) {
        if (sitemapSelector.equals("sitemap")) {
            return Collections.singletonMap(sitemapRoot, SitemapGenerator.DEFAULT_SITEMAP);
        } else if (!sitemapSelector.endsWith("-sitemap")) {
            return Collections.emptyMap();
        } else {
            List<String> parts = Arrays.asList(sitemapSelector.split("-"));
            List<String> relevantParts = parts.subList(0, parts.size() - 1);
            Map<Resource, String> roots = new LinkedHashMap<>();
            resolveSitemapRoots(sitemapRoot, relevantParts, roots);
            return roots;
        }
    }

    private static void resolveSitemapRoots(@NotNull Resource sitemapRoot, @NotNull List<String> parts,
                                            @NotNull Map<Resource, String> result) {
        if (isSitemapRoot(sitemapRoot)) {
            result.put(sitemapRoot, String.join("-", parts));
        }
        for (int i = 0, j; i < parts.size(); i++) {
            // products product page tops
            j = i + 1;
            String childName = String.join("-", parts.subList(0, j));
            Resource namedChild = sitemapRoot.getChild(childName);
            if (namedChild != null) {
                if (j == parts.size() && isSitemapRoot(namedChild)) {
                    result.put(namedChild, SitemapGenerator.DEFAULT_SITEMAP);
                } else if (parts.size() > j) {
                    resolveSitemapRoots(namedChild, parts.subList(j, parts.size()), result);
                }
            }
        }
    }

    /**
     * Returns
     *
     * @param resolver
     * @param searchPath
     * @return
     */
    @NotNull
    public static Iterator<Resource> findSitemapRoots(ResourceResolver resolver, String searchPath) {
        String correctedSearchPath = searchPath == null ? "/" : searchPath;
        StringBuilder query = new StringBuilder(correctedSearchPath.length() + 35);
        query.append("/jcr:root").append(ISO9075.encodePath(correctedSearchPath));
        if (!correctedSearchPath.endsWith("/")) {
            query.append('/');
        }
        query.append("/*[@").append(SitemapService.PROPERTY_SITEMAP_ROOT).append('=').append(Boolean.TRUE).append(']');

        return new Iterator<Resource>() {
            private final Iterator<Resource> hits = resolver.findResources(query.toString(), Query.XPATH);
            private Resource next = seek();

            private Resource seek() {
                while (hits.hasNext()) {
                    Resource nextHit = normalizeSitemapRoot(hits.next());
                    // skip a hit on the given searchPath itself. This may be when a search is done for descendant
                    // sitemaps given the normalized sitemap root path and the sitemap root's jcr:content is in the
                    // result set.
                    if (nextHit == null
                            || nextHit.getPath().equals(correctedSearchPath)
                            || nextHit.getPath().startsWith(JCR_SYSTEM_PATH)) {
                        continue;
                    }
                    return nextHit;
                }
                return null;
            }

            @Override
            public boolean hasNext() {
                return next != null;
            }

            @Override
            public Resource next() {
                Resource ret = next;
                next = seek();
                return ret;
            }
        };
    }
}
