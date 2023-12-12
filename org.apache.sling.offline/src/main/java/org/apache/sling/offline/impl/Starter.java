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
package org.apache.sling.offline.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.builder.Builders;
import org.apache.sling.api.request.builder.SlingHttpServletResponseResult;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.engine.SlingRequestProcessor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = {}, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Starter {

    public @interface Config {
        String input_path();

        String output_path();

        long retry_delay() default 10;

        long timeout() default 5000;
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Reference
    private ResourceResolverFactory factory;

    @Reference
    private SlingRequestProcessor processor;

    private volatile Config config;

    private volatile BundleContext ctx;

    @Activate
    public void activate(final BundleContext ctx, final Config config) {
        logger.info("Activating offliner...");
        this.ctx = ctx;
        this.config = config;
        final Thread t = new Thread(Starter.this::run);
        t.setDaemon(true);
        t.start();
    }

    private boolean ignore(final Resource r) {
        return r.getName().startsWith(".");
    }

    private boolean isWebPage(final Resource r) {
        return r.getName().endsWith(".md");
    }

    private boolean isImage(final Resource r) {
        return r.getName().endsWith(".png") || r.getName().endsWith(".jpg");
    }

    private void process(final ResourceResolver resolver, final Resource resource, final boolean retry) 
    throws IOException, ServletException {
        if ( this.ignore(resource) ) {
            return;
        }
        if ( this.isImage(resource) ) {
            this.handleBinary(resolver, resource);
        }
        if ( !this.isWebPage(resource) ) {
            for(final Resource c : resource.getChildren()) {
                process(resolver, c, retry);
            }
            return;
        }
        logger.info("Processing {}", resource.getPath());
        final long lastModified = resource.getResourceMetadata().getModificationTime();
        final File output = new File(config.output_path(), resource.getPath().substring(this.config.input_path().length()).concat(".html"));
        if ( lastModified > 0 && output.exists() && output.lastModified() >= lastModified ) {
            logger.info("Skipping {} as it is up to date", resource.getPath());
            return;
        }
        final long endAt = System.currentTimeMillis() + this.config.timeout();
        while ( System.currentTimeMillis() < endAt ) {
            final SlingHttpServletRequest req = Builders.newRequestBuilder(resource)
                .withExtension("html")
                .build();
            final SlingHttpServletResponseResult resp = Builders.newResponseBuilder().build();
            processor.processRequest(req, resp, resolver);

            if ( resp.getStatus() == 200 ) {
                logger.info("Writing output to {}", output.getAbsolutePath());
                output.getParentFile().mkdirs();
                Files.writeString(output.toPath(), resp.getOutputAsString());
                return;
            }
            if (!retry) {
                break;
            }
            try {
                Thread.sleep(this.config.retry_delay());
            } catch ( final InterruptedException ie) {
                // ignore
            }
        }
        logger.error("Unable to create html for {}", resource.getPath());
    }

    private void handleBinary(final ResourceResolver resolver, final Resource resource) throws IOException {
        logger.info("Processing binary {}", resource.getPath());
        final long lastModified = resource.getResourceMetadata().getModificationTime();
        final File output = new File(config.output_path(), resource.getPath().substring(this.config.input_path().length()));
        if ( lastModified > 0 && output.exists() && output.lastModified() >= lastModified ) {
            logger.info("Skipping {} as it is up to date", resource.getPath());
            return;
        }
        logger.info("Copying to {}", output.getAbsolutePath());
        output.getParentFile().mkdirs();
        Files.copy(resource.adaptTo(InputStream.class), output.toPath());
    }

    private Resource getResource(final ResourceResolver resolver, final String path) {
        logger.info("Trying to get resource {}", path);
        // 5 seconds to get the resource
        final long endAt = System.currentTimeMillis() + this.config.timeout();
        while ( System.currentTimeMillis() < endAt ) {
            final Resource resource = resolver.getResource(path);
            if ( resource != null ) {
                return resource;
            }
            try {
                Thread.sleep(this.config.retry_delay());
            } catch ( final InterruptedException ie) {
                // ignore
            }
        }
        return null;
    }

    public void run() {
        new File(this.config.output_path()).mkdirs();
        try ( final ResourceResolver resolver = factory.getAdministrativeResourceResolver(null) ) {

            final Resource root = this.getResource(resolver, config.input_path());
            if ( root == null ) {
                logger.error("Unable to find root resource at {}", config.input_path());
            } else {
                boolean first = true;
                for(final Resource c : root.getChildren()) {
                    process(resolver, c, first);
                    first = false;
                }
            }
        } catch ( final Exception e ) {
            throw new RuntimeException(e);
        }
        // shutdown
        try {
            ctx.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).stop();
        } catch (final BundleException e) {
            // ignore
        }
        System.exit(0);
    }
}
