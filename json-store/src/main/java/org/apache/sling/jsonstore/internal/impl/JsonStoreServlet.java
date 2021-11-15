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

import static org.apache.sling.jsonstore.internal.api.JsonStoreConstants.JSON_BLOB_RESOURCE_TYPE;
import static org.apache.sling.jsonstore.internal.api.JsonStoreConstants.JSON_FOLDER_RESOURCE_TYPE;
import static org.apache.sling.jsonstore.internal.api.JsonStoreConstants.JSON_PROP_NAME;

import java.io.IOException;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.jsonstore.internal.api.DataTypeValidator;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes=JSON_BLOB_RESOURCE_TYPE,
    methods= { "POST", "GET" }
)
public class JsonStoreServlet extends SlingAllMethodsServlet {

    private final ObjectMapper mapper = new ObjectMapper();

    @Reference(
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC,
        policyOption = ReferencePolicyOption.GREEDY
    )
    private volatile List<DataTypeValidator> jsonValidators;

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
        JsonNode json = null;
        try {
            json = mapper.readTree(request.getInputStream());
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
                JSON_FOLDER_RESOURCE_TYPE,
                false
            );
        }

        // Parse path
        final JsonStorePathInfo pathInfo = new JsonStorePathInfo(resource.getPath());
        request.getRequestProgressTracker().log(pathInfo.toString());
        if(pathInfo.dataPath == null || pathInfo.dataPath.length() == 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "empty data path, cannot store JSON here");
            return;
        }

        // Validate JSON
        int appliedValidators = 0;
        for(DataTypeValidator v : jsonValidators) {
            try {
                if(v.validate(resource.getResourceResolver(), json, pathInfo.site, pathInfo.dataType)) {
                    appliedValidators++;
                }
            } catch(Exception e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Validation failed: " + e);
                return;
            }
        }
        if(appliedValidators == 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No validators found for this path or input");
            return;
        }

        // Store JSON
        final ModifiableValueMap vm = resource.adaptTo(ModifiableValueMap.class);
        if(vm == null) {
            throw new ServletException("Resource does not adapt to ModifiableValueMap");
        }
        vm.put(JSON_PROP_NAME, mapper.writeValueAsString(json));

        resource.getResourceResolver().commit();

        // TODO set Location header etc.
        response.getWriter().write(String.format("Stored JSON at %s%n", resource.getPath()));
    }
}
