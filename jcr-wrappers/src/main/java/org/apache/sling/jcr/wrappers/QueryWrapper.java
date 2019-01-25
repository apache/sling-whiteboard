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

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;

public class QueryWrapper extends BaseWrapper<Query> implements Query {

    public QueryWrapper(SessionWrapper<?> sessionWrapper, Query wrappedQuery) {
        super(sessionWrapper, wrappedQuery);
    }

    @Override
    public QueryResult execute() throws InvalidQueryException, RepositoryException {
        return sessionWrapper.getObjectWrapper().wrap(sessionWrapper, delegate.execute());
    }

    @Override
    public void setLimit(long limit) {
        delegate.setLimit(limit);
    }

    @Override
    public void setOffset(long offset) {
        delegate.setOffset(offset);
    }

    @Override
    public String getStatement() {
        return delegate.getStatement();
    }

    @Override
    public String getLanguage() {
        return delegate.getLanguage();
    }

    @Override
    public String getStoredQueryPath() throws ItemNotFoundException, RepositoryException {
        try {
            return delegate.getStoredQueryPath();
        } catch (ItemNotFoundException ex) {
            try {
                return delegate.getStoredQueryPath();
            } catch (ItemNotFoundException ignore) {
                throw ex;
            }
        }
    }

    @Override
    public Node storeAsNode(String absPath) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, UnsupportedRepositoryOperationException, RepositoryException {
        return sessionWrapper.getObjectWrapper().wrap(sessionWrapper, delegate.storeAsNode(absPath));
    }

    @Override
    public void bindValue(String varName, Value value) throws IllegalArgumentException, RepositoryException {
        delegate.bindValue(varName, value);
    }

    @Override
    public String[] getBindVariableNames() throws RepositoryException {
        return delegate.getBindVariableNames();
    }
}
