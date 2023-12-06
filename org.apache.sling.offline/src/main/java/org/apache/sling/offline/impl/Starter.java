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

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.builder.Builders;
import org.apache.sling.api.request.builder.SlingHttpServletResponseResult;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.engine.SlingRequestProcessor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = {})
public class Starter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Reference
    private ResourceResolverFactory factory;

    @Reference
    private SlingRequestProcessor processor;

    @Activate
    public void activate(final BundleContext ctx) {
        logger.info("Activating offliner...");
        ctx.addFrameworkListener(new FrameworkListener() {
            @Override
            public void frameworkEvent(final FrameworkEvent event) {
                if (event.getType() == FrameworkEvent.STARTED) {
                    logger.info("Framework started, starting...");
                    new Thread(Starter.this::run).start();
                }
            }
        });
    }

    private void process(final ResourceResolver resolver, final String path) throws IOException, ServletException {
        logger.info("Getting {}", path);
        final Resource resource = resolver.getResource(path);
        if ( resource != null ) {
            final SlingHttpServletRequest req = Builders.newRequestBuilder(resource)
                .withExtension("html")
                .build();
            final SlingHttpServletResponseResult resp = Builders.newResponseBuilder().build();
            processor.processRequest(req, resp, resolver);

            logger.info("Result for {} = {}", path, resp.getOutputAsString());
        } else {
            throw new FileNotFoundException(path);
        }
    }
    public void run() {
        try ( final ResourceResolver resolver = factory.getAdministrativeResourceResolver(null) ) {

            process(resolver, "/content/docs/README.md");
        } catch ( final Exception e ) {
            throw new RuntimeException(e);
        }
    }
}
