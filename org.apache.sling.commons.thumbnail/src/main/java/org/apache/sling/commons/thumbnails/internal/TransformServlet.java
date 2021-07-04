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
package org.apache.sling.commons.thumbnails.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.poi.util.IOUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.thumbnails.BadRequestException;
import org.apache.sling.commons.thumbnails.OutputFileFormat;
import org.apache.sling.commons.thumbnails.ThumbnailSupport;
import org.apache.sling.commons.thumbnails.Transformation;
import org.apache.sling.commons.thumbnails.Transformer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A servlet to transform images using the Commons Thumbnails API. Can be
 * invoked using the syntax:
 * 
 * /content/file/path.jpg.transform/transformation-name.png
 */
@Component(immediate = true)
public class TransformServlet extends SlingAllMethodsServlet {

    private static final Logger log = LoggerFactory.getLogger(TransformServlet.class);

    private static final long serialVersionUID = -1513067546618762171L;

    public static final String SERVICE_USER = "sling-commons-thumbnails";

    private final transient TransformationServiceUser transformationServiceUser;

    private final transient Transformer transformer;

    private final transient ThumbnailSupport thumbnailSupport;

    private final transient TransformationCache transformationCache;

    private final transient ServiceRegistration<Servlet> servletRegistration;

    @Activate
    public TransformServlet(@Reference ThumbnailSupport thumbnailSupport, @Reference Transformer transformer,
            @Reference TransformationServiceUser transformationServiceUser,
            @Reference TransformationCache transformationCache, BundleContext context) {
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

        String name = StringUtils.substringBeforeLast(request.getRequestPathInfo().getSuffix(), ".");
        response.setHeader("Content-Disposition", "filename=" + request.getResource().getName());
        String format = StringUtils.substringAfterLast(request.getRequestPathInfo().getSuffix(), ".");
        log.debug("Transforming resource: {} with transformation: {} to {}", request.getResource(), name, format);
        String original = response.getContentType();
        try (ResourceResolver serviceResolver = transformationServiceUser.getTransformationServiceUser()) {
            Optional<Transformation> transformation = transformationCache.getTransformation(serviceResolver, name);
            if (transformation.isPresent()) {
                performTransformation(request, response, name, format, serviceResolver, transformation.get());
            } else {
                log.error("Unable to find transformation: {}", name);
                response.setContentType(original);
                response.sendError(404, "Could not find transformation: " + name);
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

    private void performTransformation(SlingHttpServletRequest request, SlingHttpServletResponse response, String name,
            String format, ResourceResolver serviceResolver, Transformation transformation) throws IOException {
        response.setContentType(OutputFileFormat.forRequest(request).getMimeType());

        String resourceType = request.getResource().getResourceType();
        if (thumbnailSupport.getPersistableTypes().contains(resourceType)) {
            String expectedPath = thumbnailSupport.getRenditionPath(resourceType) + "/" + name + "." + format;
            Resource rendition = request.getResource().getChild(expectedPath);
            if (rendition != null) {
                log.debug("Using existing rendition {}", name);
                IOUtils.copy(rendition.adaptTo(InputStream.class), response.getOutputStream());
            } else {
                ByteArrayOutputStream baos = transform(request, response, transformation);
                Resource file = ResourceUtil.getOrCreateResource(serviceResolver,
                        request.getResource().getPath() + "/" + expectedPath,
                        Collections.singletonMap(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FILE),
                        JcrConstants.NT_UNSTRUCTURED, false);
                Map<String, Object> properties = new HashMap<>();
                properties.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                properties.put(JcrConstants.JCR_DATA, new ByteArrayInputStream(baos.toByteArray()));
                ResourceUtil.getOrCreateResource(serviceResolver, file.getPath() + "/" + JcrConstants.JCR_CONTENT,
                        properties, JcrConstants.NT_UNSTRUCTURED, true);
            }
        } else {
            log.debug("Sending transformation to response....");
            transform(request, response, transformation);
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
