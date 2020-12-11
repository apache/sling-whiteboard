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
package org.apache.sling.repositorymaintainance.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeast;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.InvalidNodeTypeDefinitionException;
import javax.jcr.nodetype.NodeTypeExistsException;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionManager;

import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.repositorymaintainance.VersionCleanupPathConfig;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

public class VersionCleanupTest {

    private VersionManager versionManager;

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    private List<VersionCleanupPath> globalConfig;

    private Session session;

    @Before
    public void init() throws LoginException, InvalidNodeTypeDefinitionException, NodeTypeExistsException,
            UnsupportedRepositoryOperationException, UnsupportedEncodingException, ParseException, RepositoryException,
            IOException {

        session = context.resourceResolver().adaptTo(Session.class);
        versionManager = session.getWorkspace().getVersionManager();
        InputStream cnd = getClass().getResourceAsStream("/nodetypes.cnd");
        CndImporter.registerNodeTypes(new InputStreamReader(cnd, "UTF-8"), session);

        context.load().json("/version-content.json", "/content/apache/sling-apache-org");

        globalConfig = Collections.singletonList(new VersionCleanupPath(new VersionCleanupPathConfig() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public boolean keepVersions() {
                return false;
            }

            @Override
            public int limit() {
                return 5;
            }

            @Override
            public String path() {
                return "/";
            }

        }));

    }

    private void doVersions(String path, int count) throws RepositoryException {
        Node node = session.getNode(path);
        node.addMixin("mix:versionable");
        session.save();
        for (int i = 0; i < count; i++) {
            versionManager.checkpoint(path);
        }
    }

    @Test(timeout = 5000)
    public void testRunnable() throws InterruptedException, VersionException, UnsupportedRepositoryOperationException,
            InvalidItemStateException, LockException, RepositoryException {

        doVersions("/content/apache/sling-apache-org/index", 10);

        final VersionCleanup vcs = new VersionCleanup(globalConfig, context.getService(ResourceResolverFactory.class));

        vcs.start();
        while (vcs.isRunning()) {
            TimeUnit.SECONDS.sleep(2);
        }

        assertFalse(vcs.isFailed());
        assertFalse(vcs.isRunning());
        assertNull(vcs.getLastMessage());
        assertEquals(5L, vcs.getLastCleanedVersionsCount());
    }

    @Test(timeout = 5000)
    public void testStop() throws InterruptedException, VersionException, UnsupportedRepositoryOperationException,
            InvalidItemStateException, LockException, RepositoryException {

        doVersions("/content/apache/sling-apache-org/index", 100);

        final VersionCleanup vcs = new VersionCleanup(globalConfig, context.getService(ResourceResolverFactory.class));

        vcs.start();
        assertTrue(vcs.isRunning());
        vcs.stop();
        while (vcs.isRunning()) {
            TimeUnit.SECONDS.sleep(2);
        }
        assertFalse(vcs.isRunning());
        assertNull(vcs.getLastMessage());
    }

    @Test(timeout = 5000)
    public void testReRun() throws InterruptedException, VersionException, UnsupportedRepositoryOperationException,
            InvalidItemStateException, LockException, RepositoryException {

        doVersions("/content/apache/sling-apache-org/index", 100);

        final VersionCleanup vcs = Mockito
                .spy(new VersionCleanup(globalConfig, context.getService(ResourceResolverFactory.class)));

        vcs.start();
        vcs.start();

        Mockito.verify(vcs, atLeast(2)).isRunning();
        while (vcs.isRunning()) {
            TimeUnit.SECONDS.sleep(2);
        }
        assertFalse(vcs.isRunning());
        assertNull(vcs.getLastMessage());

    }

    @Test(timeout = 5000)
    public void testMissingServiceUser()
            throws InterruptedException, VersionException, UnsupportedRepositoryOperationException,
            InvalidItemStateException, LockException, RepositoryException, LoginException {

        ResourceResolverFactory factory = Mockito.mock(ResourceResolverFactory.class);
        Mockito.when(factory.getServiceResourceResolver(Mockito.anyMap()))
                .thenThrow(new LoginException("No service user"));
        final VersionCleanup vcs = Mockito.spy(new VersionCleanup(globalConfig, factory));

        vcs.start();

        while (vcs.isRunning()) {
            TimeUnit.SECONDS.sleep(2);
        }
        assertFalse(vcs.isRunning());
        assertTrue(vcs.isFailed());
        assertNotNull(vcs.getLastMessage());
    }

    @Test(timeout = 5000)
    public void testDeleted() throws InterruptedException, VersionException, UnsupportedRepositoryOperationException,
            InvalidItemStateException, LockException, RepositoryException, LoginException, PersistenceException {
        doVersions("/content/apache/sling-apache-org/index", 10);
        doVersions("/content/apache/sling-apache-org/test2", 3);
        context.resourceResolver().delete(context.resourceResolver().getResource("/content/apache/sling-apache-org/test2"));
        context.resourceResolver().commit();

        final VersionCleanup vcs = new VersionCleanup(globalConfig, context.getService(ResourceResolverFactory.class));

        vcs.start();
        while (vcs.isRunning()) {
            TimeUnit.SECONDS.sleep(2);
        }

        assertFalse(vcs.isFailed());
        assertFalse(vcs.isRunning());
        assertNull(vcs.getLastMessage());
        assertEquals(7L, vcs.getLastCleanedVersionsCount());
    }

}