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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.documentaggregator.api.DocumentAggregator;
import org.apache.sling.documentaggregator.api.DocumentTree;
import org.apache.sling.graphql.api.SlingDataFetcher;
import org.apache.sling.graphql.api.SlingDataFetcherEnvironment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = SlingDataFetcher.class, property = {"name=samples/documents"})
public class DocumentsDataFetcher implements SlingDataFetcher<Object> {

    @Reference(target="(" + DocumentTree.TARGET_TYPE + "=map)")
    private DocumentTree mappingTarget;

    @Reference
    private DocumentAggregator documentAggregator;

    private void addDocumentData(final Map<String, Object> data, String key, Resource r, DocumentAggregator aggregator, DocumentAggregator.Options opt) {
        final DocumentTree.DocumentNode target = mappingTarget.newTargetNode();
        aggregator.aggregate(r, target, opt);
        target.close();
        data.put(key, target.adaptTo(Map.class));

    }

    private Map<String, Object> toDocument(Resource r, DocumentAggregator.Options opt) {
        final Map<String, Object> data = new HashMap<>();
        data.put("path", r.getPath());

        // TODO for now those are the same...
        addDocumentData(data, "body", r, documentAggregator, opt);
        addDocumentData(data, "summary", r, documentAggregator, opt);

        return data;
    }
    
    @Override
    public @Nullable Object get(@NotNull SlingDataFetcherEnvironment e) throws Exception {
        // Use a suffix as we might not keep these built-in language in the long term
        final String langSuffix = "2020";

        final DocumentAggregator.Options opt = new DocumentAggregator.Options(e.getArgument("debug", false), new UrlBuilderStub());

        String lang = e.getArgument("lang", "xpath" + langSuffix);
        if(!lang.endsWith(langSuffix)) {
            throw new RuntimeException("Query langage must end with suffix " + langSuffix);
        }
        lang = lang.replaceAll(langSuffix + "$", "");
        final String query = e.getArgument("query");
        final List<Map<String, Object>> result = new ArrayList<>();

        final ResourceResolver resolver = e.getCurrentResource().getResourceResolver();
        final Iterator<Resource> it = resolver.findResources(query, lang);
        while(it.hasNext()) {
            result.add(toDocument(it.next(), opt));
        }
        return result;
    }
    
}
