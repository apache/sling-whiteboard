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
package org.apache.sling.distribution.chunked;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeepTree {
    private static Logger log = LoggerFactory.getLogger(DeepTree.class);
    private List<String> paths = new ArrayList<>();
    private Mode mode;
    
    public static List<String> getPaths(Resource baseResource, Mode mode) {
        String path = Objects.requireNonNull(baseResource).getPath();
        log.info("Getting deep tree for {} using mode {}", path, mode);
        DeepTree walker = new DeepTree(mode);
        walker.walkTreeRecursively(baseResource);
        log.info("Getting deep tree for {} finished with {} results", path, walker.paths.size());
        return walker.paths;
    }
    
    private DeepTree(Mode mode) {
        this.mode = mode;
    }
    
    public void walkTreeRecursively(Resource baseResource) {
        try {
            Node baseNode = baseResource.adaptTo(Node.class);
            boolean isHierarchyNode = baseNode.isNodeType("nt:hierarchyNode");
            if (mode == Mode.AllNodes || isHierarchyNode) {
                paths.add(baseResource.getPath());
                Iterable<Resource> childrenIter = baseResource.getChildren();
                List<Resource> children = getChildren(childrenIter.iterator());
                children.forEach(this::walkTreeRecursively);
            }
        } catch (RepositoryException e) {
            log.warn("Exception when walking node tree at {}", baseResource.getPath());
        }
    }
    
    private List<Resource> getChildren(Iterator<Resource> childrenIt) {
        if (!childrenIt.hasNext()) {
            return Collections.emptyList();
        }
        List<Resource> children = new ArrayList<>();
        while (childrenIt.hasNext()) {
            children.add(childrenIt.next());
        }
        return children;
    };
}
