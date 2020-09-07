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

package org.apache.sling.remotecontentapi.rcaservlet;

import java.io.IOException;

import javax.json.JsonObject;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/** This is a first shot at this remote content API, that
 *  works well with the (simplistic) test content
 *  found under /content/articles
 */
@Component(service = Servlet.class,
    property = {
            "service.description=Sling Remote Content Access Servlet",
            "service.vendor=The Apache Software Foundation",

            "sling.servlet.resourceTypes=sling/servlet/default",
            "sling.servlet.prefix:Integer=-1",

            "sling.servlet.methods=GET",
            "sling.servlet.methods=HEAD",
            "sling.servlet.selectors=s:rca",
            "sling.servlet.extension=json",
    })
public class RemoteContentAccessServlet extends SlingSafeMethodsServlet {
    private static final long serialVersionUID = 1L;

    @Reference
    private ServletResolver servletResolver;

    @Override
    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        final PipelineContext pc = new PipelineContext(request);

        try {
            new MetadataProcessor().process(pc);
            new ChildrenProcessor().process(pc);
            new ContentProcessor(servletResolver).process(pc);
        } catch(IOException ioe) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ioe.getMessage());
            return;
        }
        
        final JsonObject json = pc.build();

        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write(json.toString());
    }
}