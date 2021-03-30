/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Licensed to the Apache Software Foundation (ASF) under one
 ~ or more contributor license agreements.  See the NOTICE file
 ~ distributed with this work for additional information
 ~ regarding copyright ownership.  The ASF licenses this file
 ~ to you under the Apache License, Version 2.0 (the
 ~ "License"); you may not use this file except in compliance
 ~ with the License.  You may obtain a copy of the License at
 ~
 ~   http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package org.apache.sling.remote.resourceprovider.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.remote.resourceprovider.File;
import org.apache.sling.remote.resourceprovider.RemoteResourceReference;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CacheableResourceWrapper extends ResourceWrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheableResourceWrapper.class);

    private final ResolveContext<RemoteResourceProviderContext> context;
    private final CacheableResource resource;
    private final ResourceMetadata resourceMetadata;

    CacheableResourceWrapper(@NotNull ResolveContext<RemoteResourceProviderContext> context, @NotNull CacheableResource resource) {
        super(resource);
        this.context = context;
        this.resource = resource;
        resourceMetadata = (ResourceMetadata) resource.getResourceMetadata().clone();
    }

    @Override
    @NotNull
    public ResourceResolver getResourceResolver() {
        return context.getResourceResolver();
    }

    @Override
    @NotNull
    public ResourceMetadata getResourceMetadata() {
        return resourceMetadata;
    }

    @Override
    @NotNull
    public Iterable<Resource> getChildren() {
        return context.getResourceResolver().getChildren(this);
    }

    @Override
    @NotNull
    public Iterator<Resource> listChildren() {
        return getChildren().iterator();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        RemoteResourceReference remoteResourceReference = resource.getRemoteResourceReference();
        if (type == ValueMap.class || type == Map.class) {
            return (AdapterType) getValueMap();
        }
        if (type == InputStream.class && remoteResourceReference.getType() == RemoteResourceReference.Type.FILE) {
            Map<String, Object> authenticationInfo = context.getProviderState() == null ? Collections.emptyMap() :
                    context.getProviderState().getAuthenticationInfo();
            File file = resource.getRemoteStorageProvider().getFile(remoteResourceReference, authenticationInfo);
            if (file != null) {
                try {
                    return (AdapterType) file.getInputStream();
                } catch (IOException e) {
                    LOGGER.error(String.format("Unable to convert resource %s to an InputStream.", getPath()), e);
                }
            }
        }
        return super.adaptTo(type);
    }
}
