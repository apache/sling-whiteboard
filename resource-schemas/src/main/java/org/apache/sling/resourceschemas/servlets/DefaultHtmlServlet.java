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

package org.apache.sling.resourceschemas.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.resourceschemas.api.CreateChildAction;
import org.apache.sling.resourceschemas.api.ResourceAction;
import org.apache.sling.resourceschemas.api.ResourceSchema;
import org.apache.sling.resourceschemas.impl.HtmlGenerator;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.apache.sling.resourceschemas.api.ResourceSchemaRegistry;

/** SlingServlet that generates the default HTML representations
 *  to allow CRUD operations on resources backed by Sling Resource Schemas
 */
@Component(service = Servlet.class,
    property = {
            "service.description=Sling Resource Schemas Default HTML Servlet",
            "service.vendor=The Apache Software Foundation",
            "sling.servlet.selectors=srs",
            "sling.servlet.extensions=html",
            "sling.servlet.resourceTypes=sling/servlet/default",
            "sling.servlet.methods=GET"
    })
public class DefaultHtmlServlet extends SlingSafeMethodsServlet {

    @Reference
    private ResourceSchemaRegistry registry;
    
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {

        final Resource r = request.getResource();
        final String resourceType = r.getResourceType();
        final ResourceSchema m = registry.getSchema(resourceType);
        if(m == null) {
            response.sendError(
                HttpServletResponse.SC_BAD_REQUEST,
                "ResourceSchema not found for resource type " + resourceType + " at " + request.getResource().getPath());
            return;
        }
        
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html");
        
        final PrintWriter w = response.getWriter();
        w.println("<html><body><div class='srs-page'>");
        w.println("<h1>Sling Resource Schemas: generated edit forms<br/>for " + r.getPath() + "</h1><hr/>\n");
        
        final HtmlGenerator h = new HtmlGenerator(request, w);
        h.generateNavigation(r);
        h.generateEditForm(r, m);
        w.println();

        if(!m.getActions().isEmpty()) {
            w.println("<h2>Actions</h2>");
        }
        for(ResourceAction a : m.getActions()) {
            if(a instanceof CreateChildAction) {
                final CreateChildAction cca = (CreateChildAction)a;
                final ResourceSchema em = registry.getSchema(cca.getResourceType());
                if(em == null) {
                    w.println("<p>WARNING: ResourceSchema not found for resource type " + cca.getResourceType() + "</p>");
                } else {
                    h.generateCreateForm(r.getPath(), em);
                }
                w.println();
            }
        }
        
        w.println("\n</div></body></html>\n");
        w.flush();
    }
}
