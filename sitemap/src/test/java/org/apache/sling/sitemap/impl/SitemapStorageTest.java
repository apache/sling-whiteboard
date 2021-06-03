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
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.apache.sling.sitemap.generator.SitemapGenerator;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.hamcrest.CustomMatcher;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({SlingContextExtension.class, MockitoExtension.class})
public class SitemapStorageTest {

    public final SlingContext context = new SlingContext();

    private final SitemapStorage subject = new SitemapStorage();

    @Mock
    private ServiceUserMapped serviceUser;

    @BeforeEach
    public void setup() {
        context.registerService(ServiceUserMapped.class, serviceUser, "subServiceName", "sitemap-writer");
        context.registerInjectActivateService(subject,
                "stateMaxAge", 100
        );
    }

    @Test
    public void testConsecutiveWriteOfStateUpdatesContent() throws IOException {
        // given
        Resource root = context.create().resource("/content/site/de", ImmutableMap.of(
                "sitemapRoot", Boolean.TRUE
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
        subject.writeSitemap(root, "foo", new ByteArrayInputStream(new byte[]{0x01}), 1, 0);
        subject.writeSitemap(root, "foo", new ByteArrayInputStream(new byte[]{0x01, 0x02}), 2, 0);

        // then
        assertResourceDataEquals(
                new String(new byte[]{0x01, 0x02}, StandardCharsets.US_ASCII),
                context.resourceResolver().getResource("/var/sitemaps/content/site/de/foo-sitemap.xml")
        );
    }

    @Test
    public void testStateExpires() throws InterruptedException, IOException {
        // given
        Resource root = context.create().resource("/content/site/de", ImmutableMap.of(
                "sitemapRoot", Boolean.TRUE
        ));

        // when
        subject.writeState(root, "foo", ImmutableMap.of("i", 1));
        Thread.sleep(100);

        // then
        ValueMap properties = subject.getState(root, "foo");
        assertNull(properties.get("i", Integer.class));
    }

    @Test
    public void testListSitemapsReturnsOnlySitemaps() throws IOException {
        // given
        Resource root = context.create().resource("/content/site/de", ImmutableMap.of(
                "sitemapRoot", Boolean.TRUE
        ));

        // when
        subject.writeState(root, "foo", ImmutableMap.of("i", 1));
        subject.writeState(root, "bar", ImmutableMap.of("i", 1));
        subject.writeSitemap(root, "foobar", new ByteArrayInputStream(new byte[0]), 0, 0);
        subject.writeSitemap(root, SitemapGenerator.DEFAULT_SITEMAP, new ByteArrayInputStream(new byte[0]), 0, 0);

        // then
        Set<SitemapStorageInfo> sitemapNames = subject.getSitemaps(root);
        assertThat(sitemapNames, hasSize(2));
        assertThat(sitemapNames, hasItems(storageInfo("foobar-sitemap"), storageInfo("sitemap")));
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

    private static Matcher<SitemapStorageInfo> storageInfo(String name) {
        return new CustomMatcher<SitemapStorageInfo>("SitemapStorageInfo with name='" + name + "'") {
            @Override
            public boolean matches(Object o) {
                return o instanceof SitemapStorageInfo && ((SitemapStorageInfo) o).getSitemapSelector().equals(name);
            }
        };
    }
}
