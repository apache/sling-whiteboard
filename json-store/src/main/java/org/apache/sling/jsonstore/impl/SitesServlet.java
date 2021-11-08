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
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.jsonstore.api.JsonStore;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
    resourceTypes=STORE_ROOT_RESOURCE_TYPE,
    methods= "POST"
)
public class SitesServlet extends SlingAllMethodsServlet {
    @Reference
    private JsonStore store;

    @Override
    public void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        final String relativeSitePath = request.getParameter(PARAM_PATH);
        if(relativeSitePath == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, String.format("Missing required parameter '%s'", PARAM_PATH));
            return;
        }
        final Resource result = store.createSite(request.getResource(), relativeSitePath);

        // TODO set Location header etc.
        response.getWriter().write(String.format("Created site %s", result.getPath()));
    }
}
