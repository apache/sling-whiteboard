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
package org.apache.sling.sitemap.impl.builder;

import org.apache.sling.sitemap.SitemapException;
import org.apache.sling.sitemap.impl.builder.extensions.ExtensionProviderManager;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith({SlingContextExtension.class})
public class SitemapImplTest {

    public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    public final SlingContext context = new SlingContext();

    private ExtensionProviderManager extensionManager;

    @BeforeEach
    public void setup() {
        extensionManager = context.registerInjectActivateService(new ExtensionProviderManager());
    }

    @Test
    public void testEmptySitemap() throws IOException {
        // given
        StringWriter writer = new StringWriter();
        SitemapImpl subject = new SitemapImpl(writer, extensionManager);

        //when
        subject.close();
        String sitemap = writer.toString();

        //then
        assertEquals(
                XML_HEADER + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"></urlset>",
                sitemap
        );
    }

    @Test
    public void testSitemapWithSingleLocation() throws SitemapException, IOException {
        // given
        StringWriter writer = new StringWriter();
        SitemapImpl subject = new SitemapImpl(writer, extensionManager);

        //when
        subject.addUrl("http://example.com");
        subject.close();
        String sitemap = writer.toString();

        //then
        assertEquals(
                XML_HEADER + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
                        + "<url><loc>http://example.com</loc></url>"
                        + "</urlset>",
                sitemap
        );
    }

    @Test
    public void testWriteAfterRead() throws SitemapException, IOException {
        // given
        StringWriter writer = new StringWriter();
        SitemapImpl subject = new SitemapImpl(writer, extensionManager);

        //when
        subject.addUrl("http://example.com");
        subject.writePendingUrl();
        String sitemap1 = writer.toString();
        subject.addUrl("http://example.de");
        subject.close();
        String sitemap2 = writer.toString();

        //then
        assertEquals(
                XML_HEADER + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
                        + "<url><loc>http://example.com</loc></url>",
                sitemap1
        );
        assertEquals(
                XML_HEADER + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
                        + "<url><loc>http://example.com</loc></url>"
                        + "<url><loc>http://example.de</loc></url>"
                        + "</urlset>",
                sitemap2
        );
    }

    @Test
    public void testIOExceptionWrappedInSitemapException() throws IOException {
        // given
        Writer throwingWriter = new Writer() {
            @Override
            public void write(@NotNull char[] cbuf, int off, int len) throws IOException {
                throw new IOException();
            }

            @Override
            public void flush() throws IOException {
                throw new IOException();
            }

            @Override
            public void close() throws IOException {
                throw new IOException();
            }
        };
        SitemapImpl sitemap = new SitemapImpl(throwingWriter, extensionManager, false);

        SitemapException ex = assertThrows(SitemapException.class, () -> {
            sitemap.addUrl("http://localhost:4502");
            sitemap.addUrl("http://localhost:4503"); // throws
        });
        assertThat(ex.getCause(), instanceOf(IOException.class));
    }
}
