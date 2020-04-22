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

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.scripting.gql.api.FetcherDefinition;
import org.apache.sling.scripting.gql.api.DataFetcherFactory;

public class PipeDataFetcherFactory implements DataFetcherFactory {

    static class PipeDataFetcher implements DataFetcher<Object> {

        private final FetcherDefinition fetcherDef;

        private final Resource r;

        PipeDataFetcher(FetcherDefinition fetcherDef, Resource r) {
            this.fetcherDef = fetcherDef;
            this.r = r;
        }

        @Override
        public Object get(DataFetchingEnvironment environment) throws Exception {
            String options = fetcherDef.getFetcherOptions();
            if (!options.isEmpty() && !options.equals("$")) {
                throw new IllegalArgumentException("Invalid fetcher options: " + options);
            }

            return r;
        }

    }

    @Override
    public String getNamespace() {
        return "test";
    }

    @Override
    public String getName() {
        return "pipe";
    }

    @Override
    public DataFetcher<Object> createDataFetcher(FetcherDefinition fetcherDef, Resource r) {
        return new PipeDataFetcher(fetcherDef, r);
    }
}
