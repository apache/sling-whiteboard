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
package org.apache.sling.scripting.gql.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.UUID;

import org.apache.sling.api.resource.Resource;
import org.junit.Test;
import org.mockito.Mockito;

import graphql.ExecutionResult;

public class GraphQLResourceQueryTest {
   @Test
    public void basicTest() throws Exception {
        final String path = "/some/path/" + UUID.randomUUID();
        final Resource r = Mockito.mock(Resource.class);
        Mockito.when(r.getPath()).thenReturn(path);

        final GraphQLResourceQuery q = new GraphQLResourceQuery();
        final ExecutionResult result = q.executeQuery(r, "{ currentResource { path } }");

        if(!result.getErrors().isEmpty()) {
            fail("Errors:" + result.getErrors());
        }
        final String expected = "{currentResource={path=" + path + "}}";
        assertEquals(expected, result.getData().toString());
    }
}
