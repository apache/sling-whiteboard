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
import org.apache.sling.sitemap.SitemapService;
import org.apache.sling.sitemap.builder.Sitemap;
import org.apache.sling.sitemap.builder.Url;
import org.apache.sling.sitemap.generator.SitemapGenerator;
import org.apache.sling.sitemap.generator.SitemapGeneratorManager;
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
import java.util.*;

import static org.apache.sling.sitemap.common.SitemapUtil.normalizeSitemapRoot;

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
    private SitemapServiceConfiguration configuration;

    private int chunkSize = 10;

    @Activate
    protected void activate(Configuration configuration) {
        chunkSize = configuration.chunkSize();
    }

    @Override
    public JobExecutionResult process(Job job, JobExecutionContext context) {
        String sitemapRootPath = job.getProperty(JOB_PROPERTY_SITEMAP_ROOT, String.class);
        String sitemapName = job.getProperty(JOB_PROPERTY_SITEMAP_NAME, SitemapService.DEFAULT_SITEMAP_NAME);
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
        } catch (IOException | SitemapException ex) {
            LOG.error("Failed to write sitemap", ex);
            return result.message(ex.getMessage()).failed();
        }
    }

    private void generate(Resource res, String name, SitemapGenerator generator, JobExecutionContext jobCtxt)
            throws SitemapException, IOException {
        try {
            CopyableByteArrayOutputStream buffer = new CopyableByteArrayOutputStream();
            GenerationContextImpl genCtxt = new GenerationContextImpl();

            // prefill the buffer with existing data from storage
            ValueMap state = storage.getState(res, name);
            InputStream existingData = state.get(JcrConstants.JCR_DATA, InputStream.class);
            if (existingData != null) {
                IOUtils.copy(existingData, buffer);
            }
            // prefill the state from storage
            for (String key : state.keySet()) {
                if (key.indexOf(':') < 0) {
                    genCtxt.data.put(key, state.get(key));
                }
            }
            // get the file index, if any
            int fileIndex = state.get(SitemapStorage.PN_SITEMAP_FILE_INDEX, 1);
            int urlCount = state.get(SitemapStorage.PN_SITEMAP_ENTRIES, 0);

            MultiFileSitemap sitemap = new MultiFileSitemap(res, name, fileIndex, buffer, genCtxt, jobCtxt);
            sitemap.currentSitemap.urlCount = urlCount;

            generator.generate(res, name, sitemap, genCtxt);

            sitemap.close();

            if (LOG.isDebugEnabled()) {
                LOG.debug("Generated sitemaps: {}", String.join(", ", sitemap.files));
            }

            // when the max(fileIndex) is smaller then in previous iterations, cleanup old files.
            Collection<String> purgedFiles = storage.deleteSitemaps(res, name, i -> i.getFileIndex() >= sitemap.fileIndex);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Purged obsolete sitemaps: {}", String.join(", ", purgedFiles));
            }
        } catch (JobAbandonedException ex) {
            throw ex;
        } catch (JobStoppedException ex) {
            LOG.debug("Job stopped, removing state", ex);
            storage.deleteState(res, name);
        } catch (RuntimeException | SitemapException | IOException ex) {
            storage.deleteState(res, name);
            if (ex instanceof IOException) {
                throw (IOException) ex;
            } else if (ex.getCause() instanceof IOException) {
                throw (IOException) ex.getCause();
            } else if (ex instanceof RuntimeException) {
                throw new SitemapException(ex);
            } else {
                throw ex;
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

        int checkpoint = -1;

        void checkpoint() {
            checkpoint = count;
        }

        void rollback() {
            ensureCheckpoint();
            count = checkpoint;
        }

        @Override
        public synchronized void reset() {
            super.reset();
            checkpoint = -1;
        }

        private InputStream copyAfterCheckpoint() {
            ensureCheckpoint();
            return new ByteBufferInputStream(ByteBuffer.wrap(buf, checkpoint, size() - checkpoint));
        }

        private InputStream copy() {
            return new ByteBufferInputStream(ByteBuffer.wrap(buf, 0, size()));
        }

        private void ensureCheckpoint() {
            if (checkpoint < 0) {
                throw new IllegalStateException("no checkpoint");
            }
        }
    }

    private static class JobStoppedException extends RuntimeException {
        JobStoppedException() {
            super("Stopped");
        }
    }

    protected static class JobAbandonedException extends RuntimeException {
        JobAbandonedException() {
            super("Abandoned");
        }
    }

    private class MultiFileSitemap implements Sitemap, Closeable {

        private final Resource sitemapRoot;
        private final String name;
        private final JobExecutionContext jobContext;
        private final GenerationContextImpl generationContext;
        private final List<String> files = new ArrayList<>(1);
        private final CopyableByteArrayOutputStream buffer;
        private final CopyableByteArrayOutputStream overflowBuffer = new CopyableByteArrayOutputStream();

        private int fileIndex;
        private StatefulSitemap currentSitemap;

        MultiFileSitemap(Resource sitemapRoot, String name, int fileIndex, CopyableByteArrayOutputStream buffer,
                GenerationContextImpl generationContext, JobExecutionContext jobContext) throws IOException {
            this.sitemapRoot = sitemapRoot;
            this.name = name;
            this.fileIndex = fileIndex;
            this.buffer = buffer;
            this.jobContext = jobContext;
            this.generationContext = generationContext;
            this.currentSitemap = newSitemap();
        }

        @NotNull
        @Override
        public Url addUrl(@NotNull String location) throws SitemapException {
            try {
                rotateIfNecessary();
            } catch (IOException ex) {
                throw new SitemapException(ex);
            }
            return currentSitemap.addUrl(location);
        }

        @Override
        public void close() throws IOException {
            rotateIfNecessary();
            // no add() will happen so the counter will miss the <url> form the overflow buffer
            closeSitemap();
        }

        private StatefulSitemap newSitemap() throws IOException {
            return new StatefulSitemap(sitemapRoot, name, buffer, jobContext, generationContext);
        }

        private void closeSitemap() throws IOException {
            currentSitemap.close();
            int urlCount = currentSitemap.urlCount;
            if (urlCount == 0) {
                // don't persist empty sitemaps
                return;
            }
            String path = storage.writeSitemap(sitemapRoot, name, buffer.copy(), fileIndex, buffer.size(), urlCount);
            // increment the file index for the next sitemap and store it in the context
            generationContext.data.put(SitemapStorage.PN_SITEMAP_FILE_INDEX, ++fileIndex);
            files.add(path);
        }

        private boolean rotateIfNecessary() throws IOException {
            // create a checkpoint before flushing the pending url.
            buffer.checkpoint();
            // flush the sitemap to write the last url to the underlying buffer
            currentSitemap.flush();
            // if the buffer size exceeds the limit (-10 bytes for the closing tag)
            if (buffer.size() + 10 > configuration.getMaxSize()) {
                // retain bytes of the last written url in a temporary buffer
                overflowBuffer.reset();
                IOUtils.copy(buffer.copyAfterCheckpoint(), overflowBuffer);
                // rollback the buffer to the checkpoint
                buffer.rollback();
                // decrease the url count as we effectively move one url
                currentSitemap.urlCount --;
                // close, write and rotate the sitemap
                rotateSitemap();
                // write the overflow
                currentSitemap.flush();
                IOUtils.copy(overflowBuffer.copy(), buffer);
                currentSitemap.urlCount ++;
                return true;
            } else if (currentSitemap.urlCount + 1 > configuration.getMaxEntries()) {
                rotateSitemap();
                return true;
            }

            return false;
        }

        private void rotateSitemap() throws IOException {
            closeSitemap();
            // reset the buffer for the new sitemap
            buffer.reset();
            // create a new sitemap and if there is any initial data, write it to the buffer
            currentSitemap = newSitemap();
        }
    }

    private class StatefulSitemap extends SitemapImpl {

        private final Resource sitemapRoot;
        private final String name;
        private final CopyableByteArrayOutputStream buffer;
        private final JobExecutionContext jobContext;
        private final GenerationContextImpl generationContext;

        private int urlCount = 0;
        private int writtenUrls = 0;

        StatefulSitemap(Resource sitemapRoot, String name, CopyableByteArrayOutputStream buffer,
                JobExecutionContext jobContext, GenerationContextImpl generationContext) throws IOException {
            super(new OutputStreamWriter(buffer, StandardCharsets.UTF_8), extensionProviderManager, buffer.size() == 0);
            this.sitemapRoot = sitemapRoot;
            this.name = name;
            this.buffer = buffer;
            this.jobContext = jobContext;
            this.generationContext = generationContext;
        }

        @NotNull
        @Override
        public Url addUrl(@NotNull String location) throws SitemapException {
            if (jobContext.isStopped()) {
                throw new JobStoppedException();
            }
            Url url = super.addUrl(location);
            urlCount ++;
            return url;
        }

        @Override
        protected boolean writePendingUrl() throws SitemapException {
            boolean written = super.writePendingUrl();
            if (written && ++writtenUrls == chunkSize) {
                try {
                    // make sure the buffer has all data from the writer
                    out.flush();
                    // copy the state and add the buffer's data
                    Map<String, Object> copy = new HashMap<>(generationContext.data.size() + 1);
                    copy.putAll(generationContext.data);
                    copy.put(SitemapStorage.PN_SITEMAP_ENTRIES, urlCount);
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

        private final ValueMap data = new ValueMapDecorator(new HashMap<>());

        @Nullable
        @Override
        public <T> T getProperty(@NotNull String name, @NotNull Class<T> cls) {
            return data.get(name, cls);
        }

        @Override
        public <T> @NotNull T getProperty(@NotNull String name, @NotNull T defaultValue) {
            return data.get(name, defaultValue);
        }

        @Override
        public void setProperty(@NotNull String name, @Nullable Object data) {
            if (name.indexOf(':') > 0) {
                // don't allow using properties from a namespace
                return;
            }
            this.data.put(name, data);
        }
    }
}
