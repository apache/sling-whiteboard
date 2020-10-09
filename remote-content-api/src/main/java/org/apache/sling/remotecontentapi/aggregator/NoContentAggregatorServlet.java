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

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;

/** Servlet that output no content, for resource types like
 *  folders who do not have their own content.
 *  (TODO: those can have titles and descriptions, should output that)
 */
@Component(service = Servlet.class,
    property = {
            "service.description=Apache Sling Content Aggregator Servlet",
            "service.vendor=The Apache Software Foundation",

            "sling.servlet.resourceTypes=sling/Folder",
            "sling.servlet.resourceTypes=sling/OrderedFolder",
            "sling.servlet.prefix:Integer=-1",

            "sling.servlet.methods=GET",
            "sling.servlet.methods=HEAD",
            "sling.servlet.selectors=s:cagg",
            "sling.servlet.extension=json",
    })
public class NoContentAggregatorServlet extends SlingSafeMethodsServlet {
    private static final long serialVersionUID = 1L;

    @Override
    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
    }
}