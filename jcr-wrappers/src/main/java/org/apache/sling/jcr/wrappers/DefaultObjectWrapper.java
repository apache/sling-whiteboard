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

import java.util.Iterator;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.lock.Lock;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.commons.iterator.NodeIteratorAdapter;

/** Builds Wrapped instances of JCR objects. Meant to be extended to 
 *  return the appropriate custom wrappers.
 */
public class DefaultObjectWrapper implements ObjectWrapper {
    public Node wrap(SessionWrapper s, Node n) {
        return new NodeWrapper(s, n);
    }
    
    public Session wrap(RepositoryWrapper r, Session s) {
        return s instanceof JackrabbitSession ?
            new JackrabbitSessionWrapper(r, (JackrabbitSession) s) :
            new SessionWrapper(r, s);
    }
    
    public Property wrap(SessionWrapper s, Property p) {
        return new PropertyWrapper(s, p);
    }
    
    public Item wrap(SessionWrapper s, Item it) {
        return new ItemWrapper(s, it);
    }
    
    public Lock wrap(SessionWrapper s, Lock lock) {
        return new LockWrapper(s, lock);
    }
    
    public Query wrap(SessionWrapper s, Query q) {
        return new QueryWrapper(s, q);
    }
    
    public QueryResult wrap(SessionWrapper s, QueryResult qr) {
        return new QueryResultWrapper(s, qr);
    }
    
    public NodeIterator wrap(SessionWrapper s, final NodeIterator iter) {
        return new NodeIteratorAdapter(new Iterator<Node>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public Node next() {
                return wrap(s, iter.nextNode());
            }

            @Override
            public void remove() {
                iter.remove();
            }
        });
    }

    public PropertyIterator wrap(SessionWrapper s, final PropertyIterator iter) {
        return new PropertyIterator() {
            @Override
            public Property nextProperty() {
                return wrap(s, iter.nextProperty());
            }

            @Override
            public void skip(long skipNum) {
                iter.skip(skipNum);
            }

            @Override
            public long getSize() {
                return iter.getSize();
            }

            @Override
            public long getPosition() {
                return iter.getPosition();
            }

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public void remove() {
                iter.remove();
            }

            @Override
            public Object next() {
                // TODO cast to Property ok??
                return wrap(s, (Property)iter.next());
            }
        };
    }

    public RowIterator wrap(SessionWrapper s, final RowIterator iter) {
        return new RowIterator() {

            @Override
            public Row nextRow() {
                final Row row = iter.nextRow();

                return new Row() {
                    @Override
                    public Value[] getValues() throws RepositoryException {
                        return row.getValues();
                    }

                    @Override
                    public Value getValue(String s) throws ItemNotFoundException, RepositoryException {
                        return row.getValue(s);
                    }

                    @Override
                    public Node getNode() throws RepositoryException {
                        return wrap(s, row.getNode());
                    }

                    @Override
                    public Node getNode(String path) throws RepositoryException {
                        return wrap(s, row.getNode(path));
                    }

                    @Override
                    public String getPath() throws RepositoryException {
                        return row.getPath();
                    }

                    @Override
                    public String getPath(String s) throws RepositoryException {
                        return row.getPath(s);
                    }

                    @Override
                    public double getScore() throws RepositoryException {
                        return row.getScore();
                    }

                    @Override
                    public double getScore(String s) throws RepositoryException {
                        return row.getScore(s);
                    }
                };
            }

            @Override
            public void skip(long l) {
                iter.skip(l);
            }

            @Override
            public long getSize() {
                return iter.getSize();
            }

            @Override
            public long getPosition() {
                return iter.getPosition();
            }

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public Object next() {
                return nextRow();
            }

            @Override
            public void remove() {
                iter.remove();
            }
        };
    }

    public <F> F unwrap(F source) {
        return (F) (source instanceof BaseWrapper ? ((BaseWrapper) source).delegate : source);
    }
}
