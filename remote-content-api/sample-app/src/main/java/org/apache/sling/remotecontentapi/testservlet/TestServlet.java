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

package org.apache.sling.remotecontentapi.testservlet;

import java.io.IOException;

import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.contentmapper.api.ContentMapper;
import org.apache.sling.contentmapper.api.MappingTarget;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/** Render the current Resource to JSON */
@Component(service = Servlet.class,
    property = {
            "sling.servlet.resourceTypes=sling/servlet/default",
            "sling.servlet.prefix:Integer=-1",

            "sling.servlet.methods=GET",
            "sling.servlet.methods=HEAD",
            "sling.servlet.selectors=rakam",
            "sling.servlet.extension=json",
    })
public class TestServlet extends SlingSafeMethodsServlet {
    private static final long serialVersionUID = 1L;

    @Reference(target="(" + MappingTarget.TARGET_TYPE + "=json)")
    private transient MappingTarget mappingTarget;

    @Reference(target="(" + ContentMapper.ROLE + "=api)")
    private transient ContentMapper contentMapper;

    @Override
    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {

        MappingTarget.TargetNode target = mappingTarget.newTargetNode();
        contentMapper.map(request.getResource(), target, new TestUrlBuilder(request));
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write(target.adaptTo(String.class));
    }
}