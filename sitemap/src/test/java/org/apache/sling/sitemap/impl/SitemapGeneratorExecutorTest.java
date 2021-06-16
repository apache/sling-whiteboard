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
import org.apache.sling.sitemap.builder.Sitemap;
import org.apache.sling.sitemap.generator.SitemapGenerator;
import org.apache.sling.sitemap.impl.builder.SitemapImplTest;
import org.apache.sling.sitemap.impl.builder.extensions.ExtensionProviderManager;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

import static org.apache.sling.sitemap.impl.SitemapStorageTest.assertResourceDataEquals;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({SlingContextExtension.class, MockitoExtension.class})
public class SitemapGeneratorExecutorTest {

    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);

    private final SitemapGeneratorExecutor subject = new SitemapGeneratorExecutor();
    private final SitemapGeneratorManagerImpl generatorManager = new SitemapGeneratorManagerImpl();
    private final ExtensionProviderManager extensionProviderManager = new ExtensionProviderManager();
    private final SitemapStorage storage = spy(new SitemapStorage());
    private final SitemapServiceConfiguration sitemapServiceConfiguration = new SitemapServiceConfiguration();

    @Mock
    private JobManager jobManager;
    @Mock
    private ServiceUserMapped serviceUser;
    @Mock
    private Job job;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private JobExecutionContext executionContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private JobExecutionContext.ResultBuilder resultBuilder;
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
        context.registerInjectActivateService(sitemapServiceConfiguration);
        context.registerInjectActivateService(generatorManager);
        context.registerInjectActivateService(storage);
        context.registerInjectActivateService(extensionProviderManager);

        when(job.getProperty(SitemapGeneratorExecutor.JOB_PROPERTY_SITEMAP_NAME, SitemapService.DEFAULT_SITEMAP_NAME))
                .thenReturn(SitemapService.DEFAULT_SITEMAP_NAME);
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
        assertResourceDataAndEntriesEquals(
                SitemapImplTest.XML_HEADER + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
                        + "<url><loc>http://example.com/page1.html</loc></url>"
                        + "<url><loc>http://example.com/page2.html</loc></url>"
                        + "<url><loc>http://example.com/page3.html</loc></url>"
                        + "<url><loc>http://example.com/page4.html</loc></url>"
                        + "<url><loc>http://example.com/page5.html</loc></url>"
                        + "</urlset>",
                5,
                storageRoot.getChild("content/site/de/sitemap.xml")
        );
        verify(storage, never()).writeState(any(), any(), any());
    }

    @Test
    public void testStateRemovedOnJobStopped() {
        context.registerService(SitemapGenerator.class, new FailOnceGenerator(1,
                "http://example.com/page1.html",
                "http://example.com/page2.html"
        ));
        context.registerInjectActivateService(subject, "chunkSize", 1);

        // when
        try {
            subject.process(job, executionContext);
            fail();
        } catch (RuntimeException ex) {
            // ignore
        }

        // then
        assertNotNull(storageRoot.getChild("content/site/de/sitemap.part"));

        // and when
        when(executionContext.isStopped()).thenReturn(true);
        subject.process(job, executionContext);

        // then
        assertNull(storageRoot.getChild("content/site/de/sitemap.part"));
        assertNull(storageRoot.getChild("content/site/de/sitemap.xml"));
    }

    @Test
    public void testGeneratorExceptionRethrown() {
        // given
        ThrowingGenerator generator = new ThrowingGenerator();
        context.registerService(SitemapGenerator.class, generator);
        context.registerInjectActivateService(subject, "chunkSize", 100);
        when(executionContext.result()).thenReturn(resultBuilder);

        // when
        generator.ex = new SitemapException("sitemapexception");
        subject.process(job, executionContext);

        // then
        verify(resultBuilder, times(1)).message("sitemapexception");

        // and when
        generator.ex = new SitemapException(new IOException("ioexception"));
        subject.process(job, executionContext);

        // then
        verify(resultBuilder, times(1)).message("ioexception");
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
            fail();
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
        assertResourceDataAndEntriesEquals(
                SitemapImplTest.XML_HEADER + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
                        + "<url><loc>http://example.com/page1.html</loc></url>"
                        + "<url><loc>http://example.com/page2.html</loc></url>"
                        + "<url><loc>http://example.com/page3.html</loc></url>"
                        + "<url><loc>http://example.com/page4.html</loc></url>"
                        + "<url><loc>http://example.com/page5.html</loc></url>"
                        + "</urlset>",
                5,
                storageRoot.getChild("content/site/de/sitemap.xml")
        );
    }

    @Test
    public void testJobResumesAfterBeingAbortedMultiFile() throws IOException {
        // given
        MockOsgi.activate(sitemapServiceConfiguration, context.bundleContext(), "maxEntries", 3);
        context.registerService(SitemapGenerator.class, new FailOnceGenerator(5,
                "http://example.com/page1.html",
                "http://example.com/page2.html",
                "http://example.com/page3.html",
                "http://example.com/page4.html",
                "http://example.com/page5.html",
                "http://example.com/page6.html"
        ));
        context.registerInjectActivateService(subject, "chunkSize", 2);

        // when
        try {
            subject.process(job, executionContext);
            fail();
        } catch (RuntimeException ex) {
            // ignore exception from FailOnceGenerator
        }

        // then
        assertResourceDataAndEntriesEquals(
                SitemapImplTest.XML_HEADER + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
                        + "<url><loc>http://example.com/page1.html</loc></url>"
                        + "<url><loc>http://example.com/page2.html</loc></url>"
                        + "<url><loc>http://example.com/page3.html</loc></url>"
                        + "</urlset>",
                3,
                storageRoot.getChild("content/site/de/sitemap.xml")
        );
        assertResourceDataEquals(
                SitemapImplTest.XML_HEADER + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
                        + "<url><loc>http://example.com/page4.html</loc></url>"
                        + "<url><loc>http://example.com/page5.html</loc></url>",
                storageRoot.getChild("content/site/de/sitemap.part")
        );

        // and when (resume)
        subject.process(job, executionContext);

        // then
        assertNull(storageRoot.getChild("content/site/de/sitemap.part"));
        assertResourceDataAndEntriesEquals(
                SitemapImplTest.XML_HEADER + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
                        + "<url><loc>http://example.com/page1.html</loc></url>"
                        + "<url><loc>http://example.com/page2.html</loc></url>"
                        + "<url><loc>http://example.com/page3.html</loc></url>"
                        + "</urlset>",
                3,
                storageRoot.getChild("content/site/de/sitemap.xml")
        );
        assertResourceDataAndEntriesEquals(
                SitemapImplTest.XML_HEADER + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
                        + "<url><loc>http://example.com/page4.html</loc></url>"
                        + "<url><loc>http://example.com/page5.html</loc></url>"
                        + "<url><loc>http://example.com/page6.html</loc></url>"
                        + "</urlset>",
                3,
                storageRoot.getChild("content/site/de/sitemap-2.xml")
        );
    }

    @Test
    public void testMultiFileConsistentWithSizeOverflow() throws IOException {
        // 200 = 38 (header) + 60 (urlset) + 2 * 51 (url)
        MockOsgi.activate(sitemapServiceConfiguration, context.bundleContext(), "maxSize", 200);
        context.registerService(SitemapGenerator.class, new FailOnceGenerator(Integer.MAX_VALUE,
                "http://example.com/page1.html",
                "http://example.com/page2.html",
                "http://example.com/page3.html"
        ));
        context.registerInjectActivateService(subject, "chunkSize", 10);

        // when
        subject.process(job, executionContext);

        // then
        assertResourceDataAndEntriesEquals(
                SitemapImplTest.XML_HEADER + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
                        + "<url><loc>http://example.com/page1.html</loc></url>"
                        + "</urlset>",
                1,
                storageRoot.getChild("content/site/de/sitemap.xml")
        );
        assertResourceDataAndEntriesEquals(
                SitemapImplTest.XML_HEADER + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
                        + "<url><loc>http://example.com/page2.html</loc></url>"
                        + "</urlset>",
                1,
                storageRoot.getChild("content/site/de/sitemap-2.xml")
        );
        assertResourceDataAndEntriesEquals(
                SitemapImplTest.XML_HEADER + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
                        + "<url><loc>http://example.com/page3.html</loc></url>"
                        + "</urlset>",
                1,
                storageRoot.getChild("content/site/de/sitemap-3.xml")
        );
    }

    @Test
    public void testObsoleteFilesPurgedWhenMultiFileUpdated() throws IOException {
        // given
        FailOnceGenerator generator = new FailOnceGenerator(Integer.MAX_VALUE,
                "http://example.com/page1.html",
                "http://example.com/page2.html",
                "http://example.com/page3.html"
        );
        MockOsgi.activate(sitemapServiceConfiguration, context.bundleContext(), "maxEntries", 1);
        context.registerService(SitemapGenerator.class, generator);
        context.registerInjectActivateService(subject, "chunkSize", Integer.MAX_VALUE);

        // when
        subject.process(job, executionContext);

        // then
        assertResourceDataAndEntriesEquals(
                SitemapImplTest.XML_HEADER + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
                        + "<url><loc>http://example.com/page1.html</loc></url>"
                        + "</urlset>",
                1,
                storageRoot.getChild("content/site/de/sitemap.xml")
        );
        assertResourceDataAndEntriesEquals(
                SitemapImplTest.XML_HEADER + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
                        + "<url><loc>http://example.com/page2.html</loc></url>"
                        + "</urlset>",
                1,
                storageRoot.getChild("content/site/de/sitemap-2.xml")
        );
        assertResourceDataAndEntriesEquals(
                SitemapImplTest.XML_HEADER + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
                        + "<url><loc>http://example.com/page3.html</loc></url>"
                        + "</urlset>",
                1,
                storageRoot.getChild("content/site/de/sitemap-3.xml")
        );

        // and when
        generator.setLocations("http://example.com/pageX.html");
        subject.process(job, executionContext);

        // then
        assertResourceDataAndEntriesEquals(
                SitemapImplTest.XML_HEADER + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
                        + "<url><loc>http://example.com/pageX.html</loc></url>"
                        + "</urlset>",
                1,
                storageRoot.getChild("content/site/de/sitemap.xml")
        );
        assertNull(storageRoot.getChild("content/site/de/sitemap-2.xml"));
        assertNull(storageRoot.getChild("content/site/de/sitemap-3.xml"));
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

        assertResourceDataAndEntriesEquals(
                SitemapImplTest.XML_HEADER + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
                        + "<url><loc>http://example.com/page1.html</loc></url>"
                        + "<url><loc>http://example.com/page2.html</loc></url>"
                        + "</urlset>",
                2,
                storageRoot.getChild("content/site/de/sitemap.xml")
        );
    }

    static void assertResourceDataAndEntriesEquals(String expectedValue, int entries, @Nullable Resource resource) throws IOException {
        assertNotNull(resource);
        assertEquals(entries, resource.getValueMap().get(SitemapStorage.PN_SITEMAP_ENTRIES, -1));
        assertResourceDataEquals(expectedValue, resource);
    }

    private static class ThrowingGenerator implements SitemapGenerator {

        private SitemapException ex;

        @Override
        public void generate(@NotNull Resource sitemapRoot, @NotNull String name, @NotNull Sitemap sitemap, @NotNull GenerationContext context) throws SitemapException {
            throw ex;
        }
    }

    private static class FailOnceGenerator implements SitemapGenerator {

        private final int failAfterIdx;
        private boolean failed = false;

        private String[] locations;

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

        public void setLocations(String... locations) {
            this.locations = locations;
        }

        @Override
        public void generate(@NotNull Resource sitemapRoot, @NotNull String name, @NotNull Sitemap sitemap,
                             @NotNull GenerationContext context) throws SitemapException {
            int i = context.getProperty("i", 0);
            for (; i < locations.length; i++) {
                context.setProperty("i", i);
                // addUrl will flush with the MultiFileSitemap
                sitemap.addUrl(locations[i]);

                if (!failed && failAfterIdx == i) {
                    failed = true;
                    throw new SitemapGeneratorExecutor.JobAbandonedException();
                }
            }
        }
    }
}
