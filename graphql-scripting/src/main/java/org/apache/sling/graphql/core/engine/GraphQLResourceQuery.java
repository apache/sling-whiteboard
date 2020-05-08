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

package org.apache.sling.graphql.core.engine;

import javax.script.ScriptException;

import graphql.ExecutionInput;
import graphql.language.Comment;
import graphql.language.FieldDefinition;
import graphql.language.ObjectTypeDefinition;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.graphql.api.SchemaProvider;
import org.apache.sling.graphql.core.schema.DataFetcherDefinition;
import org.apache.sling.graphql.core.schema.DataFetcherSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Run a GraphQL query in the context of a Sling Resource */
public class GraphQLResourceQuery {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public ExecutionResult executeQuery(SchemaProvider schemaProvider, DataFetcherSelector fetchers,
                                        Resource r, String query) throws ScriptException {
        return executeQuery(schemaProvider, fetchers, r, query, Collections.emptyMap());
    }

    public ExecutionResult executeQuery(SchemaProvider schemaProvider, DataFetcherSelector fetchers,
                                        Resource r, String query, Map<String, Object> variables) throws ScriptException {
        if(r == null) {
            throw new ScriptException("Resource is null");
        }
        if(query == null) {
            throw new ScriptException("Query is null");
        }
        if(schemaProvider == null) {
            throw new ScriptException("SchemaProvider is null");
        }

        String schemaDef = null;
        try {
            schemaDef = schemaProvider.getSchema(r, null);
        } catch(Exception e) {
            final ScriptException up = new ScriptException("Schema provider failed");
            up.initCause(e);
            throw up;
        }
        log.info("Resource {} maps to GQL schema {}", r.getPath(), schemaDef);
        try {
            final GraphQLSchema schema = buildSchema(schemaDef, fetchers, r);
            final GraphQL graphQL = GraphQL.newGraphQL(schema).build();
            ExecutionInput ei = ExecutionInput.newExecutionInput()
                .query(query)
                .variables(variables)
                .build();
            final ExecutionResult result = graphQL.execute(ei);
            return result;
        } catch(Exception e) {
            final ScriptException up = new ScriptException(
                String.format("Query failed for Resource %s: schema=%s, query=%s", r.getPath(), schemaDef, query));
            up.initCause(e);
            throw up;                
        }
    }

    private GraphQLSchema buildSchema(String sdl, DataFetcherSelector fetchers, Resource r) throws IOException {
        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(sdl);
        RuntimeWiring runtimeWiring = buildWiring(typeRegistry, fetchers, r);
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        return schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
    }

    private RuntimeWiring buildWiring(TypeDefinitionRegistry typeRegistry, DataFetcherSelector fetchers, Resource r)
        throws IOException {
        List<ObjectTypeDefinition> types = typeRegistry.getTypes(ObjectTypeDefinition.class);
        RuntimeWiring.Builder builder = RuntimeWiring.newRuntimeWiring();
        for (ObjectTypeDefinition type : types) {

            builder.type(type.getName(), typeWiring -> {
                for (FieldDefinition field : type.getFieldDefinitions()) {

                    try {
                        DataFetcher<Object> fetcher = getDataFetcher(field, fetchers, r);
                        if (fetcher != null) {
                            typeWiring.dataFetcher(field.getName(), fetcher);
                        }
                    } catch(IOException e) {
                        throw new RuntimeException("Exception while building wiring", e);
                    }
                }
                return typeWiring;
            });
        }
        return builder.build();
    }

    private DataFetcher<Object> getDataFetcher(FieldDefinition field, DataFetcherSelector fetchers,
                                               Resource r) throws IOException {
        List<Comment> comments = field.getComments();
        for (Comment comment : comments) {

            String commentStr = comment.getContent();
            if (commentStr.startsWith("#")) {
                commentStr = commentStr.substring(1).trim();

                try {
                    DataFetcherDefinition def = new DataFetcherDefinition(commentStr);
                    DataFetcher<Object> fetcher = fetchers.getDataFetcherForType(def, r);
                    if (fetcher != null) {
                        return fetcher;
                    } else {
                        log.warn("No data fetcher registered for {}", def.toString());
                    }
                } catch (IllegalArgumentException iae) {
                    log.warn("Invalid fetcher definition", iae);
                }
            }
        }
        return null;
    }

}
