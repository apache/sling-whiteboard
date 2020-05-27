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

package org.apache.sling.graphql.samples.website.tagresource;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.service.component.annotations.Component;

import java.util.Iterator;

/** Provide in-memory resources for Tag queries like /content/tags/one+two+three 
 *  which will run a query for articles that have all three tags.
*/
@Component(service = ResourceProvider.class, property = { ResourceProvider.PROPERTY_NAME + "=TagsResourceProvider",
        ResourceProvider.PROPERTY_ROOT + "=" + TagsResourceProvider.ROOT_PATH })
public class TagsResourceProvider extends ResourceProvider<TagsResourceProvider.UnusedContext> {

    public static final String ROOT_PATH = "/content/tags";

    /** We don't need a context for now */
    public static class UnusedContext {
    }

    @Override
    public Resource getResource(ResolveContext<UnusedContext> ctx, String path, ResourceContext resourceContext, Resource parent) {
        return TagsResource.create(ctx.getResourceResolver(), path);
    }

    @Override
    public Iterator<Resource> listChildren(ResolveContext<UnusedContext> ctx, Resource parent) {
        // Tags resources are a flat hierarchy with no children
        return null;
    };
}
