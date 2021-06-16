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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.sitemap.SitemapException;
import org.apache.sling.sitemap.common.SitemapLinkExternalizer;
import org.apache.sling.sitemap.generator.SitemapGenerator;
import org.apache.sling.sitemap.generator.SitemapGeneratorManager;
import org.apache.sling.sitemap.impl.builder.SitemapImpl;
import org.apache.sling.sitemap.impl.builder.SitemapIndexImpl;
import org.apache.sling.sitemap.impl.builder.extensions.ExtensionProviderManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

import static org.apache.sling.sitemap.common.SitemapUtil.*;
import static org.apache.sling.sitemap.impl.SitemapServlet.*;

@Component(
        service = Servlet.class,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {
                ServletResolverConstants.SLING_SERVLET_SELECTORS + "=" + SITEMAP_SELECTOR,
                ServletResolverConstants.SLING_SERVLET_SELECTORS + "=" + SITEMAP_INDEX_SELECTOR,
                ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=" + SITEMAP_EXTENSION,
        }
)
public class SitemapServlet extends SlingSafeMethodsServlet {

    static final String SITEMAP_SELECTOR = "sitemap";
    static final String SITEMAP_INDEX_SELECTOR = "sitemap-index";
    static final String SITEMAP_EXTENSION = "xml";

    private static final Logger LOG = LoggerFactory.getLogger(SitemapServlet.class);
    private static SitemapGenerator.GenerationContext NOOP_CONTEXT = new SitemapGenerator.GenerationContext() {
        @Nullable
        @Override
        public <T> T getProperty(@NotNull String name, @NotNull Class<T> cls) {
            return null;
        }

        @NotNull
        @Override
        public <T> T getProperty(@NotNull String name, @NotNull T defaultValue) {
            return defaultValue;
        }

        @Override
        public void setProperty(@NotNull String name, @Nullable Object data) {
        }
    };

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
    private SitemapLinkExternalizer externalizer;
    @Reference
    private SitemapGeneratorManager generatorManager;
    @Reference
    private ExtensionProviderManager extensionProviderManager;
    @Reference
    private SitemapStorage storage;
    @Reference
    private SitemapServiceConfiguration sitemapServiceConfiguration;

    @Override
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response)
            throws ServletException, IOException {
        try {
            Resource requestedResource = normalizeSitemapRoot(request.getResource());

            if (!isSitemapRoot(requestedResource)) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            response.setCharacterEncoding("utf-8");
            response.setContentType("application/xml");

            List<String> selectors = Arrays.asList(request.getRequestPathInfo().getSelectors());

            if (selectors.size() == 1 && selectors.contains(SITEMAP_INDEX_SELECTOR)) {
                doGetSitemapIndex(request, response, requestedResource);
            } else if (selectors.size() == 1 && selectors.contains(SITEMAP_SELECTOR)) {
                // when only one selector is provided, that means the default sitemap got requested
                doGetSitemap(request, response, requestedResource, selectors.get(0));
            } else if (selectors.size() == 2 && selectors.get(0).equals(SITEMAP_SELECTOR)) {
                doGetSitemap(request, response, requestedResource, selectors.get(1));
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (SitemapException ex) {
            if (ex.getCause() instanceof IOException) {
                throw (IOException) ex.getCause();
            } else {
                throw new ServletException(ex);
            }
        }
    }

    protected void doGetSitemapIndex(@NotNull SlingHttpServletRequest request,
            @NotNull SlingHttpServletResponse response,
            Resource topLevelSitemapRoot) throws IOException, SitemapException {
        // when sitemaps may be served on demand, we have to query for the sitemap roots of the current resource,
        // otherwise we can simply get the top level's storage path and serve all sitemaps in there
        SitemapIndexImpl sitemapIndex = new SitemapIndexImpl(response.getWriter());
        Set<String> addedSitemapSelectors = addOnDemandSitemapsToIndex(request, topLevelSitemapRoot, sitemapIndex);

        // add any sitemap from the storage
        for (SitemapStorageInfo storageInfo : storage.getSitemaps(topLevelSitemapRoot)) {
            if (!addedSitemapSelectors.contains(storageInfo.getSitemapSelector())) {
                String location = externalize(request,
                        getSitemapLink(topLevelSitemapRoot, storageInfo.getSitemapSelector()));
                Calendar lastModified = storageInfo.getLastModified();
                if (location != null && lastModified != null) {
                    sitemapIndex.addSitemap(location, lastModified.toInstant());
                } else if (location != null) {
                    sitemapIndex.addSitemap(location);
                } else {
                    LOG.debug("Could not get absolute url for sitemap served from {}",
                            storageInfo.getSitemapSelector());
                }
            }
        }

        sitemapIndex.close();
    }

    protected void doGetSitemap(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response,
            Resource topLevelSitemapRoot, String sitemapSelector) throws SitemapException, IOException {
        Set<String> onDemandNames = generatorManager.getOnDemandNames(topLevelSitemapRoot);
        if (onDemandNames.size() > 0) {
            // resolve the actual sitemap root from the sitemapSelector
            Map<Resource, String> candidates = resolveSitemapRoots(topLevelSitemapRoot, sitemapSelector);

            for (Map.Entry<Resource, String> entry : candidates.entrySet()) {
                Resource sitemapRoot = entry.getKey();
                String name = entry.getValue();
                SitemapGenerator generator = generatorManager.getGenerator(sitemapRoot, name);

                if (generator != null && onDemandNames.contains(name)) {
                    SitemapImpl sitemap = new SitemapImpl(response.getWriter(), extensionProviderManager);
                    generator.generate(sitemapRoot, name, sitemap, NOOP_CONTEXT);
                    sitemap.close();
                    return;
                }
            }
        }

        if (!storage.copySitemap(topLevelSitemapRoot, sitemapSelector, response.getOutputStream())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Adds all on-demand sitemaps to the index within the given sitemap root.
     *
     * @param request
     * @param parentSitemapRoot
     * @param index
     * @return
     * @throws SitemapException
     */
    private Set<String> addOnDemandSitemapsToIndex(SlingHttpServletRequest request, Resource parentSitemapRoot,
            SitemapIndexImpl index) throws SitemapException {
        Set<String> addedSitemapSelectors = new HashSet<>();
        Iterator<Resource> sitemapRoots = findSitemapRoots(request.getResourceResolver(), parentSitemapRoot.getPath());
        if (!sitemapRoots.hasNext()) {
            // serve at least the top level sitemap
            sitemapRoots = Collections.singleton(parentSitemapRoot).iterator();
        }
        while (sitemapRoots.hasNext()) {
            Resource sitemapRoot = sitemapRoots.next();
            Set<String> applicableNames = generatorManager.getOnDemandNames(sitemapRoot);
            // applicable names we may serve directly, not applicable names, if any, we have to serve from storage
            for (String applicableName : applicableNames) {
                String sitemapSelector = getSitemapSelector(sitemapRoot, parentSitemapRoot, applicableName);
                String location = externalize(request, getSitemapLink(sitemapRoot, sitemapSelector));
                if (location != null) {
                    index.addSitemap(location);
                    addedSitemapSelectors.add(sitemapSelector);
                } else {
                    LOG.debug("Could not get absolute url for on-demand sitemap: {}", sitemapSelector);
                }
            }
        }

        return addedSitemapSelectors;
    }

    private String externalize(SlingHttpServletRequest request, String uri) {
        return (externalizer == null ? SitemapLinkExternalizer.DEFAULT : externalizer).externalize(request, uri);
    }

    private static String getSitemapLink(Resource sitemapRoot, String sitemapSelector) {
        String link = sitemapRoot.getPath() + '.' + SITEMAP_SELECTOR + '.';
        if (SITEMAP_SELECTOR.equals(sitemapSelector)) {
            return link + SITEMAP_EXTENSION;
        } else {
            return link + sitemapSelector + '.' + SITEMAP_EXTENSION;
        }
    }
}
