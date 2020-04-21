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
            return "sling";
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

    public MockFetcherManager() {
        super(new EchoSlingDataFetcher());
    }

}
