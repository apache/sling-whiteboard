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

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.http.HttpServletRequest;
import java.util.Iterator;
import java.util.Map;

/**
 * Simple abstract ResourceResolver implementation that delegates all calls
 * to the delegate ResourceResolver.
 */
public abstract class AbstractDelegatingResourceResolver implements ResourceResolver {

    private final ResourceResolver delegate;

    protected AbstractDelegatingResourceResolver(ResourceResolver delegate) {
        this.delegate = delegate;
    }

    protected final ResourceResolver getDelegate() {
        return delegate;
    }

    @Override @NotNull
    public ResourceResolver clone(final Map<String, Object> authenticationInfo) throws LoginException {
        return getDelegate().clone(authenticationInfo);
    }

    @Override
    public void close() {
        getDelegate().close();
    }

    @Override
    public void commit() throws PersistenceException {
        getDelegate().commit();
    }

    @Override @NotNull
    public Resource create(@NotNull final Resource parent, @NotNull final String name, final Map<String, Object> properties) throws PersistenceException {
        return getDelegate().create(parent, name, properties);
    }

    @Override
    public void delete(@NotNull final Resource resource) throws PersistenceException {
        getDelegate().delete(resource);
    }

    @Override @NotNull
    public Iterator<Resource> findResources(@NotNull final String query, final String language) {
        return getDelegate().findResources(query, language);
    }

    @Override
    public Object getAttribute(@NotNull final String name) {
        return getDelegate().getAttribute(name);
    }

    @Override @NotNull
    public Iterator<String> getAttributeNames() {
        return getDelegate().getAttributeNames();
    }

    @Override @NotNull
    public Iterable<Resource> getChildren(@NotNull final Resource parent) {
        return getDelegate().getChildren(parent);
    }

    @Override
    public String getParentResourceType(final Resource resource) {
        return getDelegate().getParentResourceType(resource);
    }

    @Override
    public String getParentResourceType(final String resourceType) {
        return getDelegate().getParentResourceType(resourceType);
    }

    @Override @Nullable
    public Resource getResource(final Resource base, @NotNull final String path) {
        return getDelegate().getResource(base, path);
    }

    @Override @Nullable
    public Resource getResource(@NotNull final String path) {
        return getDelegate().getResource(path);
    }

    @Override @NotNull
    public String[] getSearchPath() {
        return getDelegate().getSearchPath();
    }

    @Override
    public String getUserID() {
        return getDelegate().getUserID();
    }

    @Override
    public boolean hasChanges() {
        return getDelegate().hasChanges();
    }

    @Override
    public boolean hasChildren(@NotNull final Resource resource) {
        return getDelegate().hasChildren(resource);
    }

    @Override
    public boolean isLive() {
        return getDelegate().isLive();
    }

    @Override
    public boolean isResourceType(final Resource resource, final String resourceType) {
        return getDelegate().isResourceType(resource, resourceType);
    }

    @Override @NotNull
    public Iterator<Resource> listChildren(@NotNull final Resource parent) {
        return getDelegate().listChildren(parent);
    }

    @Override @Nullable
    public String map(@NotNull final HttpServletRequest request,@NotNull  final String resourcePath) {
        return getDelegate().map(request, resourcePath);
    }

    @Override @NotNull
    public String map(@NotNull final String resourcePath) {
        return getDelegate().map(resourcePath);
    }

    @Override @NotNull
    public Iterator<Map<String, Object>> queryResources(@NotNull final String query, final String language) {
        return getDelegate().queryResources(query, language);
    }

    @Override
    public void refresh() {
        getDelegate().refresh();
    }

    @Override @NotNull
    public Resource resolve(@NotNull final String absPath) {
        return getDelegate().resolve(absPath);
    }

    @Override
    @Deprecated
    @NotNull
    public Resource resolve(@NotNull final HttpServletRequest request) {
        return getDelegate().resolve(request);
    }

    @Override @NotNull
    public Resource resolve(@NotNull final HttpServletRequest request, @NotNull final String absPath) {
        return getDelegate().resolve(request, absPath);
    }

    @Override
    public void revert() {
        getDelegate().revert();
    }

    @Override @Nullable
    public <AdapterType> AdapterType adaptTo(@NotNull final Class<AdapterType> type) {
        return getDelegate().adaptTo(type);
    }

    @Override
    public Resource getParent(@NotNull final Resource child) {
        return getDelegate().getParent(child);
    }

    @Override
    public Resource move(final String srcAbsPath, final String destAbsPath) throws PersistenceException {
        return getDelegate().move(srcAbsPath, destAbsPath);
    }

    @Override
    public Resource copy(final String srcAbsPath, final String destAbsPath) throws PersistenceException {
        return getDelegate().copy(srcAbsPath, destAbsPath);
    }
}
