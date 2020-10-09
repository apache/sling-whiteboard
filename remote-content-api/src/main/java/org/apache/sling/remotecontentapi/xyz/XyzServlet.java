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

package org.apache.sling.remotecontentapi.xyz;

import java.io.IOException;

import javax.json.JsonObject;
import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/** Experimenting with dynamic generators, which might become
 *  script-driven.
 */
@Component(service = Servlet.class,
    property = {
            "service.description=Sling XYZ Servlet",
            "service.vendor=The Apache Software Foundation",

            "sling.servlet.resourceTypes=sling/servlet/default",
            "sling.servlet.prefix:Integer=-1",

            "sling.servlet.methods=GET",
            "sling.servlet.methods=HEAD",
            "sling.servlet.selectors=s:xyz",
            "sling.servlet.extension=json",
    })
public class XyzServlet extends SlingSafeMethodsServlet {
    private static final long serialVersionUID = 1L;

    @Reference
    private ServletResolver servletResolver;

    @Override
    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        final XyzContext context = new XyzContext(request);

        final Resource r = request.getResource();
        final UrlBuilder urlb = new UrlBuilder(request);
        final ProcessingRuleSelector s = new ProcessingRuleSelector(servletResolver);
        s.getRule(ProcessingRuleSelector.RuleType.METADATA, r).process(context.metadata, urlb);
        s.getRule(ProcessingRuleSelector.RuleType.CHILDREN, r).process(context.children, urlb);
        s.getRule(ProcessingRuleSelector.RuleType.CONTENT, r).process(context.content, urlb);

        final JsonObject json = context.build();

        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write(json.toString());
    }
}