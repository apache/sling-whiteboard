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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;

public class LockWrapper extends BaseWrapper<Lock> implements Lock {
    public LockWrapper(SessionWrapper<?> sessionWrapper, Lock source) {
        super(sessionWrapper, source);
    }

    @Override
    public String getLockOwner() {
        return delegate.getLockOwner();
    }

    @Override
    public boolean isDeep() {
        return delegate.isDeep();
    }

    @Override
    public Node getNode() {
        return sessionWrapper.getObjectWrapper().wrap(sessionWrapper, delegate.getNode());
    }

    @Override
    public String getLockToken() {
        return delegate.getLockToken();
    }

    @Override
    public long getSecondsRemaining() throws RepositoryException {
        return delegate.getSecondsRemaining();
    }

    @Override
    public boolean isLive() throws RepositoryException {
        return delegate.isLive();
    }

    @Override
    public boolean isSessionScoped() {
        return delegate.isSessionScoped();
    }

    @Override
    public boolean isLockOwningSession() {
        return delegate.isLockOwningSession();
    }

    @Override
    public void refresh() throws LockException, RepositoryException {
        delegate.refresh();
    }
}
