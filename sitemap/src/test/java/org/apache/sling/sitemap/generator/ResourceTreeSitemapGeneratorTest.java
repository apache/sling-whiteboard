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
package org.apache.sling.sitemap.generator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.sitemap.SitemapException;
import org.apache.sling.sitemap.SitemapService;
import org.apache.sling.sitemap.TestResourceTreeSitemapGenerator;
import org.apache.sling.sitemap.impl.builder.extensions.ExtensionProviderManager;
import org.apache.sling.sitemap.impl.builder.SitemapImpl;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.StringWriter;

import static org.apache.sling.sitemap.impl.builder.SitemapImplTest.XML_HEADER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith({SlingContextExtension.class, MockitoExtension.class})
public class ResourceTreeSitemapGeneratorTest {

    public final SlingContext context = new SlingContext();

    private final SitemapGenerator subject = new TestResourceTreeSitemapGenerator();
    private final ExtensionProviderManager extensionProviderManager = new ExtensionProviderManager();

    @Mock
    private SitemapGenerator.GenerationContext generationContext;
    private Resource sitemapRoot;

    @BeforeEach
    public void setup() {
        sitemapRoot = context.load()
                .json("/ResourceTreeSitemapGeneratorTest/sitetree.json", "/content/site/de");

        context.registerInjectActivateService(extensionProviderManager);
    }

    @Test
    public void testSitemapContainsAllResourcesButJcrContent() throws SitemapException, IOException {
        // given
        StringWriter writer = new StringWriter();
        SitemapImpl sitemap = new SitemapImpl(writer, extensionProviderManager);

        // when
        subject.generate(sitemapRoot, SitemapService.DEFAULT_SITEMAP_NAME, sitemap, generationContext);
        sitemap.close();

        // then
        assertEquals(
                XML_HEADER + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
                        + "<url><loc>/content/site/de</loc></url>"
                        + "<url><loc>/content/site/de/child1</loc></url>"
                        + "<url><loc>/content/site/de/child1/grandchild11</loc></url>"
                        + "<url><loc>/content/site/de/child2</loc></url>"
                        + "</urlset>",
                writer.toString()
        );
    }

    @Test
    public void testSitemapDoesNotContainsResourcesWithoutJcrContent() throws SitemapException, IOException {
        // given
        StringWriter writer = new StringWriter();
        SitemapImpl sitemap = new SitemapImpl(writer, extensionProviderManager);
        context.create().resource("/content/site/de/child3");

        // when
        subject.generate(sitemapRoot, SitemapService.DEFAULT_SITEMAP_NAME, sitemap, generationContext);
        sitemap.close();

        // then
        assertEquals(
                XML_HEADER + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
                        + "<url><loc>/content/site/de</loc></url>"
                        + "<url><loc>/content/site/de/child1</loc></url>"
                        + "<url><loc>/content/site/de/child1/grandchild11</loc></url>"
                        + "<url><loc>/content/site/de/child2</loc></url>"
                        + "</urlset>",
                writer.toString()
        );
    }

    @Test
    public void testSkipTo() throws SitemapException, IOException {
        // given
        StringWriter writer = new StringWriter();
        SitemapImpl sitemap = new SitemapImpl(writer, extensionProviderManager);
        when(generationContext.getProperty("lastPath", String.class)).thenReturn("/content/site/de/child2/grandchild21");
        context.create().resource("/content/site/de/child2/grandchild21");
        context.create().resource("/content/site/de/child2/grandchild21/jcr:content");
        context.create().resource("/content/site/de/child2/grandchild22");
        context.create().resource("/content/site/de/child2/grandchild22/jcr:content");
        context.create().resource("/content/site/de/child3");
        context.create().resource("/content/site/de/child3/jcr:content");
        context.create().resource("/content/site/de/child3/grandchild31");
        context.create().resource("/content/site/de/child3/grandchild31/jcr:content");

        // when
        subject.generate(sitemapRoot, SitemapService.DEFAULT_SITEMAP_NAME, sitemap, generationContext);
        sitemap.close();

        // then
        assertEquals(
                XML_HEADER + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
                        + "<url><loc>/content/site/de/child2/grandchild21</loc></url>"
                        + "<url><loc>/content/site/de/child2/grandchild22</loc></url>"
                        + "<url><loc>/content/site/de/child3</loc></url>"
                        + "<url><loc>/content/site/de/child3/grandchild31</loc></url>"
                        + "</urlset>",
                writer.toString()
        );
    }

}
