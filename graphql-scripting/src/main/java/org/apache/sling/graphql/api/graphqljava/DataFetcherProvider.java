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
import org.apache.sling.api.resource.Resource;

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
     */
    DataFetcher<Object> createDataFetcher(Resource r, String name, String options, String source);
}
