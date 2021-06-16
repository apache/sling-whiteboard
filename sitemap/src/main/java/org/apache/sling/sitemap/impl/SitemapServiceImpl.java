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
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
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
import java.util.regex.Pattern;

import static org.apache.sling.sitemap.common.SitemapUtil.*;

@Component(service = SitemapService.class)
public class SitemapServiceImpl implements SitemapService {

    private static final Logger LOG = LoggerFactory.getLogger(SitemapServiceImpl.class);

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
    private SitemapLinkExternalizer externalizer;
    @Reference
    private JobManager jobManager;
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
            infos.add(newSitemapInfo(null, location, name, -1, -1));
        }

        if (names.isEmpty()) {
            // early exit when only sitemap-index / on-demand sitemaps are served for the given root
            return infos;
        }

        for (SitemapStorageInfo storageInfo : storage.getSitemaps(sitemapRoot, names)) {
            String location = newSitemapUrl(baseUrl, storageInfo.getSitemapSelector());
            infos.add(newSitemapInfo(storageInfo.getPath(), location, storageInfo.getName(), storageInfo.getSize(),
                    storageInfo.getEntries()));
        }

        return infos;
    }

    @Override
    public boolean isSitemapGenerationPending(@NotNull Resource sitemapRoot) {
        sitemapRoot = normalizeSitemapRoot(sitemapRoot);

        if (sitemapRoot == null) {
            return false;
        }

        Set<String> onDemandNames = generatorManager.getOnDemandNames(sitemapRoot);

        // not a sitemap root, always more then one sitemap, all sitemaps on-demand
        if (!isSitemapRoot(sitemapRoot)
                || requiresSitemapIndex(sitemapRoot)
                || onDemandNames.containsAll(generatorManager.getGenerators(sitemapRoot).keySet())) {
            return false;
        }

        // check if for each name a sitemap is in the storage
        Collection<String> names = new HashSet<>(generatorManager.getGenerators(sitemapRoot).keySet());
        names.removeAll(onDemandNames);
        storage.getSitemaps(sitemapRoot, names).stream().map(SitemapStorageInfo::getName).forEach(names::remove);

        if (!names.isEmpty()) {
            // at least one sitemap is missing and so generation is pending (already queued, active or not yet
            // scheduled). This is not fully precise as multi-file sitemaps could more then once
            return true;
        }

        // last check if there is still a job running
        Collection<Job> jobs = jobManager.findJobs(
                JobManager.QueryType.ALL,
                SitemapGeneratorExecutor.JOB_TOPIC,
                1,
                Collections.singletonMap(SitemapGeneratorExecutor.JOB_PROPERTY_SITEMAP_ROOT, sitemapRoot.getPath()));

        return jobs.size() > 0;
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

    private SitemapInfo newSitemapIndexInfo(@NotNull String url) {
        return new SitemapInfoImpl(null, url, SITEMAP_INDEX_NAME, -1, -1);
    }

    private SitemapInfo newSitemapInfo(@Nullable String path, @NotNull String url, @Nullable String name, int size,
                                       int entries) {
        return new SitemapInfoImpl(path, url, name, size, entries);
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
        private final int size;
        private final int entries;

        private SitemapInfoImpl(@Nullable String path, @NotNull String url, @Nullable String name, int size,
                                int entries) {
            this.path = path;
            this.url = url;
            this.name = name;
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
        @Nullable
        public String getName() {
            return name;
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
