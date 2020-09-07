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

package org.apache.sling.remotecontentapi.aggregator;

import java.io.IOException;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.remotecontentapi.rcaservlet.PipelineContext;
import org.osgi.service.component.annotations.Component;

/** This is a first shot at this remote content API, that
 *  works well with the (simplistic) test content
 *  found under /content/articles
 */
@Component(service = Servlet.class,
    property = {
            "service.description=Apache Sling Content Aggregator Servlet",
            "service.vendor=The Apache Software Foundation",

            "sling.servlet.resourceTypes=sling/servlet/default",
            "sling.servlet.prefix:Integer=-1",

            "sling.servlet.methods=GET",
            "sling.servlet.methods=HEAD",
            "sling.servlet.selectors=s:cagg",
            "sling.servlet.extension=json",
    })
public class DefaultContentAggregatorServlet extends SlingSafeMethodsServlet {
    private static final long serialVersionUID = 1L;

    static class ConverterContext implements ResourceAggregator.Context {
        private final PipelineContext pc;

        ConverterContext(PipelineContext pc) {
            this.pc = pc;
        }

        @Override
        public String getUrlForPath(String path, boolean includeApiSelectorsAndExtension) {
            if(includeApiSelectorsAndExtension) {
                return pc.pathToUrl(path);
            } else {
                return pc.pathToUrlNoJsonExtension(path);
            }
        }

        @Override
        public String getRelativePath(Resource r) {
            return r.getPath().substring(pc.resource.getPath().length());
        }
    }

    @Override
    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        final JsonObjectBuilder json = Json.createObjectBuilder();
        new ResourceAggregator(
            request.getResource(), 
            new ConverterContext(new PipelineContext(request))
        ).addTo(json);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write(json.build().toString());
    }
}