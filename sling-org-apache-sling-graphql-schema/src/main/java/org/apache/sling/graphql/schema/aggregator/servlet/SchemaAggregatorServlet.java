/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Licensed to the Apache Software Foundation (ASF) under one
 ~ or more contributor license agreements.  See the NOTICE file
 ~ distributed with this work for additional information
 ~ regarding copyright ownership.  The ASF licenses this file
 ~ to you under the Apache License, Version 2.0 (the
 ~ "License"); you may not use this file except in compliance
 ~ with the License.  You may obtain a copy of the License at
 ~
 ~   http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

package org.apache.sling.graphql.schema.aggregator.servlet;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.graphql.schema.aggregator.api.SchemaAggregator;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(
    service = Servlet.class,
    name = "org.apache.sling.graphql.schema.aggregator.SchemaAggregatorServlet",
    immediate = true,
    configurationPolicy=ConfigurationPolicy.REQUIRE,
    property = {
        "service.description=Sling GraphQL Schema Aggregator Servlet",
        "service.vendor=The Apache Software Foundation"
    })
@Designate(ocd = SchemaAggregatorServlet.Config.class, factory=true)

public class SchemaAggregatorServlet extends SlingSafeMethodsServlet {

    @ObjectClassDefinition(
        name = "Apache Sling GraphQL Schema Aggregator Servlet",
        description = "Servlet that aggregates GraphQL schemas")
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
        String[] sling_servlet_extensions() default "GQLschema";
    }
    
    @Reference
    private transient SchemaAggregator aggregator;

    @Override
    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        final String [] selectors = request.getRequestPathInfo().getSelectors();
        if(selectors.length < 1) {
           response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required schema selector");
           return;
        }

        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        aggregator.aggregate(response.getWriter(), selectors);
    }
}
