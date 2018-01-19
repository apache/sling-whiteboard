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

package org.apache.sling.rtdx.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.rtdx.api.ResourceModel;
import org.apache.sling.rtdx.api.ResourceModelRegistry;
import org.apache.sling.rtdx.impl.HtmlGenerator;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/** SlingServlet that generates the default HTML representations
 *  to allow CRUD operations on resources backed by RTD-X models.
 */
@Component(service = Servlet.class,
    property = {
            "service.description=RTD-X Default HTML Servlet",
            "service.vendor=The Apache Software Foundation",
            "sling.servlet.selectors=rtdx",
            "sling.servlet.extensions=html",
            "sling.servlet.resourceTypes=sling/servlet/default",
            "sling.servlet.methods=GET"
    })
public class DefaultHtmlServlet extends SlingSafeMethodsServlet {

    @Reference
    private ResourceModelRegistry registry;
    
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {

        final Resource r = request.getResource();
        final String resourceType = r.getResourceType();
        final ResourceModel m = registry.getModel(resourceType);
        if(m == null) {
            response.sendError(
                HttpServletResponse.SC_BAD_REQUEST,
                "ResourceModel not found for resource type " + resourceType + " at " + request.getResource().getPath());
            return;
        }
        
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html");
        
        final PrintWriter w = response.getWriter();
        w.println("<html><body><div class='rtdx-page'><h1>RTD-X generated editing forms</h1><hr/>\n");
        
        final HtmlGenerator h = new HtmlGenerator(request, w);
        h.generateNavigation(r);
        h.generateEditForm(r, m);
        w.println();
        
        for(String rt : m.getPostHereResourceTypes()) {
            final ResourceModel em = registry.getModel(rt);
            if(em == null) {
                w.println("<p>WARNING: model not found for resource type " + rt + "</p>");
            } else {
                h.generateCreateForm(r.getPath(), em);
            }
            w.println();
        }
        
        w.println("\n</div></body></html>\n");
        w.flush();
    }
}
