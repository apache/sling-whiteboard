
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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.scripting.gql.engine.GraphQLResourceQuery;
import org.apache.sling.scripting.gql.schema.GraphQLSchemaProvider;
import org.apache.sling.scripting.gql.engine.GraphQLScriptEngine;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
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
    immediate = true,
    configurationPolicy=ConfigurationPolicy.REQUIRE,
    property = {
        "service.description=Sling GraphQL Servlet",
        "service.vendor=The Apache Software Foundation"
    })
@Designate(ocd = GraphQLServlet.Config.class, factory=true)
public class GraphQLServlet extends SlingAllMethodsServlet {
    private static final long serialVersionUID = 1L;
    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String P_QUERY = "query";

    @ObjectClassDefinition(
        name = "Apache Sling GraphQL Servlet",
        description = "Servlet that implements GraphQL endpoints")
    public @interface Config {
        @AttributeDefinition(
            name = "Selectors",
            description="Standard Sling servlet property")
        String[] sling_servlet_selectors() default "";

        @AttributeDefinition(
            name = "Resource Types",
            description="Standard Sling servlet property")
        String[] sling_servlet_resourceTypes() default "sling/servlet/default";

        @AttributeDefinition(
            name = "Methods",
            description="Standard Sling servlet property")
        String[] sling_servlet_methods() default "GET";

        @AttributeDefinition(
            name = "Extensions",
            description="Standard Sling servlet property")
        String[] sling_servlet_extensions() default "gql";
    }

    @Reference
    private GraphQLSchemaProvider schemaProvider;

    @Override
    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        execute(request.getResource(), request, response);
    }

    @Override
    public void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        execute(request.getResource(), request, response);
    }

    private void execute(Resource resource, SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        final String query = request.getParameter(P_QUERY);
        if(query == null || query.trim().length() == 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing request parameter:" + P_QUERY);
            return;
        }

        try {
            final GraphQLResourceQuery q = new GraphQLResourceQuery();
            final ExecutionResult result = q.executeQuery(schemaProvider, resource, query);
            GraphQLScriptEngine.sendJSON(response.getWriter(), result);
        } catch(Exception ex) {
            throw new IOException(ex);
        } finally {
            response.getWriter().flush();
        }
    }
}