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

import java.util.Iterator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.graphql.api.SlingDataFetcher;
import org.apache.sling.graphql.api.SlingDataFetcherEnvironment;
import org.apache.sling.graphql.api.pagination.Connection;
import org.apache.sling.graphql.helpers.GenericConnection;
import org.apache.sling.remotecontent.contentmodel.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;

@Component(service = SlingDataFetcher.class, property = {"name=samples/documents"})
public class DocumentsDataFetcher implements SlingDataFetcher<Connection<Document>> {

    @Override
    public @Nullable Connection<Document> get(@NotNull SlingDataFetcherEnvironment e) throws Exception {
        final FetcherContext ctx = new FetcherContext(e, false);

        // Use a suffix as we might not keep these built-in language in the long term
        final String langSuffix = "2020";

        String lang = e.getArgument("lang", "xpath" + langSuffix);
        if(!lang.endsWith(langSuffix)) {
            throw new RuntimeException("Query langage must end with suffix " + langSuffix);
        }
        lang = lang.replaceAll(langSuffix + "$", "");
        final String query = e.getArgument("query");

        final Iterator<Resource> resultIterator = ctx.currentResource.getResourceResolver().findResources(query, lang);
        final Iterator<Document> it = new ConvertingIterator<>(resultIterator, Document::new);
        return new GenericConnection.Builder<>(it, Document::getPath)
            .withStartAfter(ctx.afterCursor)
            .withLimit(ctx.limit)
            .build();
    }    
}