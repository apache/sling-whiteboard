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
package org.apache.sling.sitemap.impl.builder.extensions;

import org.apache.sling.sitemap.SitemapException;
import org.apache.sling.sitemap.builder.Url;
import org.apache.sling.sitemap.builder.extensions.AlternateLanguageExtension;
import org.apache.sling.sitemap.impl.builder.SitemapImpl;
import org.apache.sling.sitemap.impl.builder.SitemapImplTest;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({SlingContextExtension.class})
public class AlternateLanguageExtensionTest {

    public final SlingContext context = new SlingContext();

    private ExtensionProviderManager extensionProviderManager;

    @BeforeEach
    public void setup() {
        context.registerInjectActivateService(new AlternateLanguageExtensionProvider());
        extensionProviderManager = context.registerInjectActivateService(new ExtensionProviderManager());
    }

    @Test
    public void testAlternateLanguageCombinations() throws SitemapException, IOException {
        // given
        StringWriter writer = new StringWriter();
        SitemapImpl sitemap = new SitemapImpl(writer, extensionProviderManager);

        // when
        Url url = sitemap.addUrl("http://example.ch/de.html");
        url.addExtension(AlternateLanguageExtension.class)
                .setLocale(Locale.forLanguageTag("fr-ch"))
                .setHref("http://example.ch/fr.html");
        url.addExtension(AlternateLanguageExtension.class)
                .setLocale(Locale.forLanguageTag("it-ch"))
                .setHref("http://example.ch/it.html");
        url.addExtension(AlternateLanguageExtension.class)
                .setDefaultLocale()
                .setHref("http://example.ch/language-chooser.html");
        sitemap.close();

        // then
        assertEquals(
                SitemapImplTest.XML_HEADER + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\" " +
                        "xmlns:xhtml=\"http://www.w3.org/1999/xhtml\">"
                        + "<url>"
                        + "<loc>http://example.ch/de.html</loc>"
                        + "<xhtml:link rel=\"alternate\" hreflang=\"fr-CH\" href=\"http://example.ch/fr.html\"/>"
                        + "<xhtml:link rel=\"alternate\" hreflang=\"it-CH\" href=\"http://example.ch/it.html\"/>"
                        + "<xhtml:link rel=\"alternate\" hreflang=\"x-default\" href=\"http://example.ch/language-chooser.html\"/>"
                        + "</url>"
                        + "</urlset>",
                writer.toString()
        );
    }

    @Test
    public void testNothingWrittenWhenExtensionMissesMandatoryProperties() throws SitemapException, IOException {
        // given
        StringWriter writer = new StringWriter();
        SitemapImpl sitemap = new SitemapImpl(writer, extensionProviderManager);

        // when
        Url url = sitemap.addUrl("http://example.ch/de.html");
        url.addExtension(AlternateLanguageExtension.class)
                .setLocale(Locale.forLanguageTag("fr-ch"));
        url.addExtension(AlternateLanguageExtension.class)
                .setHref("http://example.ch/it.html");
        sitemap.close();

        // then
        assertEquals(
                SitemapImplTest.XML_HEADER + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\" " +
                        "xmlns:xhtml=\"http://www.w3.org/1999/xhtml\">"
                        + "<url>"
                        + "<loc>http://example.ch/de.html</loc>"
                        + "</url>"
                        + "</urlset>",
                writer.toString()
        );
    }
}
