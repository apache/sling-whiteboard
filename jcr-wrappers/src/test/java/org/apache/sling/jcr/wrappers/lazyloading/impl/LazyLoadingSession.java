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
package org.apache.sling.jcr.wrappers.lazyloading.impl;

import org.apache.sling.jcr.wrappers.RepositoryWrapper;
import org.apache.sling.jcr.wrappers.SessionWrapper;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class LazyLoadingSession<T extends Session> extends SessionWrapper<T> {
    private final ContentLoader contentLoader;

    public LazyLoadingSession(RepositoryWrapper repository, T wrappedSession) {
        super(repository, wrappedSession);
        contentLoader = ((LazyLoadingRepository)repository).getContentLoader();
    }
    
    @Override
    public boolean nodeExists(String path) throws RepositoryException {
        contentLoader.prepareForGetNode(this, path);
        return super.nodeExists(path);
    }
    
    @Override
    public Node getRootNode() throws RepositoryException {
        contentLoader.prepareForGetNode(this, "/");
        return getObjectWrapper().wrap(this, super.getRootNode());
    }
    
    @Override
    public Node getNode(String path) throws RepositoryException {
        contentLoader.prepareForGetNode(this, path);
        return getObjectWrapper().wrap(this, super.getNode(path));
    }
    
    public ContentLoader getContentLoader() {
        return contentLoader;
    }
}