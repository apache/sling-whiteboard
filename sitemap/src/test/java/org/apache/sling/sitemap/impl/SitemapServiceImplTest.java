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
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.apache.sling.sitemap.SitemapInfo;
import org.apache.sling.sitemap.SitemapService;
import org.apache.sling.sitemap.TestGenerator;
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
import java.io.ByteArrayInputStream;
import java.io.IOException;
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
    private final SitemapStorage storage = new SitemapStorage();
    private final SitemapGeneratorManagerImpl generatorManager = new SitemapGeneratorManagerImpl();
    private final SitemapServiceConfiguration sitemapServiceConfiguration = new SitemapServiceConfiguration();

    private TestGenerator generator = new TestGenerator() {};
    private TestGenerator newsGenerator = new TestGenerator() {};

    @Mock
    private ServiceUserMapped serviceUser;
    @Mock
    private JobManager jobManager;

    private Resource deRoot;
    private Resource enRoot;
    private Resource enFaqs;
    private Resource enNews;
    private Resource frRoot;
    private Resource noRoot;

    @BeforeEach
    public void setup() {
        deRoot = context.create().resource("/content/site/de", Collections.singletonMap(
                SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.TRUE
        ));
        enRoot = context.create().resource("/content/site/en", Collections.singletonMap(
                SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.TRUE
        ));
        enFaqs = context.create().resource("/content/site/en/fags", Collections.singletonMap(
                SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.TRUE
        ));
        enNews = context.create().resource("/content/site/en/news", Collections.singletonMap(
                SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.TRUE
        ));
        frRoot = context.create().resource("/content/site/fr", Collections.singletonMap(
                SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.TRUE
        ));
        noRoot = context.create().resource("/content/site/nothing");

        context.registerService(ServiceUserMapped.class, serviceUser, "subServiceName", "sitemap-writer");
        context.registerService(SitemapGenerator.class, generator);
        context.registerService(SitemapGenerator.class, newsGenerator);
        context.registerService(JobManager.class, jobManager);
        context.registerInjectActivateService(sitemapServiceConfiguration,
                "onDemandGenerators", newsGenerator.getClass().getName()
        );
        context.registerInjectActivateService(generatorManager);
        context.registerInjectActivateService(storage);
        context.registerInjectActivateService(subject);
    }

    @Test
    public void testIsPendingReturnsTrue() throws IOException {
        // given
        storage.writeSitemap(deRoot, SitemapGenerator.DEFAULT_SITEMAP, new ByteArrayInputStream(new byte[0]), 0, 0);

        generator.setNames(SitemapGenerator.DEFAULT_SITEMAP);

        when(jobManager.findJobs(
                eq(JobManager.QueryType.ALL),
                eq(SitemapGeneratorExecutor.JOB_TOPIC),
                eq(1L),
                ArgumentMatchers.<Map<String, Object>>argThat(arg -> arg.size() == 1
                        && arg.containsKey(SitemapGeneratorExecutor.JOB_PROPERTY_SITEMAP_ROOT)
                        && arg.containsValue("/content/site/de"))))
                .thenReturn(Collections.singleton(mock(Job.class)));

        // when, then
        // one in storage, one job
        assertTrue(subject.isSitemapGenerationPending(deRoot));
    }

    @Test
    public void testIsPendingReturnsFalse() throws IOException {
        // given
        storage.writeSitemap(deRoot, SitemapGenerator.DEFAULT_SITEMAP, new ByteArrayInputStream(new byte[0]), 0, 0);

        generator.setNames(SitemapGenerator.DEFAULT_SITEMAP);
        generator.setNames(frRoot, "a", "b");
        newsGenerator.setNames(enNews, "news");

        when(jobManager.findJobs(
                eq(JobManager.QueryType.ALL),
                eq(SitemapGeneratorExecutor.JOB_TOPIC),
                eq(1L),
                ArgumentMatchers.<Map<String, Object>>argThat(arg -> arg.size() == 1
                        && arg.containsKey(SitemapGeneratorExecutor.JOB_PROPERTY_SITEMAP_ROOT)
                        && arg.containsValue("/content/site/de"))))
                .thenReturn(Collections.emptyList());

        MockJcr.setQueryResult(
                context.resourceResolver().adaptTo(Session.class),
                "/jcr:root/content/site/en//*[@" + SitemapService.PROPERTY_SITEMAP_ROOT + "=true]" +
                        " option(index tag slingSitemaps)",
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
        generator.setNames(SitemapGenerator.DEFAULT_SITEMAP);
        generator.setNames(frRoot, "a", "b");

        MockJcr.setQueryResult(
                context.resourceResolver().adaptTo(Session.class),
                "/jcr:root/content/site/en//*[@" + SitemapService.PROPERTY_SITEMAP_ROOT + "=true]" +
                        " option(index tag slingSitemaps)",
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
    public void testSitemapUrlReturnsProperSelectors() throws IOException {
        // given
        storage.writeSitemap(deRoot, SitemapGenerator.DEFAULT_SITEMAP, new ByteArrayInputStream(new byte[0]), 100, 1);
        storage.writeSitemap(frRoot, "foobar", new ByteArrayInputStream(new byte[0]), 100, 1);
        storage.writeSitemap(enRoot, SitemapGenerator.DEFAULT_SITEMAP, new ByteArrayInputStream(new byte[0]), 100, 1);
        storage.writeSitemap(enNews, "foo", new ByteArrayInputStream(new byte[0]), 100, 1);
        storage.writeSitemap(enNews, "bar", new ByteArrayInputStream(new byte[0]), 100, 1);

        generator.setNames(deRoot, SitemapGenerator.DEFAULT_SITEMAP);
        generator.setNames(frRoot, "foobar");
        generator.setNames(enNews, "foo", "bar");
        generator.setNames(enRoot, SitemapGenerator.DEFAULT_SITEMAP);
        newsGenerator.setNames(enNews, "news");

        MockJcr.setQueryResult(
                context.resourceResolver().adaptTo(Session.class),
                "/jcr:root/content/site/en//*[@" + SitemapService.PROPERTY_SITEMAP_ROOT + "=true]" +
                        " option(index tag slingSitemaps)",
                Query.XPATH,
                Arrays.asList(enRoot.adaptTo(Node.class), enNews.adaptTo(Node.class))
        );

        // when
        Collection<SitemapInfo> deInfos = subject.getSitemapInfo(deRoot);
        Collection<SitemapInfo> frInfos = subject.getSitemapInfo(frRoot);
        Collection<SitemapInfo> enInfos = subject.getSitemapInfo(enRoot);
        Collection<SitemapInfo> enNestedInfos = subject.getSitemapInfo(enNews);

        // no name
        assertThat(deInfos, hasSize(1));
        assertThat(deInfos, hasItems(eqSitemapInfo("/site/de.sitemap.xml", 100, 1)));
        // with single name
        assertThat(frInfos, hasSize(1));
        assertThat(frInfos, hasItems(eqSitemapInfo("/site/fr.sitemap.foobar-sitemap.xml", 100, 1)));
        // nested with multiple names
        assertThat(enNestedInfos, hasSize(3));
        assertThat(enNestedInfos, hasItems(
                eqSitemapInfo("/site/en.sitemap.news-foo-sitemap.xml", 100, 1),
                eqSitemapInfo("/site/en.sitemap.news-bar-sitemap.xml", 100, 1),
                // on-demand
                eqSitemapInfo("/site/en.sitemap.news-news-sitemap.xml", -1, -1)
        ));
        // nested => index and default self
        assertThat(enInfos, hasSize(2));
        assertThat(enInfos, hasItems(
                eqSitemapInfo("/site/en.sitemap-index.xml", -1, -1),
                eqSitemapInfo("/site/en.sitemap.xml", 100, 1)
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
