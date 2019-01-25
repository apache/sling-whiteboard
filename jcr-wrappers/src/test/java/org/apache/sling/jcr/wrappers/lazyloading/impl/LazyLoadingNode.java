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

import org.apache.sling.jcr.wrappers.NodeWrapper;
import org.apache.sling.jcr.wrappers.SessionWrapper;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

public class LazyLoadingNode extends NodeWrapper {

    private final ContentLoader contentLoader;
    private final LazyLoadingSession lazyLoadingSession;
    
    public LazyLoadingNode(SessionWrapper sessionWrapper, Node node) {
        super(sessionWrapper, node);
        lazyLoadingSession = (LazyLoadingSession)sessionWrapper;
        contentLoader = ((LazyLoadingSession)sessionWrapper).getContentLoader();
    }

    @Override
    public NodeIterator getNodes() throws RepositoryException {
        contentLoader.prepareForGetChildNodes(lazyLoadingSession, getPath());
        return super.getNodes();
    }
}