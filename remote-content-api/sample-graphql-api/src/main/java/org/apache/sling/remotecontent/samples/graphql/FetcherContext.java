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

package org.apache.sling.remotecontent.samples.graphql;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.graphql.api.SlingDataFetcherEnvironment;
import org.apache.sling.graphql.api.pagination.Cursor;

class FetcherContext {
    public static final String LIMIT_ARG = "limit";
    public static final String AFTER_ARG = "after";
    public static final String PATH_ARG = "path";
    public static final int DEFAULT_LIMIT = 50;

    public final Resource currentResource;
    public final Cursor afterCursor;
    public final int limit;

    FetcherContext(SlingDataFetcherEnvironment e, boolean includePagination) {
        Resource r = e.getCurrentResource();
        final String path = e.getArgument(PATH_ARG);
        if(path != null && !path.isEmpty()) {
            r = r.getResourceResolver().getResource(path);
        }
        currentResource = r;
        limit = e.getArgument(LIMIT_ARG, DEFAULT_LIMIT);
        final String after = e.getArgument(AFTER_ARG, null);
        afterCursor = includePagination ? Cursor.fromEncodedString(after) : null;
    }
}