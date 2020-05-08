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

package org.apache.sling.graphql.api.graphqljava;

import aQute.bnd.annotation.ConsumerType;
import graphql.schema.DataFetcher;

import java.io.IOException;

import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

@ConsumerType
public interface DataFetcherProvider {
    // TODO this should be a service property
    String getNamespace();

    // TODO this is probably not needed as clients
    // can query all (ranked) services that belong to the desired
    // namespace by calling createDataFetcher and stopping
    // as soon as one returns non-null.
    String getName();

    /* Create a DataFetcher according to the supplied options.
     * The implementation can decide to reuse the same object
     * multiple times,  new one every time or anything in
     * between, no assumptions should be made around that.
     *
     * @param r the Resource to use as the context of the DataFetcher
     * @param name the name of the DataFetcher to select
     * @param options (optional) parameters for the DataFetcher, algorithm selection for example
     * @param source (optional) which data to use from r, a property name for example. The syntax of that
     *  value is defined by the specific DataFetcher that's selected by the "name" parameter.
     *
     * @return a DataFetcher, or null if this provider does not have one
     *  for the supplied parameters
     */
    @Nullable
    DataFetcher<Object> createDataFetcher(
        @NotNull Resource r,
        @NotNull String name,
        @Nullable String options,
        @Nullable String source) throws IOException;
}
