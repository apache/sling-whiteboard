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

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.documentaggregator.api.DocumentAggregator;
import org.apache.sling.documentaggregator.api.DocumentTree;
import org.apache.sling.graphql.api.SlingDataFetcher;
import org.apache.sling.graphql.api.SlingDataFetcherEnvironment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = SlingDataFetcher.class, property = {"name=samples/document"})
public class DocumentDataFetcher implements SlingDataFetcher<Object> {

    @Reference(target="(" + DocumentTree.TARGET_TYPE + "=map)")
    private DocumentTree mappingTarget;

    @Reference
    private DocumentAggregator documentAggregator;

    @Override
    public @Nullable Object get(@NotNull SlingDataFetcherEnvironment e) throws Exception {
        final String path = e.getArgument("path");

        final Map<String, Object> data = new HashMap<>();
        data.put("path", path);
        data.put("selectors", e.getArgument("selectors"));
        final DocumentAggregator.Options opt = new DocumentAggregator.Options(e.getArgument("debug", false), new UrlBuilderStub());

        // Get the target Resource
        final Resource target = e.getCurrentResource().getResourceResolver().getResource(path);

        // Use the aggregator to build the body
        final DocumentTree.DocumentNode body = mappingTarget.newTargetNode();
        documentAggregator.aggregate(target, body, opt);
        body.close();
        data.put("body", body.adaptTo(Map.class));
        return data;
    }   
}