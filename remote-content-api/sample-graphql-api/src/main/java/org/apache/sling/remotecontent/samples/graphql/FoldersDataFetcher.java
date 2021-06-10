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
import org.apache.sling.remotecontent.contentmodel.ContentGenerator;
import org.apache.sling.remotecontent.contentmodel.Folder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = SlingDataFetcher.class, property = {"name=samples/folders"})
public class FoldersDataFetcher implements SlingDataFetcher<Connection<Folder>> {

    @Reference
    private ContentGenerator contentGenerator;

    @Override
    public @Nullable Connection<Folder> get(@NotNull SlingDataFetcherEnvironment e) throws Exception {
        final FetcherContext ctx = new FetcherContext(e, true);

        final String xpathQuery = String.format(
            "/jcr:root%s//element(*, nt:folder) order by jcr:path ascending option(traversal fail)", 
            ctx.currentResource.getPath());

        final Iterator<Resource> resultIterator = ctx.currentResource.getResourceResolver().findResources(xpathQuery, "xpath");
        final Iterator<Folder> it = new ConvertingIterator<>(resultIterator, r -> new Folder(r, () -> contentGenerator));
        return new GenericConnection.Builder<>(it, Folder::getPath)
            .withStartAfter(ctx.afterCursor)
            .withLimit(ctx.limit)
            .build();
    }
}