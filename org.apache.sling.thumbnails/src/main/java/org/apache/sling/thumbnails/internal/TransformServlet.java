/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.thumbnails.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.IOUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.thumbnails.BadRequestException;
import org.apache.sling.thumbnails.OutputFileFormat;
import org.apache.sling.thumbnails.RenditionSupport;
import org.apache.sling.thumbnails.ThumbnailSupport;
import org.apache.sling.thumbnails.Transformation;
import org.apache.sling.thumbnails.Transformer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A servlet to transform images using the Thumbnails API. Can be invoked using
 * the syntax:
 * 
 * /content/file/path.jpg.transform/transformation-name.png
 */
@Component(immediate = true)
public class TransformServlet extends SlingAllMethodsServlet {

    private static final Logger log = LoggerFactory.getLogger(TransformServlet.class);

    private static final long serialVersionUID = -1513067546618762171L;

    private final transient RenditionSupport renditionSupport;

    private final transient ServiceRegistration<Servlet> servletRegistration;

    private final transient TransformationServiceUser transformationServiceUser;

    private final transient Transformer transformer;

    private final transient ThumbnailSupport thumbnailSupport;

    private final transient TransformationCache transformationCache;

    @Activate
    public TransformServlet(@Reference ThumbnailSupport thumbnailSupport, @Reference Transformer transformer,
            @Reference TransformationServiceUser transformationServiceUser,
            @Reference TransformationCache transformationCache, @Reference RenditionSupport renditionSupport,
            BundleContext context) {
        this.renditionSupport = renditionSupport;
        this.thumbnailSupport = thumbnailSupport;
        this.transformer = transformer;
        this.transformationServiceUser = transformationServiceUser;
        this.transformationCache = transformationCache;

        log.info("Registering as servlet...");
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("sling.servlet.methods", new String[] { "GET" });
        properties.put("sling.servlet.extensions", "transform");
        properties.put("sling.servlet.resourceTypes", thumbnailSupport.getSupportedTypes());

        this.servletRegistration = context.registerService(Servlet.class, this, properties);
        log.info("Transform servlet registered...");

    }

    @Deactivate
    public void deactivate() {
        if (this.servletRegistration != null) {
            this.servletRegistration.unregister();
        }
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        log.trace("doGet");

        String transformationName = StringUtils.substringBeforeLast(request.getRequestPathInfo().getSuffix(), ".");
        String renditionName = request.getRequestPathInfo().getSuffix();
        String format = StringUtils.substringAfterLast(request.getRequestPathInfo().getSuffix(), ".");
        response.setHeader("Content-Disposition", "filename=" + request.getResource().getName());

        log.debug("Transforming resource: {} with transformation: {} to {}", request.getResource(), transformationName,
                format);
        try {
            Resource file = request.getResource();
            if (renditionSupport.renditionExists(file, renditionName)) {
                response.setContentType(OutputFileFormat.forRequest(request).getMimeType());
                IOUtils.copy(renditionSupport.getRenditionContent(file, renditionName), response.getOutputStream());
            } else {
                try (ResourceResolver servicResolver = transformationServiceUser.getTransformationServiceUser()) {
                    performTransformation(request, response, transformationName, renditionName, servicResolver);
                }
            }
        } catch (BadRequestException e) {
            log.error("Could not render thumbnail due to bad request", e);
            response.sendError(400, "Could not render thumbnail due to bad request: " + e.getMessage());
        } catch (Exception e) {
            log.error("Exception rendering transformed resource", e);
            response.setStatus(500);
            RequestDispatcherOptions op = new RequestDispatcherOptions();
            op.setReplaceSuffix(thumbnailSupport.getServletErrorSuffix());
            op.setReplaceSelectors("transform");
            RequestDispatcher disp = request.getRequestDispatcher(thumbnailSupport.getServletErrorResourcePath(), op);
            disp.forward(request, response);
        }
    }

    private void performTransformation(SlingHttpServletRequest request, SlingHttpServletResponse response,
            String transformationName, String renditionName, ResourceResolver serviceResolver)
            throws IOException, ExecutionException {
        Resource file = request.getResource();
        String originalContentType = response.getContentType();
        response.setContentType(OutputFileFormat.forRequest(request).getMimeType());
        Optional<Transformation> transformationOp = transformationCache.getTransformation(serviceResolver,
                transformationName);
        if (!transformationOp.isPresent()) {
            log.error("Unable to find transformation: {}", transformationName);
            response.setContentType(originalContentType);
            response.sendError(404, "Unable to find transformation: " + transformationName);
        } else {
            Transformation transformation = transformationOp.get();
            log.debug("Transforming file...");
            ByteArrayOutputStream baos = transform(request, response, transformation);
            if (renditionSupport.supportsRenditions(file)) {
                log.debug("Saving rendition...");
                renditionSupport.setRendition(file, renditionName, new ByteArrayInputStream(baos.toByteArray()));
            }
        }
    }

    private ByteArrayOutputStream transform(SlingHttpServletRequest request, SlingHttpServletResponse response,
            Transformation transformation) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        transformer.transform(request.getResource(), transformation, OutputFileFormat.forRequest(request), baos);
        IOUtils.copy(new ByteArrayInputStream(baos.toByteArray()), response.getOutputStream());
        return baos;
    }

}
