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
package org.apache.sling.remote.resourceprovider.impl.paths;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.sling.api.resource.ResourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ShallowReferenceTree {

    private final Map<ShallowReference, ShallowReference> nodeParent = new ConcurrentHashMap<>();
    private final Map<String, ShallowReference> references = new ConcurrentSkipListMap<>();
    private final RemoveListener listener;

    public ShallowReferenceTree() {
        listener = reference -> {};
    }

    public ShallowReferenceTree(RemoveListener listener) {
        this.listener = listener;
    }

    @Nullable
    public ShallowReference getReference(@NotNull String path) {
        return references.get(path);
    }

    @NotNull
    public Set<ShallowReference> getReferences() {
        return Set.copyOf(references.values());
    }

    @Nullable
    public ShallowReference getParent(@NotNull ShallowReference node) {
        return nodeParent.get(node);
    }

    @NotNull
    public Set<ShallowReference> getChildren(@NotNull ShallowReference node) {
        Set<ShallowReference> children = new LinkedHashSet<>();
        for (ShallowReference n : references.values()) {
            ShallowReference parent = nodeParent.get(n);
            if (parent != null && parent.equals(node)) {
                children.add(n);
            }
        }
        return children;
    }

    public boolean add(@NotNull ShallowReference reference) {
        if (references.containsValue(reference)) {
            return false;
        }
        String parentPath = ResourceUtil.getParent(reference.getPath());
        ShallowReference current = reference;
        while (parentPath != null) {
            String pPath = parentPath;
            ShallowReference parent = references.computeIfAbsent(parentPath, s -> new ShallowReference(pPath));
            references.put(current.getPath(), current);
            nodeParent.put(current, parent);
            current = parent;
            parentPath = ResourceUtil.getParent(parentPath);
        }
        return true;
    }

    public boolean remove(@NotNull String path) {
        ShallowReference node = references.get(path);
        if (!references.containsValue(node)) {
            return false;
        }
        for (ShallowReference child : getChildren(node)) {
            remove(child.getPath());
        }
        references.remove(node.getPath());
        nodeParent.remove(node);
        listener.removed(node);
        return true;
    }


}
