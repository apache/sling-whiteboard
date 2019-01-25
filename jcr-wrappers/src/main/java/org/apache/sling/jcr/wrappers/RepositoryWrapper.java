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

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import org.apache.jackrabbit.api.JackrabbitSession;

public class RepositoryWrapper<T extends Repository> implements Repository {
    public final T jcr;
    private final ObjectWrapper objectWrapper;

    public RepositoryWrapper(ObjectWrapper factory, T jcr) {
        this.jcr = jcr;
        this.objectWrapper = factory;
    }
    
    public ObjectWrapper getObjectWrapper() {
        return objectWrapper;
    }

    @Override
    public String[] getDescriptorKeys() {
        return jcr.getDescriptorKeys();
    }

    @Override
    public boolean isStandardDescriptor(String key) {
        return jcr.isStandardDescriptor(key);
    }

    @Override
    public boolean isSingleValueDescriptor(String key) {
        return jcr.isSingleValueDescriptor(key);
    }

    @Override
    public Value getDescriptorValue(String key) {
        return jcr.getDescriptorValue(key);
    }

    @Override
    public Value[] getDescriptorValues(String key) {
        return jcr.getDescriptorValues(key);
    }

    @Override
    public String getDescriptor(String key) {
        return jcr.getDescriptor(key);
    }

    @Override
    public Session login(Credentials credentials, String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return objectWrapper.wrap(this, jcr.login(credentials, workspaceName));
    }

    @Override
    public Session login(Credentials credentials) throws LoginException, RepositoryException {
        return login(credentials, null);
    }

    @Override
    public Session login(String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return login(null, workspaceName);
    }

    @Override
    public Session login() throws LoginException, RepositoryException {
        return login(null, null);
    }

    public Session wrap(Session session) throws RepositoryException {
        if (session instanceof SessionWrapper) {
            return session;
        }
        return session instanceof JackrabbitSession ?
                new JackrabbitSessionWrapper(this, (JackrabbitSession) session) :
                new SessionWrapper<>(this, session);
    }
}
