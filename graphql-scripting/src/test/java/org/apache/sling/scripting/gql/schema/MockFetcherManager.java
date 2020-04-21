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

package org.apache.sling.scripting.gql.schema;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.sling.api.resource.Resource;

import java.util.LinkedHashMap;
import java.util.Map;

public class MockFetcherManager extends FetcherManager {

    static class EchoDataFetcher implements DataFetcher<Object> {

        private final Resource r;

        EchoDataFetcher(Resource r) {
            this.r = r;
        }

        @Override
        public Object get(DataFetchingEnvironment environment) {
            return r;
        }

    }

    static class EchoSlingDataFetcher implements SlingDataFetcher {

        @Override
        public String getNamespace() {
            return "test";
        }

        @Override
        public String getName() {
            return "echo";
        }

        @Override
        public DataFetcher<Object> createDataFetcher(FetcherDefinition fetcherDef, Resource r) {
            return new EchoDataFetcher(r);
        }
    }

    static class StaticDataFetcher implements DataFetcher<Object> {

        private final Object data;

        StaticDataFetcher(Object data) {
            this.data = data;
        }

        @Override
        public Object get(DataFetchingEnvironment environment) {
            return data;
        }

    }

    static class StaticSlingDataFetcher implements SlingDataFetcher {

        @Override
        public String getNamespace() {
            return "test";
        }

        @Override
        public String getName() {
            return "static";
        }

        @Override
        public DataFetcher<Object> createDataFetcher(FetcherDefinition fetcherDef, Resource r) {
            Map<String, Object> data = new LinkedHashMap<>(4);
            data.put("test", true);
            return new StaticDataFetcher(data);
        }
    }

    public MockFetcherManager() {
        super(new EchoSlingDataFetcher(), new StaticSlingDataFetcher());
    }

}
