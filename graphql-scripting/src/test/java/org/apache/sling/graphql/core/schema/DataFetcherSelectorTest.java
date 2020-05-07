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

package org.apache.sling.graphql.core.schema;

import graphql.schema.DataFetcher;
import org.apache.sling.graphql.api.graphqljava.DataFetcherProvider;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DataFetcherSelectorTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testGetDataFetcher() {
        DataFetcher<Object> gqlFetcher1 = (DataFetcher<Object>) Mockito.mock(DataFetcher.class);
        DataFetcher<Object> gqlFetcher2 = (DataFetcher<Object>) Mockito.mock(DataFetcher.class);
        DataFetcher<Object> gqlFetcher3 = (DataFetcher<Object>) Mockito.mock(DataFetcher.class);
        DataFetcherProvider fetcher1 = Mockito.mock(DataFetcherProvider.class);
        Mockito.when(fetcher1.getNamespace()).thenReturn("ns1");
        Mockito.when(fetcher1.getName()).thenReturn("name1");
        Mockito.when(fetcher1.createDataFetcher(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(gqlFetcher1);
        DataFetcherProvider fetcher2 = Mockito.mock(DataFetcherProvider.class);
        Mockito.when(fetcher2.getNamespace()).thenReturn("ns2");
        Mockito.when(fetcher2.getName()).thenReturn("name2");
        Mockito.when(fetcher2.createDataFetcher(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(gqlFetcher2);
        DataFetcherProvider fetcher3 = Mockito.mock(DataFetcherProvider.class);
        Mockito.when(fetcher3.getNamespace()).thenReturn("ns1");
        Mockito.when(fetcher3.getName()).thenReturn("name2");
        Mockito.when(fetcher3.createDataFetcher(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(gqlFetcher3);
        DataFetcherSelector fetchers = new DataFetcherSelector(fetcher1, fetcher2, fetcher3);

        assertEquals(
                gqlFetcher1,
                fetchers.getDataFetcherForType(new DataFetcherDefinition("fetch:ns1/name1"), null));
        assertEquals(
                gqlFetcher2,
                fetchers.getDataFetcherForType(new DataFetcherDefinition("fetch:ns2/name2"), null));
        assertEquals(
                gqlFetcher3,
                fetchers.getDataFetcherForType(new DataFetcherDefinition("fetch:ns1/name2"), null));
        assertNull(
                fetchers.getDataFetcherForType(new DataFetcherDefinition("fetch:ns2/name1"), null));
    }

}
