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
package org.apache.sling.scripting.graphql.it;

import javax.inject.Inject;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;

import org.apache.sling.resource.presence.ResourcePresence;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.factoryConfiguration;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class GraphQLServletIT extends GraphQLScriptingTestSupport {

    @Inject
    @Filter(value = "(path=/content/graphql/two)")
    private ResourcePresence resourcePresence;

    @Configuration
    public Option[] configuration() {
        return new Option[]{
            baseConfiguration(),
            factoryConfiguration("org.apache.sling.resource.presence.internal.ResourcePresenter")
                .put("path", "/content/graphql/two")
                .asOption(),

            // The GraphQL servlet is disabled by default, try setting up two of them
            factoryConfiguration("org.apache.sling.scripting.gql.servlet.GraphQLServlet")
                .put("sling.servlet.resourceTypes", "sling/servlet/default")
                .put("sling.servlet.extensions", "gql")
                .asOption(),
            factoryConfiguration("org.apache.sling.scripting.gql.servlet.GraphQLServlet")
                .put("sling.servlet.resourceTypes", "graphql/test/two")
                .put("sling.servlet.selectors", "testing")
                .put("sling.servlet.extensions", "otherExt")
                .asOption(),
        };
    }

    @Test
    public void testGqlExt() throws Exception {
        final String json = getContent("/graphql/two.gql", "query", "{ currentResource { resourceType name } }");
        assertThat(json, hasJsonPath("$.currentResource.resourceType", equalTo("graphql/test/two")));
        assertThat(json, hasJsonPath("$.currentResource.name", equalTo("two")));
        assertThat(json, hasNoJsonPath("$.currentResource.path"));
    }

    @Test
    public void testOtherExt() throws Exception {
        final String json = getContent("/graphql/two.testing.otherExt", "query", "{ currentResource { path name } }");
        assertThat(json, hasJsonPath("$.currentResource.path", equalTo("/content/graphql/two")));
        assertThat(json, hasJsonPath("$.currentResource.name", equalTo("two")));
        assertThat(json, hasNoJsonPath("$.currentResource.resourceType"));
        executeRequest("GET", "/graphql/two.otherExt", null, 404);
    }

    @Test
    public void testMissingQuery() throws Exception {
        executeRequest("GET", "/graphql/two.gql", null, 400);
    }

    @Test
    public void testDefaultJson() throws Exception {
        final String json = getContent("/graphql/two.json");
        assertThat(json, hasJsonPath("$.title", equalTo("GraphQL two")));
        assertThat(json, hasJsonPath("$.jcr:primaryType", equalTo("nt:unstructured")));
    }
}
