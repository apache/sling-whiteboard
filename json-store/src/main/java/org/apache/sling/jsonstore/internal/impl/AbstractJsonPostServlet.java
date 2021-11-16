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

package org.apache.sling.jsonstore.internal.impl;

import static org.apache.sling.jsonstore.internal.api.JsonStoreConstants.JSON_PROP_NAME;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.jsonstore.internal.api.JsonStoreConstants;

abstract class AbstractJsonPostServlet extends SlingAllMethodsServlet {

    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public final void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        // Parse incoming JSON
        if(!ContentType.checkJson(request, response)) {
            return;
        }
        JsonNode json = null;
        try {
            json = objectMapper.readTree(request.getInputStream());
        } catch(Exception e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "JSON parsing failed: " + e);
            return;
        }

        // Create the target Resource if needed
        Resource resource = request.getResource();
        if(resource instanceof WrappedResource) {
            resource = ResourceUtil.getOrCreateResource(
                resource.getResourceResolver(),
                resource.getPath(),
                resource.getResourceType(),
                JsonStoreConstants.FOLDER_RESOURCE_TYPE,
                false
            );
        }

        try {
            validateJson(resource, json);
        } catch(IOException ioe) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "JSON validation failed: " + ioe);
            return;
        }
        
        // Store JSON
        final ModifiableValueMap vm = resource.adaptTo(ModifiableValueMap.class);
        if(vm == null) {
            throw new ServletException("Resource does not adapt to ModifiableValueMap");
        }
        vm.put(JSON_PROP_NAME, objectMapper.writeValueAsString(json));

        resource.getResourceResolver().commit();

        // TODO set Location header etc.
        response.getWriter().write(String.format("%s stored JSON at %s%n", getClass().getSimpleName(), resource.getPath()));
    }

    protected abstract void validateJson(Resource resource, JsonNode json) throws IOException;
}
