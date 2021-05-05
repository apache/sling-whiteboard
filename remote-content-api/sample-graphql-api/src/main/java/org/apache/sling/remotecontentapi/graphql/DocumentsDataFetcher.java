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

package org.apache.sling.remotecontentapi.graphql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.documentmapper.api.DocumentMapper;
import org.apache.sling.documentmapper.api.MappingTarget;
import org.apache.sling.documentmapper.api.DocumentMapper.UrlBuilder;
import org.apache.sling.graphql.api.SlingDataFetcher;
import org.apache.sling.graphql.api.SlingDataFetcherEnvironment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = SlingDataFetcher.class, property = {"name=samples/documents"})
public class DocumentsDataFetcher implements SlingDataFetcher<Object> {

    @Reference(target="(" + MappingTarget.TARGET_TYPE + "=map)")
    private MappingTarget mappingTarget;

    // TODO should be "summary"
    @Reference(target="(" + DocumentMapper.ROLE + "=navigation)")
    private DocumentMapper summaryMapper;

    @Reference(target="(" + DocumentMapper.ROLE + "=content)")
    private DocumentMapper bodyMapper;

    private static final UrlBuilder URL_BUILDER = new UrlBuilder() {
        @Override
        public String pathToUrl(String path) {
            return getClass().getName();
        }
    };

    private void addDocumentData(final Map<String, Object> data, String key, Resource r, DocumentMapper mapper) {
        final MappingTarget.TargetNode target = mappingTarget.newTargetNode();
        mapper.map(r, target, URL_BUILDER);
        target.close();
        data.put(key, target.adaptTo(Map.class));

    }

    private Map<String, Object> toDocument(Resource r) {
        final Map<String, Object> data = new HashMap<>();
        data.put("path", r.getPath());

        // TODO how to find out whether those fields are actually needed
        // or how to evaluate them lazily
        addDocumentData(data, "body", r, bodyMapper);
        addDocumentData(data, "summary", r, summaryMapper);

        return data;
    }
    
    @Override
    public @Nullable Object get(@NotNull SlingDataFetcherEnvironment e) throws Exception {
        // Use a suffix as we might not keep these built-in language in the long term
        final String langSuffix = "2020";

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
            result.add(toDocument(it.next()));
        }
        return result;
    }
    
}
