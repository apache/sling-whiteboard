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

import java.security.Principal;
import java.util.Iterator;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.AuthorizableExistsException;
import org.apache.jackrabbit.api.security.user.AuthorizableTypeException;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.Query;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;

public class UserManagerWrapper extends BaseWrapper<UserManager> implements UserManager {

    public UserManagerWrapper(SessionWrapper<JackrabbitSession> sessionWrapper, UserManager delegate) {
        super(sessionWrapper, delegate);
    }

    @Override
    public Authorizable getAuthorizable(String id) throws RepositoryException {
        return delegate.getAuthorizable(id);
    }

    @Override
    public <T extends Authorizable> T getAuthorizable(String id, Class<T> authorizableClass) throws AuthorizableTypeException, RepositoryException {
        return delegate.getAuthorizable(id, authorizableClass);
    }

    @Override
    public Authorizable getAuthorizable(Principal principal) throws RepositoryException {
        return delegate.getAuthorizable(principal);
    }

    @Override
    public Authorizable getAuthorizableByPath(String path) throws UnsupportedRepositoryOperationException, RepositoryException {
        return delegate.getAuthorizableByPath(path);
    }

    @Override
    public Iterator<Authorizable> findAuthorizables(String relPath, String value) throws RepositoryException {
        return delegate.findAuthorizables(relPath, value);
    }

    @Override
    public Iterator<Authorizable> findAuthorizables(String relPath, String value, int searchType) throws RepositoryException {
        return delegate.findAuthorizables(relPath, value, searchType);
    }

    @Override
    public Iterator<Authorizable> findAuthorizables(Query query) throws RepositoryException {
        return delegate.findAuthorizables(query);
    }

    @Override
    public User createUser(String userID, String password) throws AuthorizableExistsException, RepositoryException {
        User user = delegate.createUser(userID, password);
        delegate.createUser(userID, password, user.getPrincipal(), user.getPath());
        return user;
    }

    @Override
    public User createUser(String userID, String password, Principal principal, String intermediatePath) throws AuthorizableExistsException, RepositoryException {
        User user = delegate.createUser(userID, password, principal, intermediatePath);
        delegate.createUser(userID, password, principal, user.getPath());
        return user;
    }

    @Override
    public User createSystemUser(String userID, String intermediatePath) throws AuthorizableExistsException, RepositoryException {
        User user = delegate.createSystemUser(userID, intermediatePath);
        delegate.createSystemUser(userID, user.getPath());
        return user;
    }

    @Override
    public Group createGroup(String groupID) throws AuthorizableExistsException, RepositoryException {
        Group group = delegate.createGroup(groupID);
        delegate.createGroup(groupID, group.getPrincipal(), group.getPath());
        return group;
    }

    @Override
    public Group createGroup(Principal principal) throws AuthorizableExistsException, RepositoryException {
        Group group = delegate.createGroup(principal);
        delegate.createGroup(group.getID(), principal, group.getPath());
        return group;
    }

    @Override
    public Group createGroup(Principal principal, String intermediatePath) throws AuthorizableExistsException, RepositoryException {
        Group group = delegate.createGroup(principal, intermediatePath);
        delegate.createGroup(principal, group.getPath());
        return group;
    }

    @Override
    public Group createGroup(String groupID, Principal principal, String intermediatePath) throws AuthorizableExistsException, RepositoryException {
        Group group = delegate.createGroup(groupID, principal, intermediatePath);
        delegate.createGroup(groupID, principal, group.getPath());
        return group;
    }

    @Override
    public boolean isAutoSave() {
        return delegate.isAutoSave();
    }

    @Override
    public void autoSave(boolean enable) throws UnsupportedRepositoryOperationException, RepositoryException {
        delegate.autoSave(enable);
        delegate.autoSave(enable);
    }
}
