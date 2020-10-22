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

package org.apache.sling.jsonifier.servlet;

import java.io.IOException;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.jsonifier.api.JsonRendererSelector;
import org.apache.sling.jsonifier.api.JsonRenderer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/** Render the current Resource to JSON */
@Component(service = Servlet.class,
    property = {
            "sling.servlet.resourceTypes=sling/servlet/default",
            "sling.servlet.prefix:Integer=-1",

            "sling.servlet.methods=GET",
            "sling.servlet.methods=HEAD",
            "sling.servlet.selectors=s:jr",
            "sling.servlet.extension=json",
    })
public class RendererServlet extends SlingSafeMethodsServlet {
    private static final long serialVersionUID = 1L;

    @Reference
    private transient JsonRendererSelector renderers;

    @Override
    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        final JsonObjectBuilder result = Json.createObjectBuilder();

        final Resource resource = request.getResource();
        final String [] selectors = request.getRequestPathInfo().getSelectors();

        final JsonRenderer renderer = renderers.select(
            request.getResource(), 
            request.getRequestPathInfo().getSelectors());
        renderer.render(result, resource, JsonRenderer.Mode.CONTENT, selectors);

        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write(result.build().toString());
    }
}