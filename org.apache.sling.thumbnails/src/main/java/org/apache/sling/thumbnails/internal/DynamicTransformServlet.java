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
import java.util.List;
import java.util.Optional;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.IOUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.thumbnails.BadRequestException;
import org.apache.sling.thumbnails.OutputFileFormat;
import org.apache.sling.thumbnails.RenditionSupport;
import org.apache.sling.thumbnails.Transformation;
import org.apache.sling.thumbnails.Transformer;
import org.apache.sling.thumbnails.internal.models.TransformationHandlerConfigImpl;
import org.apache.sling.thumbnails.internal.models.TransformationImpl;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A servlet to transform images using the Thumbnails API using a post body to
 * transform the images
 */
@Component(immediate = true, service = Servlet.class, property = { "sling.servlet.methods=POST",
        "sling.servlet.paths=/bin/sling/thumbnails/transform" })
public class DynamicTransformServlet extends SlingAllMethodsServlet {

    private static final Logger log = LoggerFactory.getLogger(DynamicTransformServlet.class);

    private final transient Transformer transformer;

    private final transient RenditionSupport renditionSupport;

    @Activate
    public DynamicTransformServlet(@Reference Transformer transformer, @Reference RenditionSupport renditionSupport) {
        this.renditionSupport = renditionSupport;
        this.transformer = transformer;
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        log.trace("doPost");

        try {

            Resource resource = request.getResourceResolver()
                    .getResource(Optional.ofNullable(request.getParameter("resource"))
                            .orElseThrow(() -> new BadRequestException("Parameter resource must be supplied")));

            if (resource == null) {
                response.sendError(404, "No resource found at: " + request.getParameter("resource"));
                return;
            }

            OutputFileFormat format = OutputFileFormat
                    .forValue(Optional.ofNullable(request.getParameter("format")).orElse("jpeg"));
            response.setHeader("Content-Disposition", "filename=" + resource.getName());
            response.setContentType(format.getMimeType());

            Transformation transformation = getTransformation(request);

            log.debug("Transforming resource: {} with transformation: {} to {}", resource, transformation, format);
            ByteArrayOutputStream baos = transform(resource, response, format.toString(), transformation);

            String renditionName = request.getParameter("renditionName");
            if (renditionName != null) {
                if (StringUtils.isBlank(renditionName) && transformation.getName() != null) {
                    renditionName = transformation.getName() + "." + format.toString().toLowerCase();
                }
                log.debug("Setting rendition: {}", renditionName);
                if (renditionSupport.supportsRenditions(resource)) {
                    renditionSupport.setRendition(resource, renditionName,
                            new ByteArrayInputStream(baos.toByteArray()));
                } else {
                    throw new BadRequestException(
                            "Type " + resource.getResourceType() + " does not support persisting renditions");
                }
            }
        } catch (BadRequestException e) {
            log.error("Could not render thumbnail due to bad request", e);
            response.sendError(400, "Could not render thumbnail due to bad request: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to render thumbnail", e);
            response.sendError(500, "Failed to render thumbnail");
        }
    }

    private Transformation getTransformation(SlingHttpServletRequest request) throws IOException {
        String transformationPath = request.getParameter("transformationResource");
        if (StringUtils.isNotBlank(request.getParameter("transformationResource"))) {
            return Optional.ofNullable(request.getResourceResolver().getResource(transformationPath))
                    .map(r -> r.adaptTo(Transformation.class)).orElseThrow(
                            () -> new BadRequestException("Requested invalid transformation: " + transformationPath));

        } else {
            return new TransformationImpl(parsePostBody(request));
        }
    }

    private List<TransformationHandlerConfigImpl> parsePostBody(SlingHttpServletRequest request) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(request.getReader(),
                    new TypeReference<List<TransformationHandlerConfigImpl>>() {
                    });
        } catch (JsonProcessingException pe) {
            throw new BadRequestException("Could not parse transformation request", pe);
        }
    }

    private ByteArrayOutputStream transform(Resource resource, SlingHttpServletResponse response, String format,
            Transformation transformation) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        transformer.transform(resource, transformation, OutputFileFormat.valueOf(format.toUpperCase()), baos);
        IOUtils.copy(new ByteArrayInputStream(baos.toByteArray()), response.getOutputStream());
        return baos;
    }

}
