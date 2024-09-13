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
package org.apache.sling.mdresource.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.owasp.encoder.Encode;

@SlingServletResourceTypes(
    resourceTypes="sling/markdown/file",
    methods="GET",
    extensions="html"
)
@Component(service=Servlet.class)
@Designate(ocd = HtmlServlet.Config.class)
public class HtmlServlet extends HttpServlet {

    @ObjectClassDefinition(name = "Apache Sling Markdown HTML Servlet", description = "Servlet to render Markdown files as HTML")
    public @interface Config {

        @AttributeDefinition(name = "Header Resource", description = "Path to the header resource")
        String header_resource();

        @AttributeDefinition(name = "Footer Resource", description = "Path to the footer resource")
        String footer_resource();

        @AttributeDefinition(name = "Head Content", description = "Content to be added to the <head> section")
        String head_contents();

        @AttributeDefinition(name = "HTML Elements Property", description = "Name of the property holding the HTML elements")
        String html_elements_property() default "html-elements";
    }

    private final Config cfg;

    @Activate
    public HtmlServlet(final Config cfg) {
        this.cfg = cfg;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
    throws ServletException, IOException {
        final SlingHttpServletRequest request = (SlingHttpServletRequest) req;
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        final PrintWriter pw = resp.getWriter();
        pw.println("<html>");
        pw.println("  <head>");
        final ValueMap props = request.getResource().getValueMap();
        String title = props.get("jcr:title", String.class);
        if ( title == null ) {
            title = props.get("title", String.class);
        }
        if (title != null) {
            pw.print("    <title>");
            pw.print(Encode.forHtmlContent(title));
            pw.println("</title>");
        }
        if (this.cfg.head_contents() != null) {
            pw.println(this.cfg.head_contents());
        }
        pw.println("  </head>");
        pw.println("  <body>");
        pw.println("    <header>");
        if (this.cfg.header_resource() != null) {
            request.getRequestDispatcher(this.cfg.header_resource()).include(request, resp);
        }
        pw.println("    </header>");
        pw.println("    <main>");
        final Object html = props.get(this.cfg.html_elements_property());
        if (html instanceof String) {
            pw.println(html.toString());
        } else if (html instanceof List) {
            boolean startSection = true;
            for (final Map.Entry<String, String> element : (List<Map.Entry<String,String>>) html) {
                if (startSection) {
                    pw.println("      <div class=\"section\">");
                    startSection = false;
                }
                pw.print(element.getValue());
            }
            if (!startSection) {
                pw.println("      </div>");
            }
        }
        pw.println("    </main>");
        pw.println("    <footer>");
        if (this.cfg.footer_resource() != null) {
            request.getRequestDispatcher(this.cfg.footer_resource()).include(request, resp);
        }
        pw.println("    </footer>");
        pw.println("  </body>");
        pw.println("</html>");
    }
}
