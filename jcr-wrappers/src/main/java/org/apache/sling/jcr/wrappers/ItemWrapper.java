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

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

public class ItemWrapper<T extends Item> extends BaseWrapper<T> implements Item {
    public ItemWrapper(SessionWrapper sessionWrapper, T delegate) {
        super(sessionWrapper, delegate);
    }

    @Override
    public String getPath() throws RepositoryException {
        return delegate.getPath();
    }

    @Override
    public String getName() throws RepositoryException {
        return delegate.getName();
    }

    @Override
    public Item getAncestor(int depth) throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        return this.sessionWrapper.getItem(this.delegate.getAncestor(depth).getPath());
    }

    @Override
    public Node getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        return this.sessionWrapper.getNode(this.delegate.getParent().getPath());
    }

    @Override
    public int getDepth() throws RepositoryException {
        return delegate.getDepth();
    }

    @Override
    public Session getSession() throws RepositoryException {
        return sessionWrapper;
    }

    @Override
    public boolean isNode() {
        return delegate.isNode();
    }

    @Override
    public boolean isNew() {
        return delegate.isNew();
    }

    @Override
    public boolean isModified() {
        return delegate.isModified();
    }

    @Override
    public boolean isSame(Item otherItem) throws RepositoryException {
        return delegate.isSame(sessionWrapper.getObjectWrapper().unwrap(otherItem));
    }

    @Override
    public void accept(final ItemVisitor visitor) throws RepositoryException {
        delegate.accept(new ItemVisitor() {
            @Override
            public void visit(Property property) throws RepositoryException {
                visitor.visit(sessionWrapper.getObjectWrapper().wrap(sessionWrapper, property));
            }

            @Override
            public void visit(Node node) throws RepositoryException {
                visitor.visit(sessionWrapper.getObjectWrapper().wrap(sessionWrapper, node));
            }
        });
    }

    @Override
    public void save() throws AccessDeniedException, ItemExistsException, ConstraintViolationException, InvalidItemStateException, ReferentialIntegrityException, VersionException, LockException, NoSuchNodeTypeException, RepositoryException {
        this.sessionWrapper.save();
    }

    @Override
    public void refresh(boolean keepChanges) throws InvalidItemStateException, RepositoryException {
        this.sessionWrapper.refresh(getPath(), delegate, keepChanges);
    }

    @Override
    public void remove() throws VersionException, LockException, ConstraintViolationException, AccessDeniedException, RepositoryException {
        this.sessionWrapper.removeItem(this.getPath());
    }
}
