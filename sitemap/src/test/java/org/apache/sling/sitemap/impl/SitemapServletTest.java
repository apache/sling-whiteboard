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
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.apache.sling.sitemap.SitemapService;
import org.apache.sling.sitemap.TestResourceTreeSitemapGenerator;
import org.apache.sling.sitemap.generator.SitemapGenerator;
import org.apache.sling.sitemap.impl.builder.SitemapImplTest;
import org.apache.sling.sitemap.impl.builder.extensions.ExtensionProviderManager;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.apache.sling.testing.mock.sling.servlet.MockRequestPathInfo;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.ServletException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith({SlingContextExtension.class, MockitoExtension.class})
public class SitemapServletTest {

    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);

    private final SitemapServlet subject = new SitemapServlet();
    private final SitemapStorage storage = spy(new SitemapStorage());
    private final SitemapServiceConfiguration sitemapServiceConfiguration = new SitemapServiceConfiguration();
    private final SitemapGeneratorManagerImpl generatorManager = new SitemapGeneratorManagerImpl();
    private final ExtensionProviderManager extensionProviderManager = new ExtensionProviderManager();

    private TestResourceTreeSitemapGenerator generator = new TestResourceTreeSitemapGenerator() {
        @Override
        public @NotNull Set<String> getNames(@NotNull Resource sitemapRoot) {
            return ImmutableSet.of(SitemapService.DEFAULT_SITEMAP_NAME);
        }
    };
    private TestResourceTreeSitemapGenerator newsGenerator = new TestResourceTreeSitemapGenerator() {
        @Override
        public @NotNull Set<String> getNames(@NotNull Resource sitemapRoot) {
            return ImmutableSet.of("news");
        }
    };

    @Mock
    private JobManager jobManager;
    @Mock
    private ServiceUserMapped serviceUser;
    private final Calendar pointInTime;
    private final String pointInTimeAtUtc;
    private Resource root;

    public SitemapServletTest() {
        pointInTime = Calendar.getInstance();
        pointInTime.setTimeInMillis(123456789L);
        pointInTimeAtUtc = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(pointInTime.toInstant().atOffset(ZoneOffset.UTC));
    }

    @BeforeEach
    public void setup() {
        root = context.create().resource("/content/site/de");
        context.create().resource("/content/site/de/jcr:content", Collections.singletonMap(
                SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.TRUE));

        context.registerService(ServiceUserMapped.class, serviceUser, "subServiceName", "sitemap-writer");
        context.registerService(SitemapGenerator.class, generator);
        context.registerService(SitemapGenerator.class, newsGenerator);
        context.registerService(JobManager.class, jobManager);
        context.registerInjectActivateService(extensionProviderManager);
        context.registerInjectActivateService(sitemapServiceConfiguration);
        context.registerInjectActivateService(generatorManager);
        context.registerInjectActivateService(storage);
        context.registerInjectActivateService(subject);

        // overrule the getLastModified() of all SitemapStorageInfos to get predictable behaviour
        doAnswer(inv -> ((Collection<SitemapStorageInfo>) inv.callRealMethod()).stream()
                .map(info -> {
                    info = spy(info);
                    when(info.getLastModified()).thenReturn(pointInTime);
                    return info;
                })
                .collect(Collectors.toCollection(LinkedHashSet::new))
        ).when(storage).getSitemaps(any());

        MockJcr.setQueryResult(
                context.resourceResolver().adaptTo(Session.class),
                Collections.singletonList(root.adaptTo(Node.class))
        );
    }

    @Test
    public void testBadRequestForResourcesThatAreNotSitemapRoot() throws ServletException, IOException {
        // given
        Resource notATopLevelRoot = context.create().resource("/content/site/en");

        MockSlingHttpServletRequest request = newSitemapIndexReq(notATopLevelRoot);
        MockSlingHttpServletResponse response = context.response();

        // when
        subject.doGet(request, response);

        // then
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testBadRequestForInvalidSelectors() throws ServletException, IOException {
        // given
        MockSlingHttpServletResponse response = context.response();

        // when, then
        subject.doGet(newReq("sitemap-index.somethingelse", root), response);
        assertEquals(400, response.getStatus());
        response = context.response();
        subject.doGet(newReq("sitemap.something.else", root), response);
        assertEquals(400, response.getStatus());
        response = context.response();
        subject.doGet(newReq("unknown", root), response);
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testSitemapIndexContainsOnlySitemapsFromStorage() throws IOException, ServletException {
        // given
        storage.writeSitemap(root, SitemapService.DEFAULT_SITEMAP_NAME, new ByteArrayInputStream(new byte[0]), 1, 0, 0);
        storage.writeSitemap(root, "news", new ByteArrayInputStream(new byte[0]), 1, 0, 0);

        MockSlingHttpServletRequest request = newSitemapIndexReq(root);
        MockSlingHttpServletResponse response = context.response();

        // when
        subject.doGet(request, response);

        // then
        assertEquals(200, response.getStatus());
        assertEquals("application/xml;charset=utf-8", response.getContentType());
        assertEquals("utf-8", response.getCharacterEncoding());
        assertEquals(
                SitemapImplTest.XML_HEADER + "<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
                        + "<sitemap><loc>/site/de.sitemap.xml</loc><lastmod>" + pointInTimeAtUtc + "</lastmod></sitemap>"
                        + "<sitemap><loc>/site/de.sitemap.news-sitemap.xml</loc><lastmod>" + pointInTimeAtUtc + "</lastmod></sitemap>"
                        + "</sitemapindex>",
                response.getOutputAsString()
        );
    }

    @Test
    public void testSitemapIndexContainsMultiFileSitemaps() throws IOException, ServletException {
        // given
        storage.writeSitemap(root, SitemapService.DEFAULT_SITEMAP_NAME, new ByteArrayInputStream(new byte[0]), 1, 0, 0);
        storage.writeSitemap(root, SitemapService.DEFAULT_SITEMAP_NAME, new ByteArrayInputStream(new byte[0]), 2, 0, 0);
        storage.writeSitemap(root, SitemapService.DEFAULT_SITEMAP_NAME, new ByteArrayInputStream(new byte[0]), 3, 0, 0);

        MockSlingHttpServletRequest request = newSitemapIndexReq(root);
        MockSlingHttpServletResponse response = context.response();

        // when
        subject.doGet(request, response);

        // then
        assertEquals(200, response.getStatus());
        assertEquals("application/xml;charset=utf-8", response.getContentType());
        assertEquals("utf-8", response.getCharacterEncoding());
        assertEquals(
                SitemapImplTest.XML_HEADER + "<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
                        + "<sitemap><loc>/site/de.sitemap.xml</loc><lastmod>" + pointInTimeAtUtc + "</lastmod></sitemap>"
                        + "<sitemap><loc>/site/de.sitemap.sitemap-2.xml</loc><lastmod>" + pointInTimeAtUtc + "</lastmod></sitemap>"
                        + "<sitemap><loc>/site/de.sitemap.sitemap-3.xml</loc><lastmod>" + pointInTimeAtUtc + "</lastmod></sitemap>"
                        + "</sitemapindex>",
                response.getOutputAsString()
        );
    }

    @Test
    public void testSitemapIndexContainsOnDemandSitemapsAndSitemapsFromStorage() throws ServletException, IOException {
        // given
        storage.writeSitemap(root, SitemapService.DEFAULT_SITEMAP_NAME, new ByteArrayInputStream(new byte[0]), 1, 0, 0);

        MockSlingHttpServletRequest request = newSitemapIndexReq(root);
        MockSlingHttpServletResponse response = context.response();
        newsGenerator.setServeOnDemand(true);

        // when
        subject.doGet(request, response);

        // then
        assertEquals(200, response.getStatus());
        assertEquals("application/xml;charset=utf-8", response.getContentType());
        assertEquals("utf-8", response.getCharacterEncoding());
        assertEquals(
                SitemapImplTest.XML_HEADER + "<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
                        + "<sitemap><loc>/site/de.sitemap.news-sitemap.xml</loc></sitemap>"
                        + "<sitemap><loc>/site/de.sitemap.xml</loc><lastmod>" + pointInTimeAtUtc + "</lastmod></sitemap>"
                        + "</sitemapindex>",
                response.getOutputAsString()
        );
    }

    @Test
    public void testSitemapServedOnDemand() throws ServletException, IOException {
        // given
        MockSlingHttpServletRequest request = newSitemapReq("news-sitemap", root);
        MockSlingHttpServletResponse response = context.response();
        newsGenerator.setServeOnDemand(true);

        // when
        subject.doGet(request, response);

        // then
        assertEquals(200, response.getStatus());
        assertEquals("application/xml;charset=utf-8", response.getContentType());
        assertEquals("utf-8", response.getCharacterEncoding());
        assertEquals(
                SitemapImplTest.XML_HEADER + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
                        + "<url><loc>/content/site/de</loc></url>"
                        + "</urlset>",
                response.getOutputAsString()
        );
    }

    @Test
    public void testSitemapServedFromStorage() throws ServletException, IOException {
        // given
        String expectedOutcome = SitemapImplTest.XML_HEADER + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
                + "<url><loc>/content/site/en</loc></url>"
                + "</urlset>";
        byte[] expectedOutcomeBytes = expectedOutcome.getBytes(StandardCharsets.UTF_8);

        storage.writeSitemap(root, "foo", new ByteArrayInputStream(expectedOutcomeBytes), 1,
                expectedOutcomeBytes.length, 1);

        MockSlingHttpServletRequest request = newSitemapReq("foo-sitemap", root);
        MockSlingHttpServletResponse response = context.response();

        // when
        subject.doGet(request, response);

        // then
        assertEquals(200, response.getStatus());
        assertEquals("application/xml;charset=utf-8", response.getContentType());
        assertEquals("utf-8", response.getCharacterEncoding());
        assertEquals(
                expectedOutcome,
                response.getOutputAsString()
        );
    }

    @Test
    public void testMultiFileSitemapServedFromStorage() throws ServletException, IOException {
        // given
        String expectedOutcome = SitemapImplTest.XML_HEADER + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
                + "<url><loc>/content/site/en</loc></url>"
                + "</urlset>";
        byte[] expectedOutcomeBytes = expectedOutcome.getBytes(StandardCharsets.UTF_8);

        storage.writeSitemap(root, "foo", new ByteArrayInputStream(expectedOutcomeBytes), 2,
                expectedOutcomeBytes.length, 1);

        MockSlingHttpServletRequest request = newSitemapReq("foo-sitemap-2", root);
        MockSlingHttpServletResponse response = context.response();

        // when
        subject.doGet(request, response);

        // then
        assertEquals(200, response.getStatus());
        assertEquals("application/xml;charset=utf-8", response.getContentType());
        assertEquals("utf-8", response.getCharacterEncoding());
        assertEquals(
                expectedOutcome,
                response.getOutputAsString()
        );
    }

    @Test
    public void testSitemapNotServed() throws ServletException, IOException {
        // given
        MockSlingHttpServletRequest request = newSitemapReq("sitemap", root);
        MockSlingHttpServletResponse response = context.response();

        // when
        subject.doGet(request, response);

        // then
        assertEquals(404, response.getStatus());
    }

    private MockSlingHttpServletRequest newSitemapReq(String sitemapFileName, Resource resource) {
        return newReq("sitemap." + sitemapFileName, resource);
    }

    private MockSlingHttpServletRequest newSitemapIndexReq(Resource resource) {
        return newReq("sitemap-index", resource);
    }

    private MockSlingHttpServletRequest newReq(String selectors, Resource resource) {
        MockSlingHttpServletRequest req = new MockSlingHttpServletRequest(context.resourceResolver(), context.bundleContext()) {
            @Override
            protected @NotNull MockRequestPathInfo newMockRequestPathInfo() {
                MockRequestPathInfo pathInfo = super.newMockRequestPathInfo();
                pathInfo.setSelectorString(selectors);
                return pathInfo;
            }
        };
        req.setResource(resource);

        return req;
    }
}
