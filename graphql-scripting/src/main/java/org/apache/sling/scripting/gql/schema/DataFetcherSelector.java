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
import org.apache.sling.api.resource.Resource;
import org.apache.sling.graphql.api.DataFetcherProvider;
import org.apache.sling.graphql.api.DataFetcherDefinition;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component(service = DataFetcherSelector.class, immediate = true, property = {
        Constants.SERVICE_DESCRIPTION + "=Apache Sling Scripting GraphQL FetcherManager",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation" })
public class DataFetcherSelector {

    @Reference(
        name = "slingFetchers",
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC)
    private final List<DataFetcherProvider> factories = new CopyOnWriteArrayList<>();

    public DataFetcherSelector() {
        // public default constructor required ...
    }

    DataFetcherSelector(DataFetcherProvider... testFetchers) {
        factories.addAll(Arrays.asList(testFetchers));
    }

    public DataFetcher<Object> getDataFetcherForType(DataFetcherDefinition def, Resource r) {
        String ns = def.getFetcherNamespace();
        String name = def.getFetcherName();

        for (DataFetcherProvider factory : factories) {
            if (factory.getNamespace().equals(ns) && factory.getName().equals(name)) {
                return factory.createDataFetcher(def, r);
            }
        }
        return null;
    }

}
