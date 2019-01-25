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

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;
import javax.jcr.AccessDeniedException;
import javax.jcr.Binary;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidLifecycleTransitionException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.MergeException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.ActivityViolationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

public class NodeWrapper extends ItemWrapper<Node> implements Node {
    public NodeWrapper(SessionWrapper sessionWrapper, Node node) {
        super(sessionWrapper, node);
    }

    @Override
    public Node addNode(String relPath) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        return sessionWrapper.addNode(getPath(), concat(getPath(), relPath), relPath);
    }

    @Override
    public Node addNode(String relPath, String primaryNodeTypeName) throws ItemExistsException, PathNotFoundException, NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException, RepositoryException {
        return sessionWrapper.addNode(getPath(), concat(getPath(), relPath), relPath, primaryNodeTypeName);
    }

    @Override
    public void orderBefore(String srcChildRelPath, String destChildRelPath) throws UnsupportedRepositoryOperationException, VersionException, ConstraintViolationException, ItemNotFoundException, LockException, RepositoryException {
        this.delegate.orderBefore(srcChildRelPath, destChildRelPath);
    }

    @Override
    public Property setProperty(String name, Value value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return sessionWrapper.getObjectWrapper().wrap(sessionWrapper, delegate.setProperty(name, value));
    }

    @Override
    public Property setProperty(String name, Value value, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return sessionWrapper.getObjectWrapper().wrap(sessionWrapper, delegate.setProperty(name, value, type));
    }

    @Override
    public Property setProperty(String name, Value[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return sessionWrapper.getObjectWrapper().wrap(sessionWrapper, delegate.setProperty(name, values));
    }

    @Override
    public Property setProperty(String name, Value[] values, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return sessionWrapper.getObjectWrapper().wrap(sessionWrapper, delegate.setProperty(name, values, type));
    }

    @Override
    public Property setProperty(String name, String[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return sessionWrapper.getObjectWrapper().wrap(sessionWrapper, delegate.setProperty(name, values));
    }

    @Override
    public Property setProperty(String name, String[] values, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return sessionWrapper.getObjectWrapper().wrap(sessionWrapper, delegate.setProperty(name, values, type));
    }

    @Override
    public Property setProperty(String name, String value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return sessionWrapper.getObjectWrapper().wrap(sessionWrapper, delegate.setProperty(name, value));
    }

    @Override
    public Property setProperty(String name, String value, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return sessionWrapper.getObjectWrapper().wrap(sessionWrapper, delegate.setProperty(name, value, type));
    }

    @Override
    public Property setProperty(String name, InputStream value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return sessionWrapper.getObjectWrapper().wrap(sessionWrapper, delegate.setProperty(name, value));
    }

    @Override
    public Property setProperty(String name, Binary value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return sessionWrapper.getObjectWrapper().wrap(sessionWrapper, delegate.setProperty(name, value));
    }

    @Override
    public Property setProperty(String name, boolean value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return sessionWrapper.getObjectWrapper().wrap(sessionWrapper, delegate.setProperty(name, value));
    }

    @Override
    public Property setProperty(String name, double value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return sessionWrapper.getObjectWrapper().wrap(sessionWrapper, delegate.setProperty(name, value));
    }

    @Override
    public Property setProperty(String name, BigDecimal value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return sessionWrapper.getObjectWrapper().wrap(sessionWrapper, delegate.setProperty(name, value));
    }

    @Override
    public Property setProperty(String name, long value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return sessionWrapper.getObjectWrapper().wrap(sessionWrapper, delegate.setProperty(name, value));
    }

    @Override
    public Property setProperty(String name, Calendar value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return sessionWrapper.getObjectWrapper().wrap(sessionWrapper, delegate.setProperty(name, value));
    }

    @Override
    public Property setProperty(String name, Node value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return sessionWrapper.getObjectWrapper().wrap(sessionWrapper, delegate.setProperty(name, value));
    }

    @Override
    public Node getNode(String relPath) throws PathNotFoundException, RepositoryException {
        return this.sessionWrapper.getNode(concat(getPath(), relPath));
    }

    @Override
    public NodeIterator getNodes() throws RepositoryException {
        return this.sessionWrapper.getNodes(getPath(), this.delegate.getNodes());
    }

    @Override
    public NodeIterator getNodes(String namePattern) throws RepositoryException {
        return this.sessionWrapper.getNodes(getPath(), this.delegate.getNodes(namePattern));
    }

    @Override
    public NodeIterator getNodes(String[] nameGlobs) throws RepositoryException {
        return this.sessionWrapper.getNodes(getPath(), this.delegate.getNodes(nameGlobs));
    }

    @Override
    public Property getProperty(String relPath) throws PathNotFoundException, RepositoryException {
        return sessionWrapper.getObjectWrapper().wrap(sessionWrapper, delegate.getProperty(relPath));
    }

    @Override
    public PropertyIterator getProperties() throws RepositoryException {
        return sessionWrapper.getObjectWrapper().wrap(sessionWrapper, delegate.getProperties());
    }

    @Override
    public PropertyIterator getProperties(String namePattern) throws RepositoryException {
        return sessionWrapper.getObjectWrapper().wrap(sessionWrapper, delegate.getProperties(namePattern));
    }

    @Override
    public PropertyIterator getProperties(String[] nameGlobs) throws RepositoryException {
        return sessionWrapper.getObjectWrapper().wrap(sessionWrapper, delegate.getProperties(nameGlobs));
    }

    @Override
    public Item getPrimaryItem() throws ItemNotFoundException, RepositoryException {
        return sessionWrapper.getObjectWrapper().wrap(sessionWrapper, delegate.getPrimaryItem());
    }

    @Override
    public String getUUID() throws UnsupportedRepositoryOperationException, RepositoryException {
        return this.delegate.getUUID();
    }

    @Override
    public String getIdentifier() throws RepositoryException {
        return this.delegate.getIdentifier();
    }

    @Override
    public int getIndex() throws RepositoryException {
        return this.delegate.getIndex();
    }

    @Override
    public PropertyIterator getReferences() throws RepositoryException {
        return sessionWrapper.getObjectWrapper().wrap(sessionWrapper, delegate.getReferences());
    }

    @Override
    public PropertyIterator getReferences(String name) throws RepositoryException {
        return sessionWrapper.getObjectWrapper().wrap(sessionWrapper, delegate.getReferences(name));
    }

    @Override
    public PropertyIterator getWeakReferences() throws RepositoryException {
        return sessionWrapper.getObjectWrapper().wrap(sessionWrapper, delegate.getWeakReferences());
    }

    @Override
    public PropertyIterator getWeakReferences(String name) throws RepositoryException {
        return sessionWrapper.getObjectWrapper().wrap(sessionWrapper, delegate.getWeakReferences(name));
    }

    @Override
    public boolean hasNode(String relPath) throws RepositoryException {
        return this.sessionWrapper.nodeExists(concat(getPath(), relPath));
    }

    @Override
    public boolean hasProperty(String relPath) throws RepositoryException {
        return this.sessionWrapper.propertyExists(concat(getPath(), relPath));
    }

    @Override
    public boolean hasNodes() throws RepositoryException {
        return this.sessionWrapper.hasNodes(this.delegate);
    }

    @Override
    public boolean hasProperties() throws RepositoryException {
        return this.delegate.hasProperties();
    }

    @Override
    public NodeType getPrimaryNodeType() throws RepositoryException {
        return this.delegate.getPrimaryNodeType();
    }

    @Override
    public NodeType[] getMixinNodeTypes() throws RepositoryException {
        return this.delegate.getMixinNodeTypes();
    }

    @Override
    public boolean isNodeType(String nodeTypeName) throws RepositoryException {
        return this.delegate.isNodeType(nodeTypeName);
    }

    @Override
    public void setPrimaryType(String nodeTypeName) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        this.delegate.setPrimaryType(nodeTypeName);
    }

    @Override
    public void addMixin(String mixinName) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        this.delegate.addMixin(mixinName);
    }

    @Override
    public void removeMixin(String mixinName) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        this.delegate.removeMixin(mixinName);
    }

    @Override
    public boolean canAddMixin(String mixinName) throws NoSuchNodeTypeException, RepositoryException {
        return this.delegate.canAddMixin(mixinName);
    }

    @Override
    public NodeDefinition getDefinition() throws RepositoryException {
        return this.delegate.getDefinition();
    }

    @Override
    public Version checkin() throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException {
        return this.delegate.checkin();
    }

    @Override
    public void checkout() throws UnsupportedRepositoryOperationException, LockException, ActivityViolationException, RepositoryException {
        this.delegate.checkout();
    }

    @Override
    public void doneMerge(Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        this.delegate.doneMerge(version);
    }

    @Override
    public void cancelMerge(Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        this.delegate.cancelMerge(version);
    }

    @Override
    public void update(String srcWorkspace) throws NoSuchWorkspaceException, AccessDeniedException, LockException, InvalidItemStateException, RepositoryException {
        this.delegate.update(srcWorkspace);
    }

    @Override
    public NodeIterator merge(String srcWorkspace, boolean bestEffort) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        return sessionWrapper.getObjectWrapper().wrap(sessionWrapper, delegate.merge(srcWorkspace, bestEffort));
    }

    @Override
    public String getCorrespondingNodePath(String workspaceName) throws ItemNotFoundException, NoSuchWorkspaceException, AccessDeniedException, RepositoryException {
        return this.delegate.getCorrespondingNodePath(workspaceName);
    }

    @Override
    public NodeIterator getSharedSet() throws RepositoryException {
        return sessionWrapper.getObjectWrapper().wrap(sessionWrapper, delegate.getSharedSet());
    }

    @Override
    public void removeSharedSet() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        this.delegate.removeSharedSet();
    }

    @Override
    public void removeShare() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        this.delegate.removeShare();
    }

    @Override
    public boolean isCheckedOut() throws RepositoryException {
        return this.delegate.isCheckedOut();
    }

    @Override
    public void restore(String versionName, boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        this.delegate.restore(versionName, removeExisting);
    }

    @Override
    public void restore(Version version, boolean removeExisting) throws VersionException, ItemExistsException, InvalidItemStateException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
        this.delegate.restore(version, removeExisting);
    }

    @Override
    public void restore(Version version, String relPath, boolean removeExisting) throws PathNotFoundException, ItemExistsException, VersionException, ConstraintViolationException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        this.delegate.restore(version, relPath, removeExisting);
    }

    @Override
    public void restoreByLabel(String versionLabel, boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        this.delegate.restoreByLabel(versionLabel, removeExisting);
    }

    @Override
    public VersionHistory getVersionHistory() throws UnsupportedRepositoryOperationException, RepositoryException {
        return this.delegate.getVersionHistory();
    }

    @Override
    public Version getBaseVersion() throws UnsupportedRepositoryOperationException, RepositoryException {
        return this.delegate.getBaseVersion();
    }

    @Override
    public Lock lock(boolean isDeep, boolean isSessionScoped) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        return sessionWrapper.getObjectWrapper().wrap(sessionWrapper, delegate.lock(isDeep, isSessionScoped));
    }

    @Override
    public Lock getLock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException {
        return sessionWrapper.getObjectWrapper().wrap(sessionWrapper, delegate.getLock());
    }

    @Override
    public void unlock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        this.delegate.unlock();
    }

    @Override
    public boolean holdsLock() throws RepositoryException {
        return this.delegate.holdsLock();
    }

    @Override
    public boolean isLocked() throws RepositoryException {
        return this.delegate.isLocked();
    }

    @Override
    public void followLifecycleTransition(String transition) throws UnsupportedRepositoryOperationException, InvalidLifecycleTransitionException, RepositoryException {
        this.delegate.followLifecycleTransition(transition);
    }

    @Override
    public String[] getAllowedLifecycleTransistions() throws UnsupportedRepositoryOperationException, RepositoryException {
        return this.delegate.getAllowedLifecycleTransistions();
    }
}
