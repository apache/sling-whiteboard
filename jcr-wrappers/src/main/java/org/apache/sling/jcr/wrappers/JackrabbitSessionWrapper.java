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
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.JackrabbitWorkspace;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.UserManager;

public class JackrabbitSessionWrapper extends SessionWrapper<JackrabbitSession> implements JackrabbitSession {
    public JackrabbitSessionWrapper(RepositoryWrapper repository, JackrabbitSession jcr) {
        super(repository, jcr);
    }

    @Override
    public Workspace getWorkspace() {
        return new JackrabbitWorkspaceWrapper(this, (JackrabbitWorkspace) this.wrappedSession.getWorkspace());
    }

    @Override
    public boolean hasPermission(String absPath, String... actions) throws RepositoryException {
        return wrappedSession.hasPermission(absPath, actions);
    }

    @Override
    public PrincipalManager getPrincipalManager() throws AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        return wrappedSession.getPrincipalManager();
    }

    @Override
    public UserManager getUserManager() throws AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        return new UserManagerWrapper(this, wrappedSession.getUserManager());
    }

    @Override
    public Item getItemOrNull(String absPath) throws RepositoryException {
        if (super.itemExists(absPath)) {
            return super.getItem(absPath);
        } else {
            return null;
        }
    }

    @Override
    public Property getPropertyOrNull(String absPath) throws RepositoryException {
        if (super.propertyExists(absPath)) {
            return super.getProperty(absPath);
        } else {
            return null;
        }
    }

    @Override
    public Node getNodeOrNull(String absPath) throws RepositoryException {
        if (super.nodeExists(absPath)) {
            return super.getNode(absPath);
        } else {
            return null;
        }
    }
}
