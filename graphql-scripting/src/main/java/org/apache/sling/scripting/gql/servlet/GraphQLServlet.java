
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

package org.apache.sling.scripting.gql.servlet;

import java.io.IOException;

import javax.script.ScriptException;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.scripting.gql.engine.GraphQLResourceQuery;
import org.apache.sling.scripting.gql.engine.GraphQLSchemaProvider;
import org.apache.sling.scripting.gql.engine.GraphQLScriptEngine;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.ExecutionResult;

/** Servlet that can be activated to implement the standard
 *  GraphQL "protocol" as per https://graphql.org/learn/serving-over-http/
 * 
 *  This servlet is only active if the corresponding OSGi configurations
 *  are created. This allows is to be mounted either on a path to support
 *  the "traditional" GraphQL single-endpoint mode, or on specific resource
 *  types and selectors to turn specific Sling Resources into GraphQL 
 *  endpoints.
 */

@Component(
    service = Servlet.class,
    property = {
         // TODO the servlet should not be mounted by default...just testing it out for now
         "sling.servlet.resourceTypes=sling/servlet/default",
         "sling.servlet.methods=GET",
         "sling.servlet.methods=HEAD",
         "sling.servlet.extensions=gql"
    }
)
public class GraphQLServlet extends SlingAllMethodsServlet {
    private static final long serialVersionUID = 1L;
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private GraphQLSchemaProvider schemaProvider;

    static class RequestParams {
        final String query;

        RequestParams(SlingHttpServletRequest request) {
            final String q = request.getParameter("query");

            // TODO hack for now...
            final String defaultQuery = "{ currentResource { path resourceType } }";
            query = (q != null && q.trim().length() > 0) ? q.trim() : defaultQuery;
        }

        @Override
        public String toString() {
            return String.format("Query=%s", query);
        }
    }

    @Override
    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        execute(request.getResource(), new RequestParams(request), response);
    }

    @Override
    public void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        execute(request.getResource(), new RequestParams(request), response);
    }

    private void execute(Resource resource, RequestParams params, SlingHttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            final GraphQLResourceQuery q = new GraphQLResourceQuery();
            final ExecutionResult result = q.executeQuery(schemaProvider, resource, params.query);
                GraphQLScriptEngine.sendJSON(response.getWriter(), result);
        } catch(Exception ex) {
            throw new IOException(ex);
        } finally {
            response.getWriter().flush();
        }
    }
}