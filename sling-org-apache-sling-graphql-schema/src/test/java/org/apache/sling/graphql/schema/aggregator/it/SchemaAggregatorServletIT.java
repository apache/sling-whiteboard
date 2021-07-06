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
package org.apache.sling.graphql.schema.aggregator.it;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.factoryConfiguration;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SchemaAggregatorServletIT extends SchemaAggregatorTestSupport {
    private static final String AGGREGATOR_SERVLET_CONFIG_PID = "org.apache.sling.graphql.schema.aggregator.SchemaAggregatorServlet";
    private static final String GQL_SCHEMA_EXT = "GQLschema";

    @Configuration
    public Option[] configuration() {
        return new Option[]{
            baseConfiguration(),

            // The aggregator servlet is disabled by default
            factoryConfiguration(AGGREGATOR_SERVLET_CONFIG_PID)
                .put("sling.servlet.resourceTypes", "sling/servlet/default")
                .put("sling.servlet.extensions", GQL_SCHEMA_EXT)
                .put("sling.servlet.selectors", new String[] { "X", "Y" })
                .put("sling.servlet.methods", new String[] { "GET" })
                .asOption(),
        };
    }

    @Test
    public void servletIsActive() throws Exception {
        // TODO this doesn't actually test the servlet so far
        //assertEquals("Not a schema yet, for providers [X]", getContent("/." + GQL_SCHEMA_EXT));
        getContent("/.json");
    }

}