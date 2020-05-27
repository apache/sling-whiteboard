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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.graphql.samples.website.models.ArticleRef;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

class ArticlesBySectionFetcher implements DataFetcher<Object> {

    public static final String NAME = "articlesBySection";

    private final Resource section;

    ArticlesBySectionFetcher(Resource section) {
        this.section = section;
    }

    @Override
    public Object get(DataFetchingEnvironment environment) throws Exception {
        // TODO should paginate instead
        final int maxArticles = 42;
        final List<ArticleRef> result = new ArrayList<>();
        final Iterable<Resource> it = () -> section.getResourceResolver().getChildren(section).iterator();
        StreamSupport.stream(it.spliterator(), false)
            .limit(maxArticles)
            .forEach(child -> result.add(new ArticleRef(child)));
        return result;
    }
}