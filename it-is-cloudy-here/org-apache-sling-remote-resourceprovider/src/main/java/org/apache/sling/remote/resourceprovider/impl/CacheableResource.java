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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.remote.resourceprovider.RemoteResourceReference;
import org.apache.sling.remote.resourceprovider.RemoteStorageProvider;
import org.jetbrains.annotations.NotNull;

public final class CacheableResource extends AbstractResource {

    public static final String NT_UNSTRUCTURED = "nt:unstructured";

    private final String path;
    private final RemoteStorageProvider remoteStorageProvider;
    private final RemoteResourceReference remoteResourceReference;
    private final String resourceType;
    private final String resourceSuperType;
    private final ValueMap valueMap;
    private final ResourceMetadata resourceMetadata;
    private LinkedHashSet<CacheableResource> children;

    public CacheableResource(RemoteStorageProvider remoteStorageProvider, RemoteResourceReference remoteResourceReference, String path,
                             Map<String, Object> properties) {
        this.remoteStorageProvider = remoteStorageProvider;
        this.remoteResourceReference = remoteResourceReference;
        this.path = path;
        resourceType = Optional.of((String) properties.get(ResourceResolver.PROPERTY_RESOURCE_TYPE)).orElse(NT_UNSTRUCTURED);
        resourceSuperType = (String) properties.get(SlingConstants.NAMESPACE_PREFIX + ":" + SlingConstants.PROPERTY_RESOURCE_SUPER_TYPE);
        valueMap = new ValueMapDecorator(Collections.unmodifiableMap(properties));
        resourceMetadata = new ResourceMetadata();
        resourceMetadata.setCreationTime(remoteResourceReference.getCreated());
        resourceMetadata.setModificationTime(remoteResourceReference.getLastModified());
        if (remoteResourceReference.getType() == RemoteResourceReference.Type.FILE) {
            resourceMetadata.setContentLength(remoteResourceReference.getSize());
        }
    }

    @Override
    @NotNull
    public String getPath() {
        return path;
    }

    @Override
    @NotNull
    public String getResourceType() {
        return resourceType;
    }

    @Override
    @NotNull
    public String getResourceSuperType() {
        return resourceSuperType;
    }

    @Override
    @NotNull
    public ResourceMetadata getResourceMetadata() {
        return resourceMetadata;
    }

    @Override
    @NotNull
    public ValueMap getValueMap() {
        return valueMap;
    }

    @Override
    public @NotNull ResourceResolver getResourceResolver() {
        // API violation; however the CacheableResource will always be wrapped and a resource resolver will be provided by the wrapper
        return null;
    }

    RemoteStorageProvider getRemoteStorageProvider() {
        return remoteStorageProvider;
    }

    RemoteResourceReference getRemoteResourceReference() {
        return remoteResourceReference;
    }

    LinkedHashSet<CacheableResource> getChildrenSet() {
        return children;
    }

    void setChildren(LinkedHashSet<CacheableResource> children) {
        this.children = children;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CacheableResource)) {
            return false;
        }
        CacheableResource other = (CacheableResource) obj;
        return
                Objects.equals(this.path, other.path) &&
                Objects.equals(this.resourceType, other.resourceType) &&
                Objects.equals(this.resourceSuperType, other.resourceSuperType) &&
                Objects.equals(this.remoteStorageProvider, other.remoteStorageProvider) &&
                Objects.equals(this.resourceMetadata, other.resourceMetadata) &&
                Objects.equals(this.children, other.children) &&
                Objects.equals(this.valueMap, other.valueMap);
    }
}
