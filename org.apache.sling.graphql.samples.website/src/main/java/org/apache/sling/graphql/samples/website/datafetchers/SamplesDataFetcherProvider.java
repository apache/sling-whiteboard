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

import graphql.schema.DataFetcher;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.graphql.api.graphqljava.DataFetcherProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;

import java.io.IOException;

/** Used by the GraphQL Core to select the appropriate DataFetcher,
 *  based on ##fetcher annotations in the schemas.
 */
@Component(service = DataFetcherProvider.class, property = { "namespace=samples" })
public class SamplesDataFetcherProvider implements DataFetcherProvider {

    @Override
    public @Nullable DataFetcher<Object> createDataFetcher(@NotNull Resource r, @NotNull String name,
            @Nullable String options, @Nullable String source) throws IOException {
                
        if(SeeAlsoDataFetcher.NAME.equals(name)) {
            return new SeeAlsoDataFetcher(r);
        } else if(CurrentResourceFetcher.NAME.equals(name)) {
            return new CurrentResourceFetcher(r);
        } else if(TagQueryDataFetcher.NAME.equals(name)) {
            return new TagQueryDataFetcher(r);
        } else if(ArticlesBySectionFetcher.NAME.equals(name)) {
            return new ArticlesBySectionFetcher(r);
        } else if(NavigationDataFetcher.NAME.equals(name)) {
            return new NavigationDataFetcher(r);
        } else if(ArticlesWithTextFetcher.NAME.equals(name)) {
            return new ArticlesWithTextFetcher(r);
        }
        return null;
    }
}
