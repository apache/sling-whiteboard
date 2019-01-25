/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.jcr.wrappers;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.InvalidNodeTypeDefinitionException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeExistsException;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinitionTemplate;

// TODO extend BaseWrapper?
public class NodeTypeManagerWrapper implements NodeTypeManager {
    private final NodeTypeManager nodeTypeManager;

    public NodeTypeManagerWrapper(NodeTypeManager nodeTypeManager) {
        this.nodeTypeManager = nodeTypeManager;
    }

    @Override
    public NodeType getNodeType(String nodeTypeName) throws NoSuchNodeTypeException, RepositoryException {
        return nodeTypeManager.getNodeType(nodeTypeName);
    }

    @Override
    public boolean hasNodeType(String name) throws RepositoryException {
        return nodeTypeManager.hasNodeType(name);
    }

    @Override
    public NodeTypeIterator getAllNodeTypes() throws RepositoryException {
        return nodeTypeManager.getAllNodeTypes();
    }

    @Override
    public NodeTypeIterator getPrimaryNodeTypes() throws RepositoryException {
        return nodeTypeManager.getPrimaryNodeTypes();
    }

    @Override
    public NodeTypeIterator getMixinNodeTypes() throws RepositoryException {
        return nodeTypeManager.getMixinNodeTypes();
    }

    @Override
    public NodeTypeTemplate createNodeTypeTemplate() throws UnsupportedRepositoryOperationException, RepositoryException {
        return nodeTypeManager.createNodeTypeTemplate();
    }

    @Override
    public NodeTypeTemplate createNodeTypeTemplate(NodeTypeDefinition ntd) throws UnsupportedRepositoryOperationException, RepositoryException {
        return nodeTypeManager.createNodeTypeTemplate(ntd);
    }

    @Override
    public NodeDefinitionTemplate createNodeDefinitionTemplate() throws UnsupportedRepositoryOperationException, RepositoryException {
        return nodeTypeManager.createNodeDefinitionTemplate();
    }

    @Override
    public PropertyDefinitionTemplate createPropertyDefinitionTemplate() throws UnsupportedRepositoryOperationException, RepositoryException {
        return nodeTypeManager.createPropertyDefinitionTemplate();
    }

    @Override
    public NodeType registerNodeType(NodeTypeDefinition ntd, boolean allowUpdate) throws InvalidNodeTypeDefinitionException, NodeTypeExistsException, UnsupportedRepositoryOperationException, RepositoryException {
        return nodeTypeManager.registerNodeType(ntd, allowUpdate);
    }

    @Override
    public NodeTypeIterator registerNodeTypes(NodeTypeDefinition[] ntds, boolean allowUpdate) throws InvalidNodeTypeDefinitionException, NodeTypeExistsException, UnsupportedRepositoryOperationException, RepositoryException {
        return nodeTypeManager.registerNodeTypes(ntds, allowUpdate);
    }

    @Override
    public void unregisterNodeType(String name) throws UnsupportedRepositoryOperationException, NoSuchNodeTypeException, RepositoryException {
        nodeTypeManager.unregisterNodeType(name);
    }

    @Override
    public void unregisterNodeTypes(String[] names) throws UnsupportedRepositoryOperationException, NoSuchNodeTypeException, RepositoryException {
        nodeTypeManager.unregisterNodeTypes(names);
    }
}
