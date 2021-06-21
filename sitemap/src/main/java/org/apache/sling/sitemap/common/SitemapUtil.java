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
package org.apache.sling.sitemap.common;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.sitemap.SitemapService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.jcr.query.Query;
import java.util.*;

/**
 * A utility class to give access to common functionality used for sitemaps.
 */
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

    /**
     * Returns the parent of the given {@link Resource} when the {@link Resource}'s name is
     * {@link JcrConstants#JCR_CONTENT}.
     *
     * @param resource
     * @return
     */
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

    /**
     * Returns true when the given {@link Resource} is a sitemap root.
     *
     * @param resource
     * @return
     */
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

    /**
     * Returns true when the given {@link Resource} is a top level sitemap root.
     *
     * @param resource
     * @return
     */
    public static boolean isTopLevelSitemapRoot(@Nullable Resource resource) {
        return isSitemapRoot(resource) && getTopLevelSitemapRoot(resource).getPath().equals(resource.getPath());
    }

    /**
     * Returns the selector for the given sitemap root {@link Resource} and the given name.
     *
     * @param sitemapRoot
     * @param topLevelSitemapRoot
     * @param name
     * @return
     */
    @NotNull
    public static String getSitemapSelector(@NotNull Resource sitemapRoot, @NotNull Resource topLevelSitemapRoot,
            @NotNull String name) {
        name = SitemapService.DEFAULT_SITEMAP_NAME.equals(name) ? "sitemap" : name + "-sitemap";

        if (!sitemapRoot.getPath().equals(topLevelSitemapRoot.getPath())) {
            String sitemapRootSubpath = sitemapRoot.getPath().substring(topLevelSitemapRoot.getPath().length() + 1);
            name = sitemapRootSubpath.replace('/', '-') + '-' + name;
        }

        return name;
    }

    /**
     * Resolves all sitemap root {@link Resource}s for the given selector within the given top level sitemap root
     * {@link Resource}. This is the inversion of {@link SitemapUtil#getSitemapSelector(Resource, Resource, String)} with
     * sitemap root being a top level sitemap root.
     * <p>
     * The returned {@link Map} only contains {@link Resource}s, that are sitemap roots according to
     * {@link SitemapUtil#isSitemapRoot(Resource)}. Each returned {@link Resource} is mapped to the name which when
     * passed to {@link SitemapUtil#getSitemapSelector(Resource, Resource, String)} would return the same selector,
     * omitting the optional multi-file index part.
     * <p>
     * As this resolution may be ambiguous, the returned {@link Map} is sorted with the sitemap root/name combinations
     * closest to the top level sitemap root taking precedence.
     *
     * @param topLevelSitemapRoot
     * @param sitemapSelector
     * @return a sorted {@link Map}
     */
    @NotNull
    public static Map<Resource, String> resolveSitemapRoots(@NotNull Resource topLevelSitemapRoot,
            @NotNull String sitemapSelector) {
        if (!isTopLevelSitemapRoot(topLevelSitemapRoot)) {
            // selectors are always relative to a top level sitemap root
            return Collections.emptyMap();
        }
        if (sitemapSelector.equals("sitemap")) {
            return Collections.singletonMap(topLevelSitemapRoot, SitemapService.DEFAULT_SITEMAP_NAME);
        }

        List<String> parts = Arrays.asList(sitemapSelector.split("-"));
        List<String> relevantParts;

        if (parts.size() == 2 && parts.get(0).equals("sitemap") && isInteger(parts.get(1))) {
            // default name with file index
            return Collections.singletonMap(topLevelSitemapRoot, SitemapService.DEFAULT_SITEMAP_NAME);
        } else if (parts.size() > 1 && parts.get(parts.size() - 1).equals("sitemap")) {
            // no file index part
            relevantParts = parts.subList(0, parts.size() - 1);
        } else if (parts.size() > 2 && parts.get(parts.size() - 2).equals("sitemap")
                && isInteger(parts.get(parts.size() - 1))) {
            // with file index part
            relevantParts = parts.subList(0, parts.size() - 2);
        } else {
            return Collections.emptyMap();
        }

        Map<Resource, String> roots = new LinkedHashMap<>();
        resolveSitemapRoots(topLevelSitemapRoot, relevantParts, roots);
        return roots;
    }

    /**
     * Returns all sitemap root {@link Resource}s within the given search path.
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
        query.append(" option(index tag slingSitemaps)");

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

    private static boolean isInteger(String text) {
        try {
            Integer.parseUnsignedInt(text);
            return true;
        } catch (NumberFormatException ex) {
            return false;
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
                    result.put(namedChild, SitemapService.DEFAULT_SITEMAP_NAME);
                } else if (parts.size() > j) {
                    resolveSitemapRoots(namedChild, parts.subList(j, parts.size()), result);
                }
            }
        }
    }
}
