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
package org.apache.sling.jcr.wrappers.lazyloading.tests;

import java.io.IOException;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.sling.commons.testing.jcr.RepositoryUtil;
import org.apache.sling.jcr.wrappers.lazyloading.impl.ContentLoader;
import org.apache.sling.jcr.wrappers.lazyloading.impl.LazyLoadingRepository;
import org.apache.sling.jcr.wrappers.lazyloading.impl.LazyLoadingSession;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Base class for tests which keeps a static repository but cleans it
 *  up for each test class.
 */
public class TestBase {
    
    protected static final Logger log = LoggerFactory.getLogger(TestBase.class);
    protected static Repository repository;
    protected Session session;
    protected static final Credentials ADMIN_CREDENTIALS = new SimpleCredentials("admin", "admin".toCharArray());
    protected static Supplier<ContentLoader> CONTENT_LOADER_SUPPLIER = () -> { return new TestContentLoader(); };
    
    /** These are the paths for which we have test content, as system view files */
    protected static final String [] TEST_CONTENT_PATHS = {
        "/content/slingshot",
        "/content/starter"
    };
    
    static void visitRecursively(Node n, Predicate<String> pathFilter, Set<String> allPathsFound) throws RepositoryException {
        if(!"/".equals(n.getPath()) && !pathFilter.test(n.getPath())) {
            return;
        }
        log.debug("visit({})", n.getPath());

        if(allPathsFound != null) {
            allPathsFound.add(n.getPath());
        }

        final NodeIterator it = n.getNodes();
        while(it.hasNext()) {
            final Node next = it.nextNode();
            visitRecursively(next, pathFilter, allPathsFound);
        }
    }

    protected void assertNoContent() throws RepositoryException {
        final Session rawSession = ((LazyLoadingSession)session).getWrappedSession();
        for(String path : TEST_CONTENT_PATHS) {
            assertFalse(rawSession.nodeExists(path), "Expecting path to be absent before visiting:" + path);
        }
    }

    protected static void setupRepository() {
        log.debug("Initializing Repository");
        final Repository rawRepository = new Jcr(new Oak()).createRepository();
        repository = new LazyLoadingRepository(rawRepository, CONTENT_LOADER_SUPPLIER.get());
    }
    
    /** Some test classes use a single repository for all tests, in which case they
     *  can override this and call setupRepository in a @BeforeAll method.
     */
    protected void beforeEachTest() {
        setupRepository();
    }

    @BeforeEach
    public void setup() throws RepositoryException, IOException {
        beforeEachTest();
        session = repository.login(ADMIN_CREDENTIALS);
        RepositoryUtil.registerNodeType(session, getClass()
                .getResourceAsStream("/SLING-INF/nodetypes/folder.cnd"));
    }
    
    @AfterEach
    public void cleanup() {
        session.logout();
    }
}