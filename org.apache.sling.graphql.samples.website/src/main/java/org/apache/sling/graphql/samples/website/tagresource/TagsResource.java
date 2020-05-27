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

import org.apache.sling.adapter.annotations.Adaptable;
import org.apache.sling.adapter.annotations.Adapter;
import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** In-memory representation of a Tag query */
@Adaptable(
    adaptableClass=Resource.class, 
    adapters={ @Adapter({ValueMap.class})}
)
public class TagsResource extends AbstractResource {
    private final String path;
    private final Map<String, Object> properties = new HashMap<>();
    private final ResourceMetadata metadata;
    private final ResourceResolver resolver;

    public static final String RESOURCE_TYPE = "samples/tag";
    public static final Pattern VALID_TAG = Pattern.compile("(\\w)+");

    private TagsResource(ResourceResolver resolver, String path, String[] tags) {
        this.path = path;
        this.metadata = new ResourceMetadata();
        this.metadata.setResolutionPath(path);
        this.resolver = resolver;
        properties.put("tags", tags);
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    @Override
    public String getResourceSuperType() {
        return null;
    }

    @Override
    public ResourceMetadata getResourceMetadata() {
        return metadata;
    }

    @Override
    public ResourceResolver getResourceResolver() {
        return resolver;
    }

    private static String [] getTags(String path) {
        final String [] tags = path.split("\\+");
        if(tags != null && Arrays.stream(tags).allMatch(tag -> VALID_TAG.matcher(tag).matches())) {
            return tags;
        }
        return new String[]{};
    }

    private static String basename(String path) {
        if(path.length() < TagsResourceProvider.ROOT_PATH.length()) {
            return path;
        }
        path = path.substring(TagsResourceProvider.ROOT_PATH.length());
        if(path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }

    /** If the supplied path is valid, return the corresponding
     *  TagsResource, otherwise null.
     */
    static TagsResource create(ResourceResolver resolver, String path) {
        final String basename = basename(path);
        final String [] tags = getTags(basename);
        return tags.length > 0 ? new TagsResource(resolver, path, tags) : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if(type == ValueMap.class) {
            return (AdapterType)new ValueMapDecorator(properties);
        }
        return super.adaptTo(type);
    }
}