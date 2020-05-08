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

package org.apache.sling.graphql.api;

import java.io.IOException;

import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import aQute.bnd.annotation.ProviderType;

@ProviderType
public interface SchemaProvider {
    
    /** Get a GraphQL Schema definition for the given resource and optional selectors
     *
     *  @param r The Resource to which the schema applies
     *  @param selectors Optional set of Request Selectors that can influence the schema selection
     *  @return a GraphQL schema that can be annotated to define the data fetchers to use, see
     *      this module's documentation. Can return null if a schema cannot be provided, in which
     *      case a different provider should be used.
     */
    @Nullable
    String getSchema(@NotNull Resource r, @Nullable String [] selectors) throws IOException;
}
