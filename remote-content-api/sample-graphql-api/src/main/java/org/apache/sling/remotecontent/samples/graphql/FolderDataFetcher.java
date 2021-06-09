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
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.graphql.api.SlingDataFetcher;
import org.apache.sling.graphql.api.SlingDataFetcherEnvironment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;

@Component(service = SlingDataFetcher.class, property = {"name=samples/folder"})
public class FolderDataFetcher implements SlingDataFetcher<Object> {

    // TODO move to document converter + make body optional
    static Map<String, Object> toDocument(Resource r) {
        final Map<String, Object> data = new HashMap<>();
        data.put("path", r.getPath());
        data.put("header", toDocumentHeader(r));
        data.put("properties", r.adaptTo(ValueMap.class));
        return data;
    }

    // TODO move to document converter + make body optional
    static Map<String, Object> toDocumentHeader(Resource r) {
        final Map<String, Object> header = new HashMap<>();
        if(r.getParent() != null) {
            header.put("parent", r.getParent().getPath());
        }
        header.put("resourceType", r.getResourceType());
        header.put("resourceSuperType", r.getResourceSuperType());
        return header;
    }

    static Resource getTargetResource(SlingDataFetcherEnvironment e) {
        Resource result = e.getCurrentResource();
        String path = e.getArgument("path");
        if(path != null && !path.isEmpty()) {
            result = result.getResourceResolver().getResource(path);
        }
        return result;
    }

    @Override
    public @Nullable Object get(@NotNull SlingDataFetcherEnvironment e) throws Exception {
        return toDocument(getTargetResource(e));
    }   
}