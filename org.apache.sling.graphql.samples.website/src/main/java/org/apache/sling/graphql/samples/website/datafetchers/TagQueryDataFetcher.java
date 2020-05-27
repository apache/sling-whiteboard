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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.graphql.samples.website.models.ArticleRef;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

class TagQueryDataFetcher implements DataFetcher<Object> {

    public static final String NAME = "tagQuery";

    private final Resource resource;

    TagQueryDataFetcher(Resource r) {
        this.resource = r;
    }

    static String jcrQuery(String ... tags) {
        // Build a query like
        //  /jcr:root/content/articles//*[@tags = "panel" and @tags = "card"]
        final StringBuilder sb = new StringBuilder("/jcr:root/content/articles//*[");
        for(int i=0 ; i < tags.length; i++) {
            if(i > 0) {
                sb.append(" and ");
            }
            sb.append("@tags=\"").append(tags[i]).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public Object get(DataFetchingEnvironment environment) throws Exception {
        final Map<String, Object> result = new HashMap<>();
        final List<ArticleRef> articles = new ArrayList<>();
        final ValueMap vm = resource.adaptTo(ValueMap.class);
        if(vm != null) {
            final String [] tags = vm.get("tags", String[].class);
            if(tags != null) {
                result.put("articles", articles);
                result.put("query", tags);

                final Iterator<Resource> it = resource.getResourceResolver().findResources(jcrQuery(tags), "xpath");
                // TODO should stop/paginate if too many results
                while(it.hasNext()) {
                    articles.add(new ArticleRef(it.next()));
                }
            }
        }
        return result;
    }
}