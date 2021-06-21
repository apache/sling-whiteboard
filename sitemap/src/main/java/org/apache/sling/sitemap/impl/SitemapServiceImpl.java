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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.sitemap.SitemapInfo;
import org.apache.sling.sitemap.SitemapService;
import org.apache.sling.sitemap.common.SitemapLinkExternalizer;
import org.apache.sling.sitemap.common.SitemapUtil;
import org.apache.sling.sitemap.generator.SitemapGeneratorManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.*;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.apache.sling.sitemap.common.SitemapUtil.*;

@Component(service = SitemapService.class)
public class SitemapServiceImpl implements SitemapService {

    private static final Logger LOG = LoggerFactory.getLogger(SitemapServiceImpl.class);

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
    private SitemapLinkExternalizer externalizer;
    @Reference
    private SitemapGeneratorManager generatorManager;
    @Reference
    private SitemapStorage storage;
    @Reference
    private SitemapServiceConfiguration sitemapServiceConfiguration;

    private ServiceTracker<SitemapScheduler, SitemapScheduler> schedulers;

    @Activate
    protected void activate(BundleContext bundleContext) {
        schedulers = new ServiceTracker<>(bundleContext, SitemapScheduler.class, null);
        schedulers.open();
    }

    @Deactivate
    protected void deactivate() {
        schedulers.close();
    }

    @Override
    public int getMaxSize() {
        return sitemapServiceConfiguration.getMaxSize();
    }

    @Override
    public int getMaxEntries() {
        return sitemapServiceConfiguration.getMaxEntries();
    }

    @Override
    public void scheduleGeneration() {
        if (schedulers.getServiceReferences() != null) {
            for (ServiceReference<SitemapScheduler> scheduler : schedulers.getServiceReferences()) {
                schedulers.getService(scheduler).run();
            }
        }
    }

    @Override
    public void scheduleGeneration(String name) {
        if (schedulers.getServiceReferences() != null) {
            for (ServiceReference<SitemapScheduler> scheduler : schedulers.getServiceReferences()) {
                schedulers.getService(scheduler).schedule(Collections.singleton(name));
            }
        }
    }

    @Override
    public void scheduleGeneration(Resource sitemapRoot) {
        if (schedulers.getServiceReferences() == null || !SitemapUtil.isSitemapRoot(sitemapRoot)) {
            return;
        }
        for (ServiceReference<SitemapScheduler> scheduler : schedulers.getServiceReferences()) {
            Object searchPath = scheduler.getProperty("searchPath");
            if (searchPath instanceof String && sitemapRoot.getPath().startsWith(searchPath + "/")) {
                schedulers.getService(scheduler).schedule(sitemapRoot, null);
            }
        }
    }

    @Override
    public void scheduleGeneration(Resource sitemapRoot, String name) {
        if (schedulers.getServiceReferences() == null || !SitemapUtil.isSitemapRoot(sitemapRoot)) {
            return;
        }

        for (ServiceReference<SitemapScheduler> scheduler : schedulers.getServiceReferences()) {
            Object searchPath = scheduler.getProperty("searchPath");
            if (searchPath instanceof String && sitemapRoot.getPath().startsWith(searchPath + "/")) {
                schedulers.getService(scheduler).schedule(sitemapRoot, Collections.singleton(name));
            }
        }
    }

    @NotNull
    @Override
    public Collection<SitemapInfo> getSitemapInfo(@NotNull Resource resource) {
        Resource sitemapRoot = normalizeSitemapRoot(resource);

        if (sitemapRoot == null) {
            LOG.debug("Not a sitemap root: {}", resource.getPath());
            return Collections.emptySet();
        }

        Resource topLevelSitemapRoot = isTopLevelSitemapRoot(sitemapRoot)
                ? sitemapRoot
                : getTopLevelSitemapRoot(sitemapRoot);
        String baseUrl = externalize(topLevelSitemapRoot);

        if (baseUrl == null) {
            LOG.debug("Could not get absolute url to sitemap: {}", resource.getPath());
            return Collections.emptySet();
        }

        Collection<String> names = new HashSet<>(generatorManager.getNames(sitemapRoot));
        Set<String> onDemandNames = generatorManager.getOnDemandNames(sitemapRoot);
        Collection<SitemapInfo> infos = new ArrayList<>(names.size() + 1);

        if (requiresSitemapIndex(sitemapRoot)) {
            String location = baseUrl + '.' + SitemapServlet.SITEMAP_INDEX_SELECTOR + '.' +
                    SitemapServlet.SITEMAP_EXTENSION;
            infos.add(newSitemapIndexInfo(location));
        }

        // write on demand sitemaps
        for (Iterator<String> it = names.iterator(); it.hasNext(); ) {
            String name = it.next();

            if (!onDemandNames.contains(name)) {
                continue;
            }

            it.remove();
            String selector = getSitemapSelector(sitemapRoot, topLevelSitemapRoot, name);
            String location = newSitemapUrl(baseUrl, selector);
            infos.add(newOnDemandSitemapInfo(location, name));
        }

        if (names.isEmpty()) {
            // early exit when only sitemap-index / on-demand sitemaps are served for the given root
            return infos;
        }

        for (SitemapStorageInfo storageInfo : storage.getSitemaps(sitemapRoot, names)) {
            String location = newSitemapUrl(baseUrl, storageInfo.getSitemapSelector());
            infos.add(
                    newStoredSitemapInfo(storageInfo.getPath(), location, storageInfo.getName(), storageInfo.getSize(),
                            storageInfo.getEntries()));
            names.remove(storageInfo.getName());
        }

        if (names.isEmpty()) {
            // early exit when all sitemaps are either on-demand or stored
            return infos;
        }

        // now names will contain only sitemaps that are either scheduled or not scheduled (generated on a different
        // host)
        ServiceReference<SitemapScheduler>[] schedulerRefs = schedulers.getServiceReferences();
        for (String name : names) {
            // check if a scheduler applicable for the name exists
            boolean hasApplicableScheduler = schedulerRefs != null && Arrays.stream(schedulerRefs)
                    .map(schedulers::getService)
                    .map(scheduler -> scheduler.getApplicableNames(resource))
                    .anyMatch(applicableNames -> applicableNames.contains(name));
            String selector = getSitemapSelector(sitemapRoot, topLevelSitemapRoot, name);
            String location = newSitemapUrl(baseUrl, selector);
            infos.add(newOnDemandSitemapInfo(location, name,
                    hasApplicableScheduler ? SitemapInfo.Status.SCHEDULED : SitemapInfo.Status.UNKNOWN));
        }


        return infos;
    }

    private boolean requiresSitemapIndex(@NotNull Resource sitemapRoot) {
        Set<String> names = generatorManager.getGenerators(sitemapRoot).keySet();
        return isTopLevelSitemapRoot(sitemapRoot)
                && (names.size() > 1
                || findSitemapRoots(sitemapRoot.getResourceResolver(), sitemapRoot.getPath()).hasNext()
                || storage.getSitemaps(sitemapRoot, names).size() > 1);
    }

    private String externalize(Resource resource) {
        return (externalizer == null ? SitemapLinkExternalizer.DEFAULT : externalizer).externalize(resource);
    }

    private static SitemapInfo newSitemapIndexInfo(@NotNull String url) {
        return new SitemapInfoImpl(null, url, SITEMAP_INDEX_NAME, SitemapInfo.Status.ON_DEMAND, -1, -1);
    }

    private static SitemapInfo newOnDemandSitemapInfo(@NotNull String url, @NotNull String name) {
        return newOnDemandSitemapInfo(url, name, SitemapInfo.Status.ON_DEMAND);
    }

    private static SitemapInfo newOnDemandSitemapInfo(@NotNull String url, @NotNull String name, @NotNull
            SitemapInfo.Status status) {
        return new SitemapInfoImpl(null, url, name, status, -1, -1);
    }

    private static SitemapInfo newStoredSitemapInfo(@NotNull String path, @NotNull String url, @NotNull String name,
            int size, int entries) {
        return new SitemapInfoImpl(path, url, name, SitemapInfo.Status.STORAGE, size, entries);
    }

    private static String newSitemapUrl(String baseUrl, String selector) {
        // worst case: 1x baseUrl, 1x selector, 1x sitemap-selector, 1x extension, 3x <dot>
        StringBuilder builder = new StringBuilder(baseUrl.length() + selector.length() +
                SitemapServlet.SITEMAP_SELECTOR.length() + SitemapServlet.SITEMAP_EXTENSION.length() + 3);
        builder.append(baseUrl);
        builder.append('.');

        if (!selector.equals(SitemapServlet.SITEMAP_SELECTOR)) {
            builder.append(SitemapServlet.SITEMAP_SELECTOR);
            builder.append('.');
        }

        builder.append(selector);
        builder.append('.');
        builder.append(SitemapServlet.SITEMAP_EXTENSION);

        return builder.toString();
    }

    private static class SitemapInfoImpl implements SitemapInfo {

        private final String url;
        private final String path;
        private final String name;
        private final Status status;
        private final int size;
        private final int entries;

        private SitemapInfoImpl(@Nullable String path, @NotNull String url, @NotNull String name,
                @NotNull Status status, int size, int entries) {
            this.path = path;
            this.url = url;
            this.name = name;
            this.status = status;
            this.size = size;
            this.entries = entries;
        }

        @Nullable
        @Override
        public String getStoragePath() {
            return path;
        }

        @NotNull
        @Override
        public String getUrl() {
            return url;
        }

        @Override
        @NotNull
        public String getName() {
            return name;
        }

        @NotNull
        @Override
        public SitemapInfo.Status getStatus() {
            return status;
        }

        @Override
        public int getSize() {
            return size;
        }

        @Override
        public int getEntries() {
            return entries;
        }

        @Override
        public String toString() {
            return "SitemapInfoImpl{" +
                    "url='" + url + '\'' +
                    ", size=" + size +
                    ", entries=" + entries +
                    '}';
        }
    }
}
