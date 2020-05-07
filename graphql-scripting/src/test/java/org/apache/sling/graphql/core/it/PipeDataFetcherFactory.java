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

package org.apache.sling.graphql.core.it;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.graphql.api.graphqljava.DataFetcherProvider;

public class PipeDataFetcherFactory implements DataFetcherProvider {

    static class PipeDataFetcher implements DataFetcher<Object> {

        private final Resource r;
        private final String options;

        PipeDataFetcher(Resource r, String name, String options, String source) {
            this.r = r;
            this.options = options;
        }

        @Override
        public Object get(DataFetchingEnvironment environment) throws Exception {
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
    public DataFetcher<Object> createDataFetcher(Resource r, String name, String options, String source) {
        return new PipeDataFetcher(r, name, options, source);
    }
}
