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

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.jsonstore.internal.api.JsonStoreConstants;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes = {
        JsonStoreConstants.CONTENT_RESOURCE_TYPE,
        JsonStoreConstants.ELEMENTS_RESOURCE_TYPE,
        JsonStoreConstants.SCHEMA_RESOURCE_TYPE,
    },
    methods = { "GET" }
)
public class JsonGetServlet extends SlingSafeMethodsServlet {

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
}
