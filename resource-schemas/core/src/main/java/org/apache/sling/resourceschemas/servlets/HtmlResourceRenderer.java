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
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.resourceschemas.api.*;
import org.apache.sling.api.request.ResponseUtil;


/** Define a Property of a Resource: name, type, required etc */
class HtmlResourceRenderer implements ResourceRenderer {

    private final SlingHttpServletRequest request;
    private final PrintWriter w;
    private final ResourceSchemaRegistry registry;
    
    HtmlResourceRenderer(ResourceSchemaRegistry rsr, SlingHttpServletRequest request, PrintWriter writer) {
        this.request = request;
        this.w = writer;
        this.registry = rsr;
    }
 
    @Override
    public void renderPrefix(Resource r, ResourceSchema s) throws IOException {
        w.println("<html><body><div class='srs-page'>");
        w.println("<h1>Sling Resource Schemas: generated edit forms<br/>for " + r.getPath() + "</h1><hr/>\n");
    }
    
    @Override
    public void renderSuffix(Resource r, ResourceSchema s) throws IOException {
        w.println("\n</div></body></html>\n");
    }
    
    public void renderContent(Resource r, ResourceSchema s) throws IOException {
        if(s.getProperties().isEmpty()) {
            return;
        }
        form(
            s, 
            r.adaptTo(ValueMap.class), 
            "Edit the current Resource",
            r.getPath() + " (" + s.getDescription() + ")",
            "srs-edit-form",
            "", 
            addSelectorsAndExtension(request, r.getPath()),
            null);
    }
    
    public void renderNavigationItems(Resource r, NavigationItem ... items) throws IOException {
        w.println("<div class='srs-navigation'><h2>Navigation</h2>");
        w.println("<ul>");
        
        for(NavigationItem i : items) {
            final String prefix = i.type == NavigationType.PARENT ? "^" : "-";
            link("ul", prefix + " " + i.resource.getName(), addSelectorsAndExtension(request, i.resource.getPath()));
        }
        
        w.println("</ul></div>");
    }
    
    public void renderActions(Resource r, ResourceAction ... actions) throws IOException {
        
        if(actions.length == 0) {
            w.println("<h2>No Actions available here</h2>");
        } else {
            w.println("<h2>Actions</h2>");
        }
        
        for(ResourceAction act : actions) {
            if(act instanceof CreateChildAction) {
                final CreateChildAction cca = (CreateChildAction)act;
                generateCreateForm(r.getPath(), registry.getSchema(cca.getResourceType()));
            }
        }
    }
    
    private String addSelectorsAndExtension(SlingHttpServletRequest request, String path) {
        final StringBuilder sb = new StringBuilder(path);
        for(String selector : request.getRequestPathInfo().getSelectors()) {
            sb.append(".").append(selector);
        }
        final String ext = request.getRequestPathInfo().getExtension();
        if(ext != null && ext.length() > 0) {
            sb.append(".").append(ext);
        }
        return sb.toString();
    }
    
    private void generateCreateForm(String parentPath, ResourceSchema m) {
        form(
            m, 
            null,
            null,
            "Create a " + m.getDescription() + " here", 
            "srs-create-form", 
            parentPath + "/*", 
            "*",
            ResourceSchemasConstants.SRS_FORM_MARKER_PARAMETER);
    }
    
    private void form(ResourceSchema m, ValueMap vm, String title, String subtitle, String cssClass, String actionPath, String redirectPath, String formMarkerFieldName) {
        // TODO should use templates
        // TODO escape values
        w.println("<br/>");
        w.println("<div class='" + cssClass + "'>");
        if(title != null) {
            w.println("<h2>" + ResponseUtil.escapeXml(title) + "</h2>");
        }
        w.println("<p>" + ResponseUtil.escapeXml(subtitle) + "</p>");
        w.println("<form method='POST' action='" + ResponseUtil.escapeXml(actionPath) + "' enctype='multipart/form-data'>\n");
        hiddenField("sling:resourceType", ResponseUtil.escapeXml(m.getName()));
        if(redirectPath != null) {
            hiddenField(":redirect", ResponseUtil.escapeXml(redirectPath));
        }
        if(formMarkerFieldName != null) {
            hiddenField(formMarkerFieldName, "");
        }
        for(ResourceProperty p : m.getProperties()) {
            w.println("<br/>");
            inputField(p, vm);
        }
        w.println("<br/><input type='submit'/>");
        w.println("</form></div>");
    }
    
   private void hiddenField(String name, String value) {
        w.println("<input type='hidden' name='" + ResponseUtil.escapeXml(name) + "' value='" + ResponseUtil.escapeXml(value) + "'/>");
    }
    
    private void inputField(ResourceProperty p, ValueMap vm) {
        w.println("<label for='" + ResponseUtil.escapeXml(p.getName()) + "'>" + ResponseUtil.escapeXml(p.getLabel()) + "</label>");
        w.print("<input type='text' name='" + ResponseUtil.escapeXml(p.getName()) + "'");
        if(vm != null) {
            final String value = vm.get(p.getName(), String.class);
            if(value != null) {
                // TODO escape
                w.print(" value='" + ResponseUtil.escapeXml(value) + "'");
            }
        }
        w.print("/>");
        
    }
    
    private void link(String enclosingElement, String text, String href) {
        w.println("<" + enclosingElement + ">");
        w.println("<a href='" + ResponseUtil.escapeXml(href) + "'>" + ResponseUtil.escapeXml(text) + "</a>");
        w.println("</" + enclosingElement + ">");
    }
}