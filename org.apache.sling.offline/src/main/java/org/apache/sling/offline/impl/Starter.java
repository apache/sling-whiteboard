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
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Reference
    private ResourceResolverFactory factory;

    @Reference
    private SlingRequestProcessor processor;

    private volatile Config config;

    @Activate
    public void activate(final BundleContext ctx, final Config config) {
        logger.info("Activating offliner...");
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

    private void process(final ResourceResolver resolver, final Resource resource) throws IOException, ServletException {
        if ( this.ignore(resource) ) {
            return;
        }
        if ( !this.isWebPage(resource) ) {
            return;
        }
        logger.info("Processing {}", resource.getPath());
        final SlingHttpServletRequest req = Builders.newRequestBuilder(resource)
            .withExtension("html")
            .build();
        final SlingHttpServletResponseResult resp = Builders.newResponseBuilder().build();
        processor.processRequest(req, resp, resolver);

        final File output = new File(config.output_path(), resource.getPath().substring(this.config.input_path().length()).concat(".html"));
        logger.info("Writing output to {}", output.getAbsolutePath());
        output.getParentFile().mkdirs();
        Files.writeString(output.toPath(), resp.getOutputAsString());
    }

    private Resource getResource(final ResourceResolver resolver, final String path) {
        logger.info("Trying to get resource {}", path);
        // 5 seconds to get the resource
        final long endAt = System.currentTimeMillis() + 5000;
        while ( System.currentTimeMillis() < endAt ) {
            final Resource resource = resolver.getResource(path);
            if ( resource != null ) {
                return resource;
            }
            try {
                Thread.sleep(100);
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
                for(final Resource c : root.getChildren()) {
                    process(resolver, c);
                }
            }
        } catch ( final Exception e ) {
            throw new RuntimeException(e);
        }
    }
}
