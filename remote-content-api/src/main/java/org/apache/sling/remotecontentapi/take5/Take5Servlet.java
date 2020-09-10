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

package org.apache.sling.remotecontentapi.take5;

import java.io.IOException;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;

/** This is "take 5" of this experiment, using a table of
 *  content processors driven by Resource matchers
 */
@Component(service = Servlet.class,
    property = {
            "sling.servlet.resourceTypes=sling/servlet/default",
            "sling.servlet.prefix:Integer=-1",

            "sling.servlet.methods=GET",
            "sling.servlet.methods=HEAD",
            "sling.servlet.selectors=s:t5",
            "sling.servlet.extension=json",
    })
public class Take5Servlet extends SlingSafeMethodsServlet {
    private static final long serialVersionUID = 1L;
    private final RulesList rules = new RulesList();

    @Override
    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        final JsonObjectBuilder result = Json.createObjectBuilder();
        final UrlBuilder urlb = new UrlBuilder(request);
        final Resource resource = request.getResource();
        
        // Nav and metadata
        final JsonObjectBuilder navigation = Json.createObjectBuilder();
        navigation.add("self", urlb.pathToUrl(resource.getPath()));
        if(resource.getParent() != null) {
            navigation.add("parent", urlb.pathToUrl(resource.getParent().getPath()));
        }
        result.add("navigation", navigation);

        final JsonObjectBuilder metadata = Json.createObjectBuilder();
        metadata.add("_id", resource.getPath());
        result.add("metatada", metadata);

        // Apply the first rule that matches and has a non-null processor
        final JsonObjectBuilder content = Json.createObjectBuilder();
        rules.applyRules(resource, true, content, urlb);
        result.add("content", content);

        // And recurse into children
        final JsonObjectBuilder children = Json.createObjectBuilder();
        for(Resource child : resource.getChildren()) {
            final JsonObjectBuilder childJson = Json.createObjectBuilder();
            if(rules.applyRules(child, false, childJson, urlb)) {
                children.add(child.getName(), childJson);
            }
        }
        result.add("children", children);

        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write(result.build().toString());
    }
}