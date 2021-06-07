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

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.*;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobExecutionContext;
import org.apache.sling.event.jobs.consumer.JobExecutionResult;
import org.apache.sling.event.jobs.consumer.JobExecutor;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.apache.sling.sitemap.SitemapException;
import org.apache.sling.sitemap.builder.Url;
import org.apache.sling.sitemap.generator.SitemapGenerator;
import org.apache.sling.sitemap.impl.builder.SitemapImpl;
import org.apache.sling.sitemap.impl.builder.extensions.ExtensionProviderManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventProperties;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.apache.sling.sitemap.impl.SitemapUtil.normalizeSitemapRoot;

@Component(
        service = JobExecutor.class,
        property = {
                JobExecutor.PROPERTY_TOPICS + "=" + SitemapGeneratorExecutor.JOB_TOPIC
        }
)
@Designate(ocd = SitemapGeneratorExecutor.Configuration.class)
public class SitemapGeneratorExecutor implements JobExecutor {

    @ObjectClassDefinition(name = "Apache Sling Sitemap - Background Generator")
    @interface Configuration {

        @AttributeDefinition(name = "Chunk size", description = "If set to a positive integer, the background job " +
                "writes incomplete sitemaps after the given number of urls to the repository and persists progress. " +
                "This allows the job to be interrupted and resumed.")
        int chunkSize() default Integer.MAX_VALUE;
    }

    static final String JOB_TOPIC = "org/apache/sling/sitemap/build";
    static final String JOB_PROPERTY_SITEMAP_ROOT = "sitemap.root";
    static final String JOB_PROPERTY_SITEMAP_NAME = "sitemap.name";

    private static final Logger LOG = LoggerFactory.getLogger(SitemapGeneratorExecutor.class);
    private static final Map<String, Object> AUTH = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE,
            "sitemap-reader");

    @Reference
    private ResourceResolverFactory resourceResolverFactory;
    @Reference
    private SitemapGeneratorManager generatorManager;
    @Reference
    private ExtensionProviderManager extensionProviderManager;
    @Reference
    private SitemapStorage storage;
    @Reference(target = "(subServiceName=sitemap-reader)")
    private ServiceUserMapped serviceUserMapped;
    @Reference
    private EventAdmin eventAdmin;
    @Reference
    private SitemapServiceImpl sitemapService;

    private int chunkSize = 10;

    @Activate
    protected void activate(Configuration configuration) {
        chunkSize = configuration.chunkSize();
    }

    @Override
    public JobExecutionResult process(Job job, JobExecutionContext context) {
        String sitemapRootPath = job.getProperty(JOB_PROPERTY_SITEMAP_ROOT, String.class);
        String sitemapName = job.getProperty(JOB_PROPERTY_SITEMAP_NAME, SitemapGenerator.DEFAULT_SITEMAP);
        JobExecutionContext.ResultBuilder result = context.result();

        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH)) {
            Resource sitemapRoot = normalizeSitemapRoot(resourceResolver.getResource(sitemapRootPath));

            if (sitemapRoot == null) {
                return result.message("Cannot find sitemap root at: " + sitemapRootPath).cancelled();
            }

            SitemapGenerator generator = generatorManager.getGenerator(sitemapRoot, sitemapName);

            if (generator == null) {
                return result.message("Generator of '" + sitemapName + "' unavailable at: " + sitemapRootPath).failed();
            }

            generate(sitemapRoot, sitemapName, generator, context);

            return result.succeeded();
        } catch (LoginException ex) {
            LOG.warn("Failed to login service user for sitemap generation", ex);
            return result.message(ex.getMessage()).cancelled();
        } catch (IOException ex) {
            LOG.error("Failed to write sitemap", ex);
            return result.message(ex.getMessage()).failed();
        }
    }

    private void generate(Resource sitemapRoot, String name, SitemapGenerator generator,
                          JobExecutionContext executionContext) throws IOException {
        try {
            CopyableByteArrayOutputStream buffer = new CopyableByteArrayOutputStream();
            GenerationContextImpl context = new GenerationContextImpl();
            // prefill the buffer with existing data
            ValueMap state = storage.getState(sitemapRoot, name);
            InputStream existingData = state.get(JcrConstants.JCR_DATA, InputStream.class);
            if (existingData != null) {
                IOUtils.copy(existingData, buffer);
            }
            // prefill the state from storage
            for (String key : state.keySet()) {
                if (key.indexOf(':') < 0) {
                    context.state.put(key, state.get(key));
                }
            }

            Writer writer = new OutputStreamWriter(buffer, StandardCharsets.UTF_8);
            SitemapImpl sitemap = new ChunkedSitemap(writer, extensionProviderManager, existingData == null,
                    sitemapRoot, name, buffer, context) {
                @Override
                public @NotNull Url addUrl(@NotNull String location) throws SitemapException {
                    if (executionContext.isStopped()) {
                        throw new JobStoppedException();
                    }
                    return super.addUrl(location);
                }
            };

            generator.generate(sitemapRoot, name, sitemap, context);
            sitemap.close();

            String storagePath = storage.writeSitemap(sitemapRoot, name, buffer.copy(), buffer.size(), sitemap.getUrlCount());

            Map<String, Object> eventProperties = new HashMap<>(3);
            eventProperties.put(SitemapGenerator.EVENT_PROPERTY_SITEMAP_NAME, name);
            eventProperties.put(SitemapGenerator.EVENT_PROPERTY_SITEMAP_ROOT, sitemapRoot.getPath());
            eventProperties.put(SitemapGenerator.EVENT_PROPERTY_SITEMAP_URLS, sitemap.getUrlCount());
            eventProperties.put(SitemapGenerator.EVENT_PROPERTY_SITEMAP_STORAGE_PATH, storagePath);
            eventProperties.put(SitemapGenerator.EVENT_PROPERTY_SITEMAP_STORAGE_SIZE, buffer.size());
            eventProperties.put(SitemapGenerator.EVENT_PROPERTY_SITEMAP_EXCEEDS_LIMITS,
                    !sitemapService.isWithinLimits(buffer.size(), sitemap.getUrlCount()));
            
            eventAdmin.sendEvent(new Event(SitemapGenerator.EVENT_TOPIC_SITEMAP_UPDATED, new EventProperties(eventProperties)));
        } catch (JobStoppedException ex) {
            LOG.debug("Job stopped, removing state", ex);
            storage.removeState(sitemapRoot, name);
        } catch (SitemapException | IOException ex) {
            storage.removeState(sitemapRoot, name);
            if (ex instanceof SitemapException && ex.getCause() instanceof IOException) {
                throw (IOException) ex.getCause();
            } else if (ex instanceof IOException) {
                throw (IOException) ex;
            } else {
                throw new IOException(ex);
            }
        }
    }

    private static class ByteBufferInputStream extends InputStream {

        private final ByteBuffer buf;

        public ByteBufferInputStream(ByteBuffer buf) {
            this.buf = buf;
        }

        public int read() {
            if (!buf.hasRemaining()) {
                return -1;
            }
            return buf.get() & 0xFF;
        }

        public int read(byte @NotNull [] bytes, int off, int len) {
            if (!buf.hasRemaining()) {
                return -1;
            }

            len = Math.min(len, buf.remaining());
            buf.get(bytes, off, len);
            return len;
        }
    }

    private static class CopyableByteArrayOutputStream extends ByteArrayOutputStream {
        private InputStream copy() {
            return new ByteBufferInputStream(ByteBuffer.wrap(buf, 0, size()));
        }
    }

    private static class JobStoppedException extends RuntimeException {
        JobStoppedException() {
            super("Stopped at " + System.currentTimeMillis());
        }
    }

    private class ChunkedSitemap extends SitemapImpl {

        private final Resource sitemapRoot;
        private final String name;
        private final GenerationContextImpl context;
        private final CopyableByteArrayOutputStream buffer;

        private int writtenUrls = 0;

        public ChunkedSitemap(Writer writer, ExtensionProviderManager extensionProviderManager, boolean writeHeader,
                              Resource sitemapRoot, String name, CopyableByteArrayOutputStream buffer,
                              GenerationContextImpl context) throws IOException {
            super(writer, extensionProviderManager, writeHeader);
            this.sitemapRoot = sitemapRoot;
            this.name = name;
            this.context = context;
            this.buffer = buffer;
        }

        @Override
        protected boolean writePendingUrl() throws SitemapException {
            boolean written = super.writePendingUrl();
            if (written && ++writtenUrls == chunkSize) {
                try {
                    // make sure the buffer has all data from the writer
                    out.flush();
                    // copy the state and add the buffer's data
                    Map<String, Object> copy = new HashMap<>(context.state.size() + 1);
                    copy.putAll(context.state);
                    copy.put(JcrConstants.JCR_DATA, buffer.copy());
                    // write the state and reset the counter for the next iteration
                    storage.writeState(sitemapRoot, name, copy);
                    writtenUrls = 0;
                } catch (IOException ex) {
                    throw new SitemapException(ex);
                }
            }
            return written;
        }
    }

    private class GenerationContextImpl implements SitemapGenerator.GenerationContext {

        private final ValueMap state = new ValueMapDecorator(new HashMap<>());

        @Nullable
        @Override
        public <T> T getProperty(@NotNull String name, @NotNull Class<T> cls) {
            return state.get(name, cls);
        }

        @Override
        public <T> @NotNull T getProperty(@NotNull String name, @NotNull T defaultValue) {
            return state.get(name, defaultValue);
        }

        @Override
        public void setProperty(@NotNull String name, @Nullable Object data) {
            state.put(name, data);
        }
    }
}
