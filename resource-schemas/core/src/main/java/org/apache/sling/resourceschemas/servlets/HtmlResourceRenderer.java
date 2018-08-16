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
    
    private void html(String formatString, String ... variables) {
        for(int i=0 ; i < variables.length; i++) {
            variables[i] = ResponseUtil.escapeXml(variables[i]);
        }
        w.print(String.format(formatString, (Object [])variables));
    }
 
    @Override
    public void renderPrefix(Resource r, ResourceSchema s) throws IOException {
        html("<html>\n");
        html("<head>\n<meta charset=\"utf-8\">\n");
        html("<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n");
        html("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
        html("<link href=\"/content/starter/css/bundle.css\" rel=\"stylesheet\">\n");
        html("<body>\n<div id='srs-page'>\n");
        html("<h1>Sling Resource Schemas: generated edit forms<br/>for %s </h1>\n<hr/>\n", r.getPath());
    }
    
    @Override
    public void renderSuffix(Resource r, ResourceSchema s) throws IOException {
        html("\n</div>\n</body>\n</html>\n");
    }
    
    public void renderContent(Resource r, ResourceSchema s) throws IOException {
        html("\n<div id='srs-content'>\n");
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
        html("</div>\n");
    }
    
    public void renderNavigationItems(Resource r, NavigationItem ... items) throws IOException {
        html("\n<div id='srs-navigation'>\n<h2>Navigation</h2>\n");
        html("<ul>\n");
        
        for(NavigationItem i : items) {
            final String prefix = i.type == NavigationType.PARENT ? "^" : "-";
            link(prefix + " " + i.resource.getName(), addSelectorsAndExtension(request, i.resource.getPath()));
        }
        
        html("</ul>\n</div>\n");
    }
    
    public void renderActions(Resource r, ResourceAction ... actions) throws IOException {
        html("\n<div id='srs-actions'>\n");

        if(actions.length == 0) {
            html("<h2>No Actions available here</h2>");
        } else {
            html("<h2>Actions</h2>");
        }
        
        for(ResourceAction act : actions) {
            if(act instanceof CreateChildAction) {
                final CreateChildAction cca = (CreateChildAction)act;
                generateCreateForm(r.getPath(), registry.getSchema(cca.getResourceType()));
            }
        }
        html("</div>");
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
        html("\n<div class='%s'>\n", cssClass);
        if(title != null) {
            html("<h2>%s</h2>\n", title);
        }
        html("<p>%s</p>\n", subtitle);
        html("<form method='POST' action='%s' enctype='multipart/form-data'>\n", actionPath);
        hiddenField("sling:resourceType", ResponseUtil.escapeXml(m.getName()));
        if(redirectPath != null) {
            hiddenField(":redirect", ResponseUtil.escapeXml(redirectPath));
        }
        if(formMarkerFieldName != null) {
            hiddenField(formMarkerFieldName, "");
        }
        for(ResourceProperty p : m.getProperties()) {
            html("<br/>\n");
            inputField(p, vm);
        }
        html("<br/>\n<input type='submit'/>\n");
        html("</form>\n</div>\n");
    }
    
   private void hiddenField(String name, String value) {
        html("<input type='hidden' name='%s' value='%s'/>\n", name, value);
    }
    
    private void inputField(ResourceProperty p, ValueMap vm) {
        html("<label for='%s'>%s</label>", p.getName(), p.getLabel());
        html("<input type='text' name='%s'", p.getName());
        if(vm != null) {
            final String value = vm.get(p.getName(), String.class);
            if(value != null) {
                html(" value='%s'", value);
            }
        }
        html("/>\n");
        
    }
    
    private void link(String text, String href) {
        html("<li>");
        html("<a href='%s'>%s</a>", href, text);
        html("</li>\n");
    }
}