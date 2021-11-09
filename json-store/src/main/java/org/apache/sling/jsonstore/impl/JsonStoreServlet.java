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

package org.apache.sling.jsonstore.impl;

import static org.apache.sling.jsonstore.api.JsonStoreConstants.*;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes=JSON_BLOB_RESOURCE_TYPE,
    methods= { "POST", "GET" }
)
public class JsonStoreServlet extends SlingAllMethodsServlet {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        // Stream the stored JSON, which was validated earlier
        final ValueMap vm = request.getResource().adaptTo(ValueMap.class);
        if(vm == null) {
            throw new ServletException("Resource does not adapt to ValueMap");
        }
        final String json = vm.get(JSON_PROP_NAME, String.class);
        if(json == null) {
            throw new ServletException("Missing " + JSON_PROP_NAME + " property on " + request.getResource().getPath());
        }
        response.getWriter().write(json);
    }

    @Override
    public void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {

        // Parse incoming JSON
        // TODO validate, depending on subpath
        JsonNode json = null;
        try {
            json = mapper.readTree(request.getInputStream());
        } catch(Exception e) {
            throw new ServletException("JSON parsing failed", e);
        }

        // Create the target Resource if needed
        Resource resource = request.getResource();
        if(resource instanceof WrappedResource) {
            resource = ResourceUtil.getOrCreateResource(
                resource.getResourceResolver(),
                resource.getPath(),
                resource.getResourceType(),
                JSON_FOLDER_RESOURCE_TYPE,
                false
            );
        }

        // Store JSON
        final ModifiableValueMap vm = resource.adaptTo(ModifiableValueMap.class);
        if(vm == null) {
            throw new ServletException("Resource does not adapt to ModifiableValueMap");
        }
        vm.put(JSON_PROP_NAME, mapper.writeValueAsString(json));

        resource.getResourceResolver().commit();

        /*
        // TODO Validate the schema, or the JSON according to the desired schema
        final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        factory.getSchema(schema);
        final String json = mapper.writeValueAsString(schema);
        */
        
        // TODO set Location header etc.
        response.getWriter().write(String.format("Stored JSON at %s", resource.getPath()));
    }
}
