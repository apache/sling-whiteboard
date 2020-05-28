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

package org.apache.sling.graphql.samples.website.datafetchers;

import java.util.Map;

import org.apache.sling.api.resource.Resource;
import graphql.schema.DataFetchingEnvironment;

class FetcherUtil {

    private FetcherUtil() {
    }

    /** Return the "source" Resource to use, preferably the one provided
     *  by the DataFetchingEnvironment, otherwise the supplied base Resource.
     */
    static Resource getSourceResource(DataFetchingEnvironment env, Resource base) {
        Resource result = base;
        String path = null;
        final Object o = env.getSource();
        if(o instanceof Map) {
            final Map<?, ?> m = (Map<?,?>)o;
            path = String.valueOf(m.get("path"));
        }
        if(path != null) {
            final Resource r = base.getResourceResolver().getResource(base, path);
            if(r != null) {
                result = r;
            }
        }
        return result;
    }
}