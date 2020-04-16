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
package org.apache.sling.common.resource.graft;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.common.resource.graft.impl.AbstractDelegatingResource;
import org.apache.sling.common.resource.graft.impl.AbstractDelegatingResourceResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This is not a general purpose implementation. It is sufficient for covering the
 * execution path taken when the default GET servlet renders a dialog (as JSON).
 */
public class GraftedResource extends AbstractDelegatingResource {

    private final GraftingResourceResolver resolver;

    private final Map<String, Object> properties = new LinkedHashMap<>();

    public static GraftedResource graftOnto(Resource rootstock) {
        // Resource implementations inheriting from AbstractResource
        // delegate all child related calls to their ResourceResolver
        return new GraftedResource(new GraftingResourceResolver(rootstock.getResourceResolver()), rootstock);
    }

    private GraftedResource(final GraftingResourceResolver resolver, final Resource rootstock) {
        super(rootstock);
        this.resolver = resolver;
        ValueMap props = rootstock.getValueMap();
        if (!props.isEmpty()) {
            this.properties.putAll(props);
        }
    }

    @Nullable
    public GraftedResource parent() {
        return resolver.getGraftedParent(this);
    }

    public GraftedResource appendChild(@NotNull final String name) {
        return addChildBefore(null, name);
    }

    public GraftedResource addChildBefore(@Nullable final String beforeName, @NotNull final String name) {
        SyntheticResource syntheticResource = new SyntheticResource(getDelegate().getResourceResolver(), getPath() + "/" + name, null);
        return insertBefore(beforeName, syntheticResource);
    }

    public GraftedResource setProperty(String name, Object value) {
        properties.put(name, value);
        return this;
    }

    public GraftedResource removeProperty(String name) {
        properties.remove(name);
        return this;
    }

    public GraftedResource insertBefore(@Nullable final String beforeName, @NotNull final Resource child) {
        return resolver.insertBefore(this, beforeName, child);
    }

    public void removeChild(@NotNull final String name) {
        resolver.removeChild(this, name);
    }

    @NotNull
    @Override
    public ResourceResolver getResourceResolver() {
        return resolver;
    }

    @Override @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType adaptTo(@NotNull final Class<AdapterType> type) {
        if (type == Map.class) {
            return (AdapterType) properties;
        }
        return super.adaptTo(type);
    }

    private static class GraftingResourceResolver extends AbstractDelegatingResourceResolver {

        private final Map<String, List<Resource>> childrenByParent = new LinkedHashMap<>();

        private GraftingResourceResolver(final ResourceResolver delegate) {
            super(delegate);
        }

        @Override @NotNull
        public Iterator<Resource> listChildren(@NotNull final Resource parent) {
            List<Resource> children = childrenByParent.get(parent.getPath());
            if (children != null) {
                return children.iterator();
            } else {
                return getDelegate().listChildren(parent);
            }
        }

        private GraftedResource insertBefore(@NotNull final Resource parent, @Nullable final String beforeName, @NotNull final Resource child) {
            List<Resource> children = getOrInitializeChildren(parent);
            GraftedResource decoratedChild = decorate(child);
            for (int i = 0; i < children.size(); i++) {
                if (children.get(i).getName().equals(beforeName)) {
                    children.add(i, decoratedChild);
                    return decoratedChild;
                }
            }

            // add to the end if "beforeName" is not found
            children.add(decoratedChild);
            return decoratedChild;
        }

        public void removeChild(@NotNull final Resource parent, @NotNull final String name) {
            String parentPath = parent.getPath();
            if (!isInitialized(parentPath)) {
                Resource childToRemove = parent.getChild(name);
                if (childToRemove == null) {
                    // parent does not have this child, nothing to do
                    return;
                }
            }
            List<Resource> children = getOrInitializeChildren(parent);
            Iterator<Resource> childIterator = children.iterator();
            while (childIterator.hasNext()) {
                if (childIterator.next().getName().equals(name)) {
                    childIterator.remove();
                    return;
                }
            }
        }

        private boolean isInitialized(@NotNull final String parentPath) {
            return childrenByParent.containsKey(parentPath);
        }

        @NotNull
        private List<Resource> getOrInitializeChildren(@NotNull final Resource parent) {
            String parentPath = parent.getPath();
            if (!isInitialized(parentPath)) {
                ArrayList<Resource> children = new ArrayList<>();
                for (final Resource child : getDelegate().getChildren(parent)) {
                    children.add(decorate(child));
                }
                childrenByParent.put(parentPath, children);
            }
            return childrenByParent.get(parentPath);
        }

        private GraftedResource decorate(Resource resource) {
            // Resource implementations inheriting from AbstractResource
            // delegate all child related calls to their ResourceResolver
            return new GraftedResource(this, resource);
        }

        @Nullable
        private GraftedResource getGraftedParent(final GraftedResource child) {
            String parentPath = ResourceUtil.getParent(child.getPath());
            String grandParentPath = ResourceUtil.getParent(parentPath);
            if (isInitialized(grandParentPath)) {
                String parentName = ResourceUtil.getName(parentPath);
                List<Resource> resources = childrenByParent.get(grandParentPath);
                for (final Resource resource : resources) {
                    if (resource.getName().equals(parentName)) {
                        return (GraftedResource) resource;
                    }
                }
            }
            return null;
        }
    }
}
