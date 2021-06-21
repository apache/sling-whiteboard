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

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.apache.sling.sitemap.SitemapService;
import org.apache.sling.sitemap.generator.SitemapGenerator;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.hamcrest.CustomMatcher;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({SlingContextExtension.class, MockitoExtension.class})
public class SitemapStorageTest {

    public final SlingContext context = new SlingContext();

    private final SitemapStorage subject = new SitemapStorage();
    private final SitemapGeneratorManagerImpl generatorManager = new SitemapGeneratorManagerImpl();
    private final SitemapServiceConfiguration configuration = new SitemapServiceConfiguration();

    @Mock(lenient = true)
    private SitemapGenerator generator;
    @Mock
    private ServiceUserMapped serviceUser;

    @BeforeEach
    public void setup() {
        context.registerService(SitemapGenerator.class, generator);
        context.registerService(ServiceUserMapped.class, serviceUser, "subServiceName", "sitemap-writer");
        context.registerInjectActivateService(configuration);
        context.registerInjectActivateService(generatorManager);
        context.registerInjectActivateService(subject,
                "stateMaxAge", 100
        );

        when(generator.getNames(any())).thenReturn(Collections.singleton(SitemapService.DEFAULT_SITEMAP_NAME));
    }

    // Read/Write

    @Test
    public void testConsecutiveWriteOfStateUpdatesContent() throws IOException {
        // given
        Resource root = context.create().resource("/content/site/de", ImmutableMap.of(
                SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.TRUE
        ));

        // when
        subject.writeState(root, "foo", ImmutableMap.of("i", 1));
        subject.writeState(root, "foo", ImmutableMap.of("i", 2));

        // then
        ValueMap properties = subject.getState(root, "foo");
        assertEquals(2, properties.get("i", Integer.class));
    }

    @Test
    public void testConsecutiveWriteOfSitemapUpdatesContent() throws IOException {
        // given
        Resource root = context.create().resource("/content/site/de", ImmutableMap.of(
                "sitemapRoot", Boolean.TRUE
        ));

        // when
        subject.writeSitemap(root, "foo", new ByteArrayInputStream(new byte[]{0x01}), 1, 1, 0);
        subject.writeSitemap(root, "foo", new ByteArrayInputStream(new byte[]{0x01, 0x02}), 1, 2, 0);

        // then
        assertResourceDataEquals(
                new String(new byte[]{0x01, 0x02}, StandardCharsets.US_ASCII),
                context.resourceResolver().getResource("/var/sitemaps/content/site/de/foo-sitemap.xml")
        );
    }

    @Test
    public void testListSitemapsReturnsOnlySitemaps() throws IOException {
        // given
        Resource root = context.create().resource("/content/site/de", ImmutableMap.of(
                SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.TRUE
        ));

        // when
        subject.writeState(root, "foo", ImmutableMap.of("i", 1));
        subject.writeState(root, "bar", ImmutableMap.of("i", 1));
        subject.writeSitemap(root, "foobar", new ByteArrayInputStream(new byte[0]), 1, 0, 0);
        subject.writeSitemap(root, SitemapService.DEFAULT_SITEMAP_NAME, new ByteArrayInputStream(new byte[0]), 1, 0, 0);

        // then
        Collection<SitemapStorageInfo> sitemapNames = subject.getSitemaps(root);
        assertThat(sitemapNames, hasSize(2));
        assertThat(sitemapNames, hasItems(storageInfo("foobar-sitemap"), storageInfo("sitemap")));
    }

    // Cleanup

    @Test
    public void testStateExpires() throws InterruptedException, IOException {
        // given
        Resource root = context.create().resource("/content/site/de", ImmutableMap.of(
                SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.TRUE
        ));

        // when
        subject.writeState(root, "foo", ImmutableMap.of("i", 1));
        Thread.sleep(100);

        // then
        ValueMap properties = subject.getState(root, "foo");
        assertNull(properties.get("i", Integer.class));
    }


    @Test
    public void testCleanupExpiredStates() throws Exception {
        // given
        Resource root = context.create().resource("/content/site/de", ImmutableMap.of(
                SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.TRUE
        ));

        // when
        subject.writeState(root, SitemapService.DEFAULT_SITEMAP_NAME, ImmutableMap.of("i", 1));

        // then
        assertNotNull(context.resourceResolver().getResource("/var/sitemaps/content/site/de/sitemap.part"));

        // and when
        Thread.sleep(100);
        subject.run();

        // then
        assertNull(context.resourceResolver().getResource("/var/sitemaps/content/site/de/sitemap.part"));
    }

    @Test
    public void testCleanupObsoleteSitemapsAfterTopLevelChanged() throws Exception {
        // given
        Resource newRoot = context.create().resource("/content/site/ch");
        Resource initialRoot = context.create().resource("/content/site/ch/de-ch", ImmutableMap.of(
                SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.TRUE
        ));

        // when
        subject.writeSitemap(initialRoot, SitemapService.DEFAULT_SITEMAP_NAME, new ByteArrayInputStream(new byte[0]), 1,
                0, 0);
        subject.writeSitemap(initialRoot, SitemapService.DEFAULT_SITEMAP_NAME, new ByteArrayInputStream(new byte[0]), 2,
                0, 0);

        // then
        assertNotNull(context.resourceResolver().getResource("/var/sitemaps/content/site/ch/de-ch/sitemap.xml"));
        assertNotNull(context.resourceResolver().getResource("/var/sitemaps/content/site/ch/de-ch/sitemap-2.xml"));

        // and when
        newRoot.adaptTo(ModifiableValueMap.class).put(SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.TRUE);
        context.resourceResolver().commit();
        subject.run();

        // then
        assertNull(context.resourceResolver().getResource("/var/sitemaps/content/site/ch/de-ch/sitemap.xml"));
        assertNull(context.resourceResolver().getResource("/var/sitemaps/content/site/ch/de-ch/sitemap-2.xml"));
    }

    @Test
    public void testCleanupObsoleteSitemapsAfterNestedSitemapRootChanged() throws Exception {
        // given
        Resource root = context.create().resource("/content/site/de", ImmutableMap.of(
                SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.TRUE
        ));
        Resource news = context.create().resource("/content/site/de/news", ImmutableMap.of(
                SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.TRUE
        ));

        // when
        subject.writeSitemap(root, SitemapService.DEFAULT_SITEMAP_NAME, new ByteArrayInputStream(new byte[0]), 1, 0, 0);
        subject.writeSitemap(news, SitemapService.DEFAULT_SITEMAP_NAME, new ByteArrayInputStream(new byte[0]), 1, 0, 0);

        // then
        assertNotNull(context.resourceResolver().getResource("/var/sitemaps/content/site/de/sitemap.xml"));
        assertNotNull(context.resourceResolver().getResource("/var/sitemaps/content/site/de/news-sitemap.xml"));

        // and when
        news.adaptTo(ModifiableValueMap.class).put(SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.FALSE);
        context.resourceResolver().commit();
        subject.run();

        // then
        assertNotNull(context.resourceResolver().getResource("/var/sitemaps/content/site/de/sitemap.xml"));
        assertNull(context.resourceResolver().getResource("/var/sitemaps/content/site/de/news-sitemap.xml"));
    }

    // Eventing

    @Test
    public void testNoPurgeEventSentOnStateCleanup() throws IOException, InterruptedException {
        // given
        List<Event> capturedEvents = new ArrayList<>();
        Resource root = context.create().resource("/content/site/de", ImmutableMap.of(
                SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.TRUE
        ));
        context.registerService(EventHandler.class, capturedEvents::add, EventConstants.EVENT_TOPIC, new String[]{
                SitemapGenerator.EVENT_TOPIC_SITEMAP_UPDATED,
                SitemapGenerator.EVENT_TOPIC_SITEMAP_PURGED
        });

        // when
        subject.writeState(root, "foo", Collections.emptyMap());
        Thread.sleep(100);
        subject.run();

        // then
        assertThat(capturedEvents, hasSize(0));
    }

    @Test
    public void testUpdatedAndPurgeEventSentOnSitemapWriteCleanup() throws IOException, InterruptedException {
        // given
        List<Event> capturedEvents = new ArrayList<>();
        Resource root = context.create().resource("/content/site/de", ImmutableMap.of(
                SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.TRUE
        ));
        context.registerService(EventHandler.class, capturedEvents::add, EventConstants.EVENT_TOPIC, new String[]{
                SitemapGenerator.EVENT_TOPIC_SITEMAP_UPDATED,
                SitemapGenerator.EVENT_TOPIC_SITEMAP_PURGED
        });

        // when
        String storagePath = subject.writeSitemap(root, "foo", new ByteArrayInputStream(new byte[]{0x00}), 1, 0, 0);
        context.resourceResolver().delete(root);
        context.resourceResolver().commit();
        Thread.sleep(100);
        subject.run();

        // then
        System.out.println(capturedEvents.stream().map(Event::toString).collect(Collectors.joining(",")));
        assertThat(capturedEvents, hasSize(2));
        assertThat(capturedEvents, hasItems(
                sitemapEvent(SitemapGenerator.EVENT_TOPIC_SITEMAP_UPDATED, storagePath),
                sitemapEvent(SitemapGenerator.EVENT_TOPIC_SITEMAP_PURGED, storagePath)
        ));
    }

    @Test
    public void testUpdatedAndPurgeEventSentOnSitemapWriteDelete() throws IOException, InterruptedException {
        // given
        List<Event> capturedEvents = new ArrayList<>();
        Resource root = context.create().resource("/content/site/de", ImmutableMap.of(
                SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.TRUE
        ));
        context.registerService(EventHandler.class, capturedEvents::add, EventConstants.EVENT_TOPIC, new String[]{
                SitemapGenerator.EVENT_TOPIC_SITEMAP_UPDATED,
                SitemapGenerator.EVENT_TOPIC_SITEMAP_PURGED
        });

        // when
        Thread.sleep(100);
        String storagePath = subject.writeSitemap(root, "foo", new ByteArrayInputStream(new byte[]{0x00}), 1, 0, 0);
        subject.deleteSitemaps(root, "foo", info -> true);
        Thread.sleep(100);

        // then
        assertThat(capturedEvents, hasSize(2));
        assertThat(capturedEvents, hasItems(
                sitemapEvent(SitemapGenerator.EVENT_TOPIC_SITEMAP_UPDATED, storagePath),
                sitemapEvent(SitemapGenerator.EVENT_TOPIC_SITEMAP_PURGED, storagePath)
        ));
    }

    static void assertResourceDataEquals(String expectedValue, Resource resource) throws IOException {
        assertNotNull(resource);
        InputStream inputStream = resource.adaptTo(InputStream.class);
        if (inputStream == null) {
            inputStream = resource.getValueMap().get("jcr:data", InputStream.class);
            if (inputStream == null) {
                Resource content = resource.getChild("jcr:content");
                inputStream = content != null ? content.getValueMap().get("jcr:data", InputStream.class) : null;
            }
        }
        assertNotNull(inputStream);
        StringWriter sitemap = new StringWriter();
        IOUtils.copy(inputStream, sitemap, StandardCharsets.UTF_8);
        assertEquals(expectedValue, sitemap.toString());
    }

    private static Matcher<Event> sitemapEvent(String topic, String storagePath) {
        return new CustomMatcher<Event>("Event with storagePath property set to " + storagePath) {
            @Override
            public boolean matches(Object o) {
                return o instanceof Event
                        && storagePath.equals(
                        ((Event) o).getProperty(SitemapGenerator.EVENT_PROPERTY_SITEMAP_STORAGE_PATH));
            }
        };
    }

    private static Matcher<SitemapStorageInfo> storageInfo(String name) {
        return new CustomMatcher<SitemapStorageInfo>("SitemapStorageInfo with name='" + name + "'") {
            @Override
            public boolean matches(Object o) {
                return o instanceof SitemapStorageInfo && ((SitemapStorageInfo) o).getSitemapSelector().equals(name);
            }
        };
    }
}
