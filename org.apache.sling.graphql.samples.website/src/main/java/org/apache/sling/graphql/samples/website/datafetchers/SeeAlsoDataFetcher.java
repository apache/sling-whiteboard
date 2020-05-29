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

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.graphql.samples.website.models.SlingWrappers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

/** Retrieve additional information for our articles 'seeAlso' field */
class SeeAlsoDataFetcher implements DataFetcher<Object> {

    public static final String NAME = "seeAlso";

    private final Resource resource;

    SeeAlsoDataFetcher(Resource r) {
        this.resource = r;
    }

    /** For "see also", our articles have just the article name but no path or section.
     *  This maps those names (which are Sling Resource names) to their Resource, so
     *  we can use the full path + title to render links.
     */
    private static Map<String, Object> toArticleRef(ResourceResolver resolver, String nodeName) {
        final String jcrQuery = "/jcr:root/content/articles//" + nodeName;
        final Iterator<Resource> it = resolver.findResources(jcrQuery, "xpath");

        // We want exactly one result
        if(!it.hasNext()) {
            throw new RuntimeException("No Resource found:" + jcrQuery);
        }
        final Map<String, Object> result = SlingWrappers.resourceWrapper(it.next());
        if(it.hasNext()) {
            throw new RuntimeException("More than one Resource found:" + jcrQuery);
        }

        return result;
    }

    @Override
    public Object get(DataFetchingEnvironment env) throws Exception {

        // Our "see also" field only contains node names - this demonstrates
        // using a query to get their full paths
        // 
        final ValueMap vm = FetcherUtil.getSourceResource(env, resource).adaptTo(ValueMap.class);
        if(vm != null) {
            return Arrays
                .stream(vm.get(NAME, String[].class))
                .map(it -> toArticleRef(resource.getResourceResolver(), it))
                .toArray();
        }
        return null;
    }
}