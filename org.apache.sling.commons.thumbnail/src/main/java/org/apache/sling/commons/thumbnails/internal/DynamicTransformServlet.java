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
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.poi.util.IOUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.thumbnails.BadRequestException;
import org.apache.sling.commons.thumbnails.OutputFileFormat;
import org.apache.sling.commons.thumbnails.Transformation;
import org.apache.sling.commons.thumbnails.Transformer;
import org.apache.sling.commons.thumbnails.internal.models.TransformationHandlerConfigImpl;
import org.apache.sling.commons.thumbnails.internal.models.TransformationImpl;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A servlet to transform images using the Commons Thumbnails API using a post
 * body to transform the images
 */
@Component(immediate = true, service = Servlet.class, property = { "sling.servlet.methods=POST",
        "sling.servlet.paths=/bin/sling/commons/thumbnails/transform" })
public class DynamicTransformServlet extends SlingAllMethodsServlet {

    private static final Logger log = LoggerFactory.getLogger(DynamicTransformServlet.class);

    private final transient Transformer transformer;

    @Activate
    public DynamicTransformServlet(@Reference Transformer transformer) {
        this.transformer = transformer;
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        log.trace("doPost");

        Resource resource = request.getResourceResolver().getResource(request.getParameter("resource"));

        OutputFileFormat format = OutputFileFormat.forValue(request.getParameter("format"));
        response.setHeader("Content-Disposition", "filename=" + resource.getName());
        response.setContentType(format.getMimeType());

        try {
            ObjectMapper objectMapper = new ObjectMapper();

            List<TransformationHandlerConfigImpl> transformations = parsePostBody(request, objectMapper);

            log.debug("Transforming resource: {} with transformation: {} to {}", resource, transformations, format);
            transform(resource, response, format.toString(), new TransformationImpl(transformations));

        } catch (BadRequestException e) {
            log.error("Could not render thumbnail due to bad request", e);
            response.sendError(400, "Could not render thumbnail due to bad request: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to render thumbnail", e);
            response.sendError(500, "Failed to render thumbnail");
        }
    }

    private List<TransformationHandlerConfigImpl> parsePostBody(SlingHttpServletRequest request,
            ObjectMapper objectMapper) throws IOException {
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
