
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

import java.util.Date;

import org.apache.sling.api.resource.Resource;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.TypeRuntimeWiring;

/** Run a GraphQL query in the context of a Sling Resource */
class GraphQLResourceQuery {

    static class EchoDataFetcher implements DataFetcher<Object> {
        private final Object data;

        EchoDataFetcher(Object data) {
            this.data = data;
        }
        @Override
        public Object get(DataFetchingEnvironment environment) throws Exception {
            return data;
        }
    }

    ExecutionResult executeQuery(Resource r, String query) {
        final String schemaDef = 
            "type Query { currentResource : SlingResource }\n"
            + "type SlingResource { path: String }\n"
        ;

        final GraphQLSchema schema = buildSchema(schemaDef, r);
        final GraphQL graphQL = GraphQL.newGraphQL(schema).build();
        final ExecutionResult result = graphQL.execute(query);
        return result;
    }

    private GraphQLSchema buildSchema(String sdl, Resource r) {
        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(sdl);
        RuntimeWiring runtimeWiring = buildWiring(r);
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        return schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
    }

    private RuntimeWiring buildWiring(Resource r) {
        // TODO data should pass via the DataFetchingEnvironment...
        final String path = r == null ? "NO_PATH" : r.getPath();
        return RuntimeWiring.newRuntimeWiring()
            .type(TypeRuntimeWiring.newTypeWiring("Query").dataFetcher("currentResource", new EchoDataFetcher("DummyCurrentResourceResult")).build())
            .type(TypeRuntimeWiring.newTypeWiring("SlingResource").dataFetcher("path", new EchoDataFetcher(path)).build())
            .build()
        ;
    }
}
