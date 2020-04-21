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
package org.apache.sling.scripting.gql.engine;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import static org.hamcrest.Matchers.equalTo;

import java.util.UUID;

import com.google.gson.Gson;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.scripting.gql.schema.GraphQLSchemaProvider;
import org.junit.Test;
import org.mockito.Mockito;

import graphql.ExecutionResult;

public class GraphQLResourceQueryTest {
    private final GraphQLSchemaProvider schemaProvider = new MockSchemaProvider();

    @Test
    public void basicTest() throws Exception {
        final String resourceType = "RT-" + UUID.randomUUID();
        final String path = "/some/path/" + UUID.randomUUID();
        final Resource r = Mockito.mock(Resource.class);
        Mockito.when(r.getPath()).thenReturn(path);
        Mockito.when(r.getResourceType()).thenReturn(resourceType);

        final GraphQLResourceQuery q = new GraphQLResourceQuery();
        final ExecutionResult result = q.executeQuery(schemaProvider, r, "{ currentResource { path resourceType } }");

        if (!result.getErrors().isEmpty()) {
            fail("Errors:" + result.getErrors());
        }

        final String json = new Gson().toJson(result);
        assertThat(json, hasJsonPath("$.data.currentResource"));
        assertThat(json, hasJsonPath("$.data.currentResource.path", equalTo(path)));
        assertThat(json, hasJsonPath("$.data.currentResource.resourceType", equalTo(resourceType)));
    }
}
