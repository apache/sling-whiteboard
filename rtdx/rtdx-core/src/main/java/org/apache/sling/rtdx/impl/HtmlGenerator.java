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

package org.apache.sling.rtdx.impl;

import java.io.PrintWriter;
import java.util.Iterator;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.rtdx.api.*;

/** Define a Property of a Resource: name, type, required etc */
public class HtmlGenerator {

    private final PrintWriter w;
    
    // TODO: pagination would be better...
    public static final int MAX_NAV_CHILDREN = 20;
    
    public HtmlGenerator(PrintWriter pw) {
        this.w = pw;
    }
    
    public String addSelectorsAndExtension(SlingHttpServletRequest request, String path) {
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
    
    public void generateNavigation(SlingHttpServletRequest request, Resource r) {
        w.println("<div class='rtdx-navigation'>");
        w.println("<h1>Navigate from " + r.getPath() + " (a " + r.getResourceType() + ")</h1>");
        w.println("<ul>");
        
        if(r.getParent() != null) {
            link("li", "..", addSelectorsAndExtension(request, r.getParent().getPath()));
        }
        
        int availableLinks = MAX_NAV_CHILDREN;
        final Iterator<Resource> it = r.getResourceResolver().listChildren(r);
        while(it.hasNext()) {
            if(availableLinks-- <= 0) {
                w.println("<li><em>Stopping at " + MAX_NAV_CHILDREN + " links</em></li>");
                break;
            }
            final Resource child = it.next();
            link("li", child.getName(), addSelectorsAndExtension(request, child.getPath()));
        }
        w.println("</ul></div>");
    }
    
    public void generateEditForm(Resource r, ResourceModel m) {
        if(m.getProperties().isEmpty()) {
            w.println("<h1 class='rtdx-no-edit-form'>No Edit form: this Resource has no editable fields:" + r.getPath() + "</h1>");
            return;
        }
        form(
            m, 
            r.adaptTo(ValueMap.class), 
            "Edit " + r.getPath() + " (" + m.getDescription() + ")",
            "rtdx-edit-form",
            "", 
            null);
    }
    
    public void generateCreateForm(String parentPath, ResourceModel m) {
        form(
            m, 
            null, 
            "Create a " + m.getDescription() + " here", 
            "rtdx-create-form", 
            parentPath + "/*", 
            "*");
    }
    
     public void form(ResourceModel m, ValueMap vm, String title, String cssClass, String actionPath, String redirectPath) {
        // TODO should use templates
        // TODO escape values
        w.println("<br/>");
        w.println("<div class='" + cssClass + "'>");
        w.println("<h1>" + title + "</h1>");
        w.println("<form method='POST' action='" + actionPath + "' enctype='multipart/form-data'>\n");
        hiddenField("sling:resourceType", m.getName());
        if(redirectPath != null) {
            hiddenField(":redirect", redirectPath);
        }
        for(ResourceProperty p : m.getProperties()) {
            w.println("<br/>");
            inputField(p, vm);
        }
        w.println("<br/><input type='submit'/>");
        w.println("</form></div>");
    }
    
   private void hiddenField(String name, String value) {
        w.println("<input type='hidden' name='" + name + "' value='" + value + "'/>");
    }
    
    private void inputField(ResourceProperty p, ValueMap vm) {
        w.println("<label for'" + p.getName() + "'>" + p.getLabel() + "</>");
        w.print("<input type='text' name='" + p.getName() + "'");
        if(vm != null) {
            final String value = vm.get(p.getName(), String.class);
            if(value != null) {
                // TODO escape
                w.print("value='" + value + "'");
            }
        }
        w.print("/>");
        
    }
    
    private void link(String enclosingElement, String text, String href) {
        w.println("<" + enclosingElement + ">");
        w.println("<a href='" + href + "'>" + text + "</a>");
        w.println("</" + enclosingElement + ">");
    }
}