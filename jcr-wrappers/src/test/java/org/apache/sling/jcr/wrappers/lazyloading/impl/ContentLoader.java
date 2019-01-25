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

import org.apache.sling.jcr.wrappers.SessionWrapper;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;

/** This is the core of the content pre-loading logic. 
 * 
 *  Implementations will need to be clever enough to load the right amount
 *  of content while not doing too much work.
 */
public interface ContentLoader {
    
    /** Load the specified node if present in the pre-loadable content */
    void prepareForGetNode(SessionWrapper s, String absolutePath) throws RepositoryException;
    
    /** Load the child nodes of parentPath if available. 
     * 
     *  This will require knowledge of which paths lead to actual content, 
     *  as this is typically called when walking down the tree before content
     *  that's found in descendant nodes is preloaded.
     */
    void prepareForGetChildNodes(SessionWrapper s, String parentPath) throws RepositoryException;
    
    /** Create the specified node, called if this ContentLoader has found out
     *  that it's needed to reach pre-loadable content.
     * 
     *  If specific nodetypes are required, this method will need to find out
     *  what the appropriate nodetype is.
    */
    Node createIntermediateNode(Node parent, String childName) throws RepositoryException;
    
    /** Load the required content for the supplied Query, if its scope extends
     *  to pre-loadable content.
     *  
     *  This will require analyzing Queries to find out if pre-loadable content
     *  influences their results.
     * 
     *  The set of Queries that cause pre-loading will need to be strictly 
     *  defined to avoid pre-loading everything as soon as a somewhat
     *  broad Query is prepared. 
     * 
     *  This will have an impact at the application level, where too
     *  broad queries will be ignored by this method, causing useful content
     *  to be missing.
     */
    void prepareForQuery(SessionWrapper s, Query q) throws RepositoryException;
}