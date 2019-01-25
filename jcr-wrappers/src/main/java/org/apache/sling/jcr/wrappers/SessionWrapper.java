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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;
import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.retention.RetentionManager;
import javax.jcr.security.AccessControlManager;
import javax.jcr.version.VersionException;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class SessionWrapper<T extends Session> implements Session {
    private final RepositoryWrapper repository;
    private final ObjectWrapper objectWrapper;
    public final T wrappedSession;

    public SessionWrapper(RepositoryWrapper repository, T wrappedSession) {
        this.repository = repository;
        this.wrappedSession = wrappedSession;
        this.objectWrapper = repository.getObjectWrapper();
    }
    
    public ObjectWrapper getObjectWrapper() {
        return objectWrapper;
    }

    public T getWrappedSession() {
        return wrappedSession;
    }

    @Override
    public Repository getRepository() {
        return this.repository;
    }

    @Override
    public String getUserID() {
        return this.wrappedSession.getUserID();
    }

    @Override
    public String[] getAttributeNames() {
        return this.wrappedSession.getAttributeNames();
    }

    @Override
    public Object getAttribute(String name) {
        return this.wrappedSession.getAttribute(name);
    }

    @Override
    public Node getRootNode() throws RepositoryException {
        return objectWrapper.wrap(this, wrappedSession.getRootNode());
    }

    public NodeIterator getNodes(String path, NodeIterator children) throws RepositoryException {
        return objectWrapper.wrap(this, children);
    }

    public boolean hasNodes(Node node) throws RepositoryException {
        return node.hasNodes();
    }

    @Override
    public Session impersonate(Credentials credentials) throws LoginException, RepositoryException {
        return wrappedSession.impersonate(credentials);
    }

    @Override
    public Node getNodeByUUID(String uuid) throws ItemNotFoundException, RepositoryException {
        return objectWrapper.wrap(this, wrappedSession.getNodeByUUID(uuid));
    }

    @Override
    public Node getNodeByIdentifier(String id) throws ItemNotFoundException, RepositoryException {
        return objectWrapper.wrap(this, wrappedSession.getNodeByIdentifier(id));
    }

    @Override
    public Item getItem(String absPath) throws PathNotFoundException, RepositoryException {
        return objectWrapper.wrap(this, wrappedSession.getItem(absPath));
    }

    @Override
    public Node getNode(String absPath) throws PathNotFoundException, RepositoryException {
        return objectWrapper.wrap(this, wrappedSession.getNode(absPath));
    }

    @Override
    public Property getProperty(String absPath) throws PathNotFoundException, RepositoryException {
        return objectWrapper.wrap(this, wrappedSession.getProperty(absPath));
    }

    @Override
    public boolean itemExists(String absPath) throws RepositoryException {
        return this.wrappedSession.itemExists(absPath);
    }

    @Override
    public boolean nodeExists(String absPath) throws RepositoryException {
        return this.wrappedSession.nodeExists(absPath);
    }

    @Override
    public boolean propertyExists(String absPath) throws RepositoryException {
        return this.wrappedSession.propertyExists(absPath);
    }

    @Override
    public void removeItem(String absPath) throws VersionException, LockException, ConstraintViolationException, AccessDeniedException, RepositoryException {
        this.wrappedSession.removeItem(absPath);
    }

    @Override
    public void save() throws AccessDeniedException, ItemExistsException, ReferentialIntegrityException, ConstraintViolationException, InvalidItemStateException, VersionException, LockException, NoSuchNodeTypeException, RepositoryException {
        this.wrappedSession.save();
    }

    @Override
    public void refresh(boolean keepChanges) throws RepositoryException {
        this.wrappedSession.refresh(keepChanges);
    }

    public void refresh(String path, Item item, boolean keepChanges) throws RepositoryException {
        item.refresh(keepChanges);
    }

    @Override
    public boolean hasPendingChanges() throws RepositoryException {
        return this.wrappedSession.hasPendingChanges();
    }

    @Override
    public ValueFactory getValueFactory() throws UnsupportedRepositoryOperationException, RepositoryException {
        return this.wrappedSession.getValueFactory();
    }

    @Override
    public boolean hasPermission(String absPath, String actions) throws RepositoryException {
        return this.wrappedSession.hasPermission(absPath, actions);
    }

    @Override
    public void checkPermission(String absPath, String actions) throws AccessControlException, RepositoryException {
        this.wrappedSession.checkPermission(absPath, actions);
    }

    @Override
    public boolean hasCapability(String methodName, Object target, Object[] arguments) throws RepositoryException {
        return this.wrappedSession.hasCapability(methodName, target, arguments);
    }

    @Override
    public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehavior) throws PathNotFoundException, ConstraintViolationException, VersionException, LockException, RepositoryException {
        return this.wrappedSession.getImportContentHandler(parentAbsPath, uuidBehavior);
    }

    @Override
    public void importXML(String parentAbsPath, InputStream in, int uuidBehavior) throws IOException, PathNotFoundException, ItemExistsException, ConstraintViolationException, VersionException, InvalidSerializedDataException, LockException, RepositoryException {
        this.wrappedSession.importXML(parentAbsPath, in, uuidBehavior);
    }

    @Override
    public void exportSystemView(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse) throws PathNotFoundException, SAXException, RepositoryException {
        this.wrappedSession.exportSystemView(absPath, contentHandler, skipBinary, noRecurse);
    }

    @Override
    public void exportSystemView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse) throws IOException, PathNotFoundException, RepositoryException {
        this.wrappedSession.exportSystemView(absPath, out, skipBinary, noRecurse);
    }

    @Override
    public void exportDocumentView(String absPath, ContentHandler contentHandler, boolean skipBinary, boolean noRecurse) throws PathNotFoundException, SAXException, RepositoryException {
        this.wrappedSession.exportDocumentView(absPath, contentHandler, skipBinary, noRecurse);
    }

    @Override
    public void exportDocumentView(String absPath, OutputStream out, boolean skipBinary, boolean noRecurse) throws IOException, PathNotFoundException, RepositoryException {
        this.wrappedSession.exportDocumentView(absPath, out, skipBinary, noRecurse);
    }

    @Override
    public void setNamespacePrefix(String prefix, String uri) throws NamespaceException, RepositoryException {
        this.wrappedSession.setNamespacePrefix(prefix, uri);
    }

    @Override
    public String[] getNamespacePrefixes() throws RepositoryException {
        return this.wrappedSession.getNamespacePrefixes();
    }

    @Override
    public String getNamespaceURI(String prefix) throws NamespaceException, RepositoryException {
        return this.wrappedSession.getNamespaceURI(prefix);
    }

    @Override
    public String getNamespacePrefix(String uri) throws NamespaceException, RepositoryException {
        return this.wrappedSession.getNamespacePrefix(uri);
    }

    @Override
    public void logout() {
        this.wrappedSession.logout();
    }

    @Override
    public boolean isLive() {
        return this.wrappedSession.isLive();
    }

    @Override
    public void addLockToken(String lt) {
        this.wrappedSession.addLockToken(lt);
    }

    @Override
    public String[] getLockTokens() {
        return this.wrappedSession.getLockTokens();
    }

    @Override
    public void removeLockToken(String lt) {
        this.wrappedSession.removeLockToken(lt);
    }

    @Override
    public AccessControlManager getAccessControlManager() throws UnsupportedRepositoryOperationException, RepositoryException {
        AccessControlManager manager = this.wrappedSession.getAccessControlManager();
        return manager instanceof JackrabbitAccessControlManager ?
                new JackrabbitAccessControlManagerWrapper(this, (JackrabbitAccessControlManager) manager) :
                new AccessControlManagerWrapper<>(this, manager);
    }

    @Override
    public RetentionManager getRetentionManager() throws UnsupportedRepositoryOperationException, RepositoryException {
        return this.wrappedSession.getRetentionManager();
    }

    @Override
    public void move(String srcAbsPath, String destAbsPath) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        this.wrappedSession.move(srcAbsPath, destAbsPath);
    }

    @Override
    public Workspace getWorkspace() {
        return new WorkspaceWrapper(this, this.wrappedSession.getWorkspace());
    }

    public Node addNode(String parent, String path, String name) throws RepositoryException {
        return objectWrapper.wrap(this, wrappedSession.getNode(parent).addNode(name));
    }

    public Node addNode(String parent, String path, String name, String type) throws RepositoryException {
        return objectWrapper.wrap(this, wrappedSession.getNode(parent).addNode(name, type));
    }
}
