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
import java.util.Set;
import javax.jcr.AccessDeniedException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlPolicy;

public class JackrabbitAccessControlManagerWrapper extends AccessControlManagerWrapper<JackrabbitAccessControlManager> implements JackrabbitAccessControlManager {
    public JackrabbitAccessControlManagerWrapper(SessionWrapper<?> sessionWrapper, JackrabbitAccessControlManager delegate) {
        super(sessionWrapper, delegate);
    }

    @Override
    public JackrabbitAccessControlPolicy[] getApplicablePolicies(Principal principal) throws AccessDeniedException, AccessControlException, UnsupportedRepositoryOperationException, RepositoryException {
        return delegate.getApplicablePolicies(principal);
    }

    @Override
    public JackrabbitAccessControlPolicy[] getPolicies(Principal principal) throws AccessDeniedException, AccessControlException, UnsupportedRepositoryOperationException, RepositoryException {
        return delegate.getPolicies(principal);
    }

    @Override
    public AccessControlPolicy[] getEffectivePolicies(Set<Principal> principals) throws AccessDeniedException, AccessControlException, UnsupportedRepositoryOperationException, RepositoryException {
        return delegate.getEffectivePolicies(principals);
    }

    @Override
    public boolean hasPrivileges(String absPath, Set<Principal> principals, Privilege[] privileges) throws PathNotFoundException, AccessDeniedException, RepositoryException {
        return delegate.hasPrivileges(absPath, principals, privileges);
    }

    @Override
    public Privilege[] getPrivileges(String absPath, Set<Principal> principals) throws PathNotFoundException, AccessDeniedException, RepositoryException {
        return delegate.getPrivileges(absPath, principals);
    }
}
