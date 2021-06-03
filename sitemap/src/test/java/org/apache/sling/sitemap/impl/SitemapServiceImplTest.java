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

import com.google.common.collect.ImmutableSet;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.sitemap.SitemapInfo;
import org.apache.sling.sitemap.common.Externalizer;
import org.apache.sling.sitemap.generator.SitemapGenerator;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.hamcrest.CustomMatcher;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.query.Query;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith({SlingContextExtension.class, MockitoExtension.class})
public class SitemapServiceImplTest {

    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);

    private final SitemapServiceImpl subject = new SitemapServiceImpl();
    private final SitemapGeneratorManager generatorManager = new SitemapGeneratorManager();

    @Mock
    private JobManager jobManager;
    @Mock
    private SitemapGenerator generator;
    @Mock
    private SitemapStorage storage;

    private Resource deRoot;
    private Resource enRoot;
    private Resource enFaqs;
    private Resource enNews;
    private Resource frRoot;
    private Resource noRoot;

    @BeforeEach
    public void setup() {
        deRoot = context.create().resource("/content/site/de", Collections.singletonMap(
                "sitemapRoot", Boolean.TRUE
        ));
        enRoot = context.create().resource("/content/site/en", Collections.singletonMap(
                "sitemapRoot", Boolean.TRUE
        ));
        enFaqs = context.create().resource("/content/site/en/fags", Collections.singletonMap(
                "sitemapRoot", Boolean.TRUE
        ));
        enNews = context.create().resource("/content/site/en/news", Collections.singletonMap(
                "sitemapRoot", Boolean.TRUE
        ));
        frRoot = context.create().resource("/content/site/fr", Collections.singletonMap(
                "sitemapRoot", Boolean.TRUE
        ));
        noRoot = context.create().resource("/content/site/nothing");

        context.registerService(SitemapStorage.class, storage);
        context.registerService(SitemapGenerator.class, generator);
        context.registerService(JobManager.class, jobManager);
        context.registerInjectActivateService(generatorManager);
        context.registerInjectActivateService(subject, "onDemandNames", "news");
    }

    @Test
    public void testIsPendingReturnsTrue() {
        // given
        when(generator.getNames(any())).thenReturn(Collections.singleton(SitemapGenerator.DEFAULT_SITEMAP));
        when(jobManager.findJobs(
                eq(JobManager.QueryType.ALL),
                eq(SitemapGeneratorExecutor.JOB_TOPIC),
                eq(1L),
                ArgumentMatchers.<Map<String, Object>>argThat(arg -> arg.size() == 1
                        && arg.containsKey(SitemapGeneratorExecutor.JOB_PROPERTY_SITEMAP_ROOT)
                        && arg.containsValue("/content/site/de"))))
                .thenReturn(Collections.singleton(mock(Job.class)));
        when(storage.getSitemaps(eq(deRoot), any())).thenReturn(Collections.singleton(mock(SitemapStorageInfo.class)));

        // when, then
        // one in storage, one job
        assertTrue(subject.isSitemapGenerationPending(deRoot));
    }

    @Test
    public void testIsPendingReturnsFalse() {
        // given
        when(generator.getNames(any())).thenReturn(ImmutableSet.of(SitemapGenerator.DEFAULT_SITEMAP));
        when(generator.getNames(frRoot)).thenReturn(ImmutableSet.of("a", "b"));
        when(generator.getNames(enNews)).thenReturn(ImmutableSet.of("news"));
        when(jobManager.findJobs(
                eq(JobManager.QueryType.ALL),
                eq(SitemapGeneratorExecutor.JOB_TOPIC),
                eq(1L),
                ArgumentMatchers.<Map<String, Object>>argThat(arg -> arg.size() == 1
                        && arg.containsKey(SitemapGeneratorExecutor.JOB_PROPERTY_SITEMAP_ROOT)
                        && arg.containsValue("/content/site/de"))))
                .thenReturn(Collections.emptyList());
        when(storage.getSitemaps(eq(deRoot), any())).thenReturn(Collections.singleton(mock(SitemapStorageInfo.class)));

        MockJcr.setQueryResult(
                context.resourceResolver().adaptTo(Session.class),
                "/jcr:root/content/site/en//*[@sitemapRoot=true]",
                Query.XPATH,
                Arrays.asList(enRoot.adaptTo(Node.class), enFaqs.adaptTo(Node.class))
        );

        // when, then
        // one in storage, no job
        assertFalse(subject.isSitemapGenerationPending(deRoot));
        // not a root
        assertFalse(subject.isSitemapGenerationPending(noRoot));
        // has descendants => index on-demand
        assertFalse(subject.isSitemapGenerationPending(enRoot));
        // is on demand
        assertFalse(subject.isSitemapGenerationPending(enNews));
        // has multiple names => index on-demand
        assertFalse(subject.isSitemapGenerationPending(frRoot));
    }

    @Test
    public void testSitemapIndexUrlReturned() {
        // given
        when(generator.getNames(any())).thenReturn(Collections.singleton(SitemapGenerator.DEFAULT_SITEMAP));
        when(generator.getNames(frRoot)).thenReturn(ImmutableSet.of("a", "b"));

        MockJcr.setQueryResult(
                context.resourceResolver().adaptTo(Session.class),
                "/jcr:root/content/site/en//*[@sitemapRoot=true]",
                Query.XPATH,
                Arrays.asList(enRoot.adaptTo(Node.class), enFaqs.adaptTo(Node.class))
        );

        // when
        // has descendants
        Collection<SitemapInfo> enInfo = subject.getSitemapInfo(enRoot);
        // has multiple names
        Collection<SitemapInfo> frInfo = subject.getSitemapInfo(frRoot);

        // then
        assertThat(enInfo, hasSize(1));
        assertThat(enInfo, hasItems(
                eqSitemapInfo("/site/en.sitemap-index.xml", -1, -1)
        ));
        assertThat(frInfo, hasSize(1));
        assertThat(frInfo, hasItems(
                eqSitemapInfo("/site/fr.sitemap-index.xml", -1, -1)
        ));
    }

    @Test
    public void testSitemapUrlReturnEmptyCollections() {
        // no name
        assertThat(subject.getSitemapInfo(deRoot), hasSize(0));
        // no names nested
        assertThat(subject.getSitemapInfo(enFaqs), hasSize(0));
        // not a root
        assertThat(subject.getSitemapInfo(noRoot), hasSize(0));
    }

    @Test
    public void testSitemapUrlReturnsProperSelectors() {
        // given
        SitemapStorageInfo storageInfoSitemap =
                new SitemapStorageInfo("", "sitemap", null, 100, 1);
        SitemapStorageInfo storageInfoFoobarSitemap =
                new SitemapStorageInfo("", "foobar-sitemap", null, 100, 1);
        SitemapStorageInfo storageInfoNewsFooSitemap =
                new SitemapStorageInfo("", "news-foo-sitemap", null, 100, 1);
        SitemapStorageInfo storageInfoNewsBarSitemap =
                new SitemapStorageInfo("", "news-bar-sitemap", null, 100, 1);

        when(generator.getNames(deRoot)).thenReturn(ImmutableSet.of(SitemapGenerator.DEFAULT_SITEMAP));
        when(generator.getNames(frRoot)).thenReturn(ImmutableSet.of("foobar"));
        when(generator.getNames(enNews)).thenReturn(ImmutableSet.of("foo", "bar", "news"));
        when(storage.getSitemaps(any(), any())).thenReturn(ImmutableSet.of(
                storageInfoSitemap, storageInfoFoobarSitemap, storageInfoNewsFooSitemap, storageInfoNewsBarSitemap));

        // when
        Collection<SitemapInfo> deInfos = subject.getSitemapInfo(deRoot);
        Collection<SitemapInfo> frInfos = subject.getSitemapInfo(frRoot);
        Collection<SitemapInfo> enInfos = subject.getSitemapInfo(enNews);

        // no name
        assertThat(deInfos, hasSize(1));
        assertThat(deInfos, hasItems(eqSitemapInfo("/site/de.sitemap.xml", 100, 1)));
        // with single name
        assertThat(frInfos, hasSize(1));
        assertThat(frInfos, hasItems(eqSitemapInfo("/site/fr.sitemap.foobar-sitemap.xml", 100, 1)));
        // nested with multiple names
        assertThat(enInfos, hasSize(3));
        assertThat(enInfos, hasItems(
                eqSitemapInfo("/site/en.sitemap.news-foo-sitemap.xml", 100, 1),
                eqSitemapInfo("/site/en.sitemap.news-bar-sitemap.xml", 100, 1),
                // on-demand
                eqSitemapInfo("/site/en.sitemap.news-news-sitemap.xml", -1, -1)
        ));
    }

    private static Matcher<SitemapInfo> eqSitemapInfo(String url, int size, int entries) {
        return new CustomMatcher<SitemapInfo>("with url " + url + ", with size " + size + ", with entries " + entries) {
            @Override
            public boolean matches(Object o) {
                return o instanceof SitemapInfo &&
                        url.equals(((SitemapInfo) o).getUrl()) &&
                        size == ((SitemapInfo) o).getSize() &&
                        entries == ((SitemapInfo) o).getEntries();
            }
        };
    }
}
