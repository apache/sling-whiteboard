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

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;

/** Builds Wrapped instances of JCR objects. Meant to be extended to 
 *  return the appropriate custom wrappers.
 */
public interface ObjectWrapper {
    public <F> F unwrap(F source);
        
    Session wrap(RepositoryWrapper r, Session s);
    Item wrap(SessionWrapper s, Item it);
    Node wrap(SessionWrapper s, Node n);
    Property wrap(SessionWrapper s, Property p);
    
    Lock wrap(SessionWrapper s, Lock lock);
    
    Query wrap(SessionWrapper s, Query q);
    QueryResult wrap(SessionWrapper s, QueryResult qr);
    NodeIterator wrap(SessionWrapper s, final NodeIterator iter);
    PropertyIterator wrap(SessionWrapper s, final PropertyIterator iter);
    RowIterator wrap(SessionWrapper s, final RowIterator iter);
}
