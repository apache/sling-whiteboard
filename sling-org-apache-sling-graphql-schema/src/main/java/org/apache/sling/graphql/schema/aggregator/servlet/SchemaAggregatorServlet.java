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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.graphql.schema.aggregator.api.SchemaAggregator;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final Logger log = LoggerFactory.getLogger(getClass().getName());

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

        @AttributeDefinition(
            name = "Selectors to partials mapping",
            description="Each entry is in the format S:P1,P2,... where S is a selector and P* the names of the corresponding schema partials")
        String[] selectors_to_partials_mapping() default {};

    }
    
    @Reference
    private transient SchemaAggregator aggregator;

    private Map<String, String[]> selectorsToPartialNames = new HashMap<>();

    @Activate
    public void activate(BundleContext ctx, Config cfg) {
        for(String str : cfg.selectors_to_partials_mapping()) {
            final String [] parts = str.split("[:,]");
            if(parts.length < 2) {
                log.warn("Invalid selectors_to_partials_mapping configuration string [{}]", str);
                continue;
            }
            final String selector = parts[0].trim();
            final String [] names = new String[parts.length - 1];
            for(int i=1; i < parts.length; i++) {
                names[i-1] = parts[i].trim();
            }
            if(log.isInfoEnabled()) {
                log.info("Registering selector mapping: {} -> {}", selector, Arrays.asList(names));
            }
            selectorsToPartialNames.put(selector, names);
        }
    }

    @Override
    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        final String [] selectors = request.getRequestPathInfo().getSelectors();
        if(selectors.length < 1) {
           response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required schema selector");
           return;
        }

        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");

        final String key = selectors[0];
        final String[] partialNames = selectorsToPartialNames.get(key);
        if(partialNames == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No partial names defined for selector " + key);
            return;
        }
        if(log.isDebugEnabled()) {
            log.debug("Selector {} maps to partial names {}", key, Arrays.asList(partialNames));
        }
        aggregator.aggregate(response.getWriter(), partialNames);
    }
}
