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

package org.apache.sling.graphql.samples.website.models;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

// TODO we should be able to avoid such model classes
// using a Sling-aware default data fetcher
public class ArticleRef {
    private final String title;
    private final String path;
    private final String [] tags;

    public ArticleRef(Resource r) {
        final ValueMap vm = r.adaptTo(ValueMap.class);
        title = vm == null ? null : vm.get("title", String.class);
        tags = vm == null ? null : vm.get("tags", String[].class);
        path = r.getPath();
    }

    public String getTitle() {
        return title;
    }

    public String getPath() {
        return path;
    }

    public String [] getTags() {
        return tags;
    }
}