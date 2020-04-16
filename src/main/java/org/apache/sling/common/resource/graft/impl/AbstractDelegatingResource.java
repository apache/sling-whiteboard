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
package org.apache.sling.common.resource.graft.impl;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

/**
 * Custom AbstractDelegatingResource implementation to avoid the automatic
 * unwrapping that is applied to Sling's ResourceWrapper deep down in
 * Sling's request processing.
 * <br>
 * This class simply delegates all calls to the delegate resource.
 */
public abstract class AbstractDelegatingResource implements Resource {

    private Resource delegate;

    protected AbstractDelegatingResource(Resource delegate) {
        this.delegate = delegate;
    }

    protected final Resource getDelegate() {
        return delegate;
    }

    @Override @Nullable
    public Resource getChild(@NotNull final String relPath) {
        return getDelegate().getChild(relPath);
    }

    @Override @NotNull
    public Iterable<Resource> getChildren() {
        return getDelegate().getChildren();
    }

    @Override @NotNull
    public String getName() {
        return getDelegate().getName();
    }

    @Override @Nullable
    public Resource getParent() {
        return getDelegate().getParent();
    }

    @Override @NotNull
    public String getPath() {
        return getDelegate().getPath();
    }

    @Override @NotNull
    public ResourceMetadata getResourceMetadata() {
        return getDelegate().getResourceMetadata();
    }

    @Override @NotNull
    public ResourceResolver getResourceResolver() {
        return getDelegate().getResourceResolver();
    }

    @Override @Nullable
    public String getResourceSuperType() {
        return getDelegate().getResourceSuperType();
    }

    @Override @NotNull
    public String getResourceType() {
        return getDelegate().getResourceType();
    }

    @Override @NotNull
    public ValueMap getValueMap() {
        return getDelegate().getValueMap();
    }

    @Override
    public boolean hasChildren() {
        return getDelegate().hasChildren();
    }

    @Override
    public boolean isResourceType(final String resourceType) {
        return getDelegate().isResourceType(resourceType);
    }

    @Override @NotNull
    public Iterator<Resource> listChildren() {
        return getDelegate().listChildren();
    }

    @Override @Nullable
    public <AdapterType> AdapterType adaptTo(@NotNull final Class<AdapterType> type) {
        return getDelegate().adaptTo(type);
    }
}
