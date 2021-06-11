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
import org.apache.sling.event.jobs.consumer.JobExecutionContext;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.apache.sling.sitemap.SitemapException;
import org.apache.sling.sitemap.SitemapService;
import org.apache.sling.sitemap.generator.SitemapGenerator;
import org.apache.sling.sitemap.builder.Sitemap;
import org.apache.sling.sitemap.impl.builder.extensions.ExtensionProviderManager;
import org.apache.sling.sitemap.impl.builder.SitemapImpl;
import org.apache.sling.sitemap.impl.builder.SitemapImplTest;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

import static org.apache.sling.sitemap.impl.SitemapStorageTest.assertResourceDataEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith({SlingContextExtension.class, MockitoExtension.class})
public class SitemapGeneratorExecutorTest {

    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);

    private final SitemapGeneratorExecutor subject = new SitemapGeneratorExecutor();
    private final SitemapGeneratorManagerImpl generatorManager = new SitemapGeneratorManagerImpl();
    private final ExtensionProviderManager extensionProviderManager = new ExtensionProviderManager();
    private final SitemapStorage storage = spy(new SitemapStorage());
    private final SitemapServiceImpl sitemapService = new SitemapServiceImpl();

    @Mock
    private JobManager jobManager;
    @Mock
    private ServiceUserMapped serviceUser;
    @Mock
    private Job job;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private JobExecutionContext executionContext;
    @Mock
    private SitemapGenerator generator;

    private Resource rootResource;
    private Resource storageRoot;

    @BeforeEach
    public void setup() {
        rootResource = context.create().resource("/content/site/de", Collections.singletonMap(
                SitemapService.PROPERTY_SITEMAP_ROOT, Boolean.TRUE
        ));
        storageRoot = context.create().resource("/var/sitemaps");

        context.registerService(ServiceUserMapped.class, serviceUser, "subServiceName", "sitemap-reader");
        context.registerService(ServiceUserMapped.class, serviceUser, "subServiceName", "sitemap-writer");
        context.registerService(SitemapGenerator.class, generator);
        context.registerService(JobManager.class, jobManager);
        context.registerInjectActivateService(generatorManager);
        context.registerInjectActivateService(storage);
        context.registerInjectActivateService(extensionProviderManager);
        context.registerInjectActivateService(sitemapService);

        when(job.getProperty(SitemapGeneratorExecutor.JOB_PROPERTY_SITEMAP_NAME, SitemapGenerator.DEFAULT_SITEMAP))
                .thenReturn(SitemapGenerator.DEFAULT_SITEMAP);
        when(job.getProperty(SitemapGeneratorExecutor.JOB_PROPERTY_SITEMAP_ROOT, String.class))
                .thenReturn(rootResource.getPath());
    }

    @Test
    public void testNoStateWrittenOnLargerChunkSize() throws IOException {
        context.registerService(SitemapGenerator.class, new FailOnceGenerator(Integer.MAX_VALUE,
                "http://example.com/page1.html",
                "http://example.com/page2.html",
                "http://example.com/page3.html",
                "http://example.com/page4.html",
                "http://example.com/page5.html"
        ));
        context.registerInjectActivateService(subject, "chunkSize", 10);

        // when
        subject.process(job, executionContext);

        // then
        assertResourceDataEquals(
                SitemapImplTest.XML_HEADER + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
                        + "<url><loc>http://example.com/page1.html</loc></url>"
                        + "<url><loc>http://example.com/page2.html</loc></url>"
                        + "<url><loc>http://example.com/page3.html</loc></url>"
                        + "<url><loc>http://example.com/page4.html</loc></url>"
                        + "<url><loc>http://example.com/page5.html</loc></url>"
                        + "</urlset>",
                storageRoot.getChild("content/site/de/sitemap.xml")
        );
        verify(storage, never()).writeState(any(), any(), any());
    }

    @Test
    public void testJobResumesAfterBeingAborted() throws IOException {
        // given
        context.registerService(SitemapGenerator.class, new FailOnceGenerator(4,
                "http://example.com/page1.html",
                "http://example.com/page2.html",
                "http://example.com/page3.html",
                "http://example.com/page4.html",
                "http://example.com/page5.html"
        ));
        context.registerInjectActivateService(subject, "chunkSize", 3);

        // when
        try {
            subject.process(job, executionContext);
        } catch (RuntimeException ex) {
            // ignore exception from FailOnceGenerator
        }

        // then
        assertResourceDataEquals(
                SitemapImplTest.XML_HEADER + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
                        + "<url><loc>http://example.com/page1.html</loc></url>"
                        + "<url><loc>http://example.com/page2.html</loc></url>"
                        + "<url><loc>http://example.com/page3.html</loc></url>",
                storageRoot.getChild("content/site/de/sitemap.part")
        );

        // and when (resume)
        subject.process(job, executionContext);

        // then
        assertNull(storageRoot.getChild("content/site/de/sitemap.part"));
        assertResourceDataEquals(
                SitemapImplTest.XML_HEADER + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
                        + "<url><loc>http://example.com/page1.html</loc></url>"
                        + "<url><loc>http://example.com/page2.html</loc></url>"
                        + "<url><loc>http://example.com/page3.html</loc></url>"
                        + "<url><loc>http://example.com/page4.html</loc></url>"
                        + "<url><loc>http://example.com/page5.html</loc></url>"
                        + "</urlset>",
                storageRoot.getChild("content/site/de/sitemap.xml")
        );
    }

    @Test
    public void testGenerationContextDoesNotLeakRawJcrData() throws IOException {
        // given
        String injectedText = "foobar";
        context.registerService(SitemapGenerator.class, new FailOnceGenerator(1,
                "http://example.com/page1.html",
                "http://example.com/page2.html"
        ) {
            @Override
            public void generate(@NotNull Resource sitemapRoot, @NotNull String name, @NotNull Sitemap sitemap, @NotNull GenerationContext context) throws SitemapException {
                try {
                    super.generate(sitemapRoot, name, sitemap, context);
                } finally {
                    // try to get jcr:data
                    assertNull(context.getProperty("jcr:data", InputStream.class));
                    Object mark = new Object();
                    assertEquals(mark, context.getProperty("jcr:data", mark));
                    // try to write jcr:data
                    context.setProperty("jcr:data", new ByteArrayInputStream(injectedText.getBytes(StandardCharsets.UTF_8)));
                }
            }
        });
        context.registerInjectActivateService(subject, "chunkSize", 1);

        // when
        try {
            subject.process(job, executionContext);
        } catch (RuntimeException ex) {
            // ignore exception from FailOnceGenerator
        }
        subject.process(job, executionContext);

        assertResourceDataEquals(
                SitemapImplTest.XML_HEADER + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
                        + "<url><loc>http://example.com/page1.html</loc></url>"
                        + "<url><loc>http://example.com/page2.html</loc></url>"
                        + "</urlset>",
                storageRoot.getChild("content/site/de/sitemap.xml")
        );
    }

    private static class FailOnceGenerator implements SitemapGenerator {

        private final String[] locations;
        private final int failAfterIdx;
        private boolean failed = false;

        /**
         * Creates a new test generator adding the given to the sitemap. It fails once after the given index throwing
         * an {@link IOException}.
         *
         * @param failAfterIdx
         * @param locations
         */
        public FailOnceGenerator(int failAfterIdx, String... locations) {
            this.locations = locations;
            this.failAfterIdx = failAfterIdx;
        }

        @Override
        public @NotNull Set<String> getNames(@NotNull Resource sitemapRoot) {
            return Collections.singleton(SitemapGenerator.DEFAULT_SITEMAP);
        }

        @Override
        public void generate(@NotNull Resource sitemapRoot, @NotNull String name, @NotNull Sitemap sitemap, @NotNull GenerationContext context) throws SitemapException {
            int i = context.getProperty("i", 0);
            for (; i < locations.length; i++) {
                context.setProperty("i", i);

                if (!failed && failAfterIdx == i) {
                    try {
                        // force the state to be written, if configured
                        ((SitemapImpl) sitemap).flush();
                        failed = true;
                        throw new RuntimeException("done writing");
                    } catch (IOException ex) {
                        throw new SitemapException(ex);
                    }
                }

                sitemap.addUrl(locations[i]);
            }
        }
    }
}
