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
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

@ExtendWith({SlingContextExtension.class, MockitoExtension.class})
public class SitemapServiceImplTest {

    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);

    private final SitemapServiceImpl subject = new SitemapServiceImpl();
    private final SitemapStorage storage = new SitemapStorage();
    private final SitemapGeneratorManagerImpl generatorManager = new SitemapGeneratorManagerImpl();
    private final SitemapScheduler scheduler = new SitemapScheduler();
    private final SitemapServiceConfiguration sitemapServiceConfiguration = new SitemapServiceConfiguration();

    private TestGenerator generator = new TestGenerator() {
    };
    private TestGenerator newsGenerator = new TestGenerator() {
        @Override
        public @NotNull Set<String> getOnDemandNames(@NotNull Resource sitemapRoot) {
            return getNames(sitemapRoot);
        }
    };

    @Mock
    private ServiceUserMapped serviceUser;
    @Mock
    private JobManager jobManager;

    private Resource deRoot;
    private Resource enRoot;
    private Resource enFaqs;
    private Resource enNews;
    private Resource frRoot;
    private Resource itRoot;
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
        itRoot = context.create().resource("/content/site/it", Collections.singletonMap(
                SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.TRUE
        ));
        noRoot = context.create().resource("/content/site/nothing");

        context.registerService(ServiceUserMapped.class, serviceUser, "subServiceName", "sitemap-writer");
        context.registerService(ServiceUserMapped.class, serviceUser, "subServiceName", "sitemap-reader");
        context.registerService(SitemapGenerator.class, generator, "service.ranking", 1);
        context.registerService(SitemapGenerator.class, newsGenerator, "service.ranking", 2);
        context.registerService(JobManager.class, jobManager);
        context.registerInjectActivateService(sitemapServiceConfiguration);
        context.registerInjectActivateService(generatorManager);
        context.registerInjectActivateService(storage);
        context.registerInjectActivateService(scheduler,
                "scheduler.name", "default",
                "scheduler.expression", "never",
                "names", new String[] { SitemapService.DEFAULT_SITEMAP_NAME });
        context.registerInjectActivateService(subject);
    }

    @Test
    public void testSitemapIndexUrlReturned() {
        // given
        generator.setNames(SitemapService.DEFAULT_SITEMAP_NAME);
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
        assertThat(enInfo, hasSize(2));
        assertThat(enInfo, hasItems(
                eqSitemapInfo("/site/en.sitemap-index.xml", -1, -1, SitemapInfo.Status.ON_DEMAND),
                eqSitemapInfo("/site/en.sitemap.xml", -1, -1, SitemapInfo.Status.SCHEDULED)
        ));
        assertThat(frInfo, hasSize(3));
        assertThat(frInfo, hasItems(
                eqSitemapInfo("/site/fr.sitemap-index.xml", -1, -1, SitemapInfo.Status.ON_DEMAND),
                eqSitemapInfo("/site/fr.sitemap.a-sitemap.xml", -1, -1, SitemapInfo.Status.UNKNOWN),
                eqSitemapInfo("/site/fr.sitemap.a-sitemap.xml", -1, -1, SitemapInfo.Status.UNKNOWN)
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
        storage.writeSitemap(deRoot, SitemapService.DEFAULT_SITEMAP_NAME, new ByteArrayInputStream(new byte[0]), 1, 100,
                1);
        storage.writeSitemap(frRoot, "foobar", new ByteArrayInputStream(new byte[0]), 1, 100, 1);
        storage.writeSitemap(enRoot, SitemapService.DEFAULT_SITEMAP_NAME, new ByteArrayInputStream(new byte[0]), 1, 100,
                1);
        storage.writeSitemap(enNews, "foo", new ByteArrayInputStream(new byte[0]), 1, 100, 1);
        storage.writeSitemap(enNews, "bar", new ByteArrayInputStream(new byte[0]), 1, 100, 1);
        storage.writeSitemap(itRoot, SitemapService.DEFAULT_SITEMAP_NAME, new ByteArrayInputStream(new byte[0]), 1, 100,
                1);
        storage.writeSitemap(itRoot, SitemapService.DEFAULT_SITEMAP_NAME, new ByteArrayInputStream(new byte[0]), 2, 100,
                1);

        generator.setNames(deRoot, SitemapService.DEFAULT_SITEMAP_NAME);
        generator.setNames(frRoot, "foobar");
        generator.setNames(enNews, "foo", "bar");
        generator.setNames(enRoot, SitemapService.DEFAULT_SITEMAP_NAME);
        generator.setNames(itRoot, SitemapService.DEFAULT_SITEMAP_NAME);
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
        Collection<SitemapInfo> itInfos = subject.getSitemapInfo(itRoot);
        Collection<SitemapInfo> frInfos = subject.getSitemapInfo(frRoot);
        Collection<SitemapInfo> enInfos = subject.getSitemapInfo(enRoot);
        Collection<SitemapInfo> enNestedInfos = subject.getSitemapInfo(enNews);

        // no name
        assertThat(deInfos, hasSize(1));
        assertThat(deInfos, hasItems(eqSitemapInfo("/site/de.sitemap.xml", 100, 1)));
        // no name, multi file
        assertThat(itInfos, hasSize(3));
        assertThat(itInfos, hasItems(
                eqSitemapInfo("/site/it.sitemap-index.xml", -1, -1),
                eqSitemapInfo("/site/it.sitemap.xml", 100, 1),
                eqSitemapInfo("/site/it.sitemap.sitemap-2.xml", 100, 1)
        ));
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
        return eqSitemapInfo(url, size, entries, null);
    }

    private static Matcher<SitemapInfo> eqSitemapInfo(String url, int size, int entries, SitemapInfo.Status status) {
        return new CustomMatcher<SitemapInfo>("with url " + url + ", with size " + size + ", with entries " + entries) {
            @Override
            public boolean matches(Object o) {
                return o instanceof SitemapInfo &&
                        url.equals(((SitemapInfo) o).getUrl()) &&
                        size == ((SitemapInfo) o).getSize() &&
                        entries == ((SitemapInfo) o).getEntries() &&
                        (status == null || status.equals(((SitemapInfo) o).getStatus()));
            }
        };
    }
}
