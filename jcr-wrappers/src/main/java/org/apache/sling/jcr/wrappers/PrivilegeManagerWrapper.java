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
import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.api.security.authorization.PrivilegeManager;

public class PrivilegeManagerWrapper extends BaseWrapper<PrivilegeManager> implements PrivilegeManager {

    public PrivilegeManagerWrapper(SessionWrapper<?> sessionWrapper, PrivilegeManager delegate) {
        super(sessionWrapper, delegate);
    }

    @Override
    public Privilege[] getRegisteredPrivileges() throws RepositoryException {
        return delegate.getRegisteredPrivileges();
    }

    @Override
    public Privilege getPrivilege(String privilegeName) throws AccessControlException, RepositoryException {
        return delegate.getPrivilege(privilegeName);
    }

    @Override
    public Privilege registerPrivilege(String privilegeName, boolean isAbstract, String[] declaredAggregateNames) throws AccessDeniedException, NamespaceException, RepositoryException {
        return delegate.registerPrivilege(privilegeName, isAbstract, declaredAggregateNames);
    }
}
