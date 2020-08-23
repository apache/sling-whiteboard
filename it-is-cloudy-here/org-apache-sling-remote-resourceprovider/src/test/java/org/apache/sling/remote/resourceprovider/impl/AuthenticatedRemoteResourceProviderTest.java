/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Licensed to the Apache Software Foundation (ASF) under one
 ~ or more contributor license agreements.  See the NOTICE file
 ~ distributed with this work for additional information
 ~ regarding copyright ownership.  The ASF licenses this file
 ~ to you under the Apache License, Version 2.0 (the
 ~ "License"); you may not use this file except in compliance
 ~ with the License.  You may obtain a copy of the License at
 ~
 ~   http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package org.apache.sling.remote.resourceprovider.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.contentparser.api.ContentParser;
import org.apache.sling.contentparser.json.internal.JSONContentParser;
import org.apache.sling.remote.resourceprovider.RemoteStorageProvider;
import org.apache.sling.remote.resourceprovider.impl.mocks.MockRemoteStorageProvider;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContext;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(OsgiContextExtension.class)
@ExtendWith(MockitoExtension.class)
class AuthenticatedRemoteResourceProviderTest extends RemoteResourceProviderTestBase {

    private final OsgiContext context = new OsgiContext();

    private @Mock(lenient = true) ResolveContext alice;
    private @Mock(lenient = true) ResolveContext bob;

    private static final Map<String, Set<String>> whitelistedFiles = new HashMap<>();

    private static final String ALICE = "alice";
    private static final String BOB = "bob";
    private static final Map<String, Object> aliceAuthenticationInfo;
    private static final Map<String, Object> bobAuthenticationInfo;

    static {
        whitelistedFiles.put("src/test/resources/content", Set.of(ALICE, BOB));
        whitelistedFiles.put("src/test/resources/content/test-1", Set.of(ALICE, BOB));
        whitelistedFiles.put("src/test/resources/content/test-1/.sling.json", Set.of(BOB));
        whitelistedFiles.put("src/test/resources/content/test-1/test-1.txt", Set.of(ALICE, BOB));

        aliceAuthenticationInfo = new HashMap<>();
        aliceAuthenticationInfo.put(ResourceResolverFactory.USER, ALICE);
        bobAuthenticationInfo = new HashMap<>();
        bobAuthenticationInfo.put(ResourceResolverFactory.USER, BOB);
    }

    @SuppressWarnings("unchecked")
    @BeforeEach
    void beforeEach() {
        when(alice.getProviderState()).thenReturn(new RemoteResourceProviderContext(aliceAuthenticationInfo));
        when(bob.getProviderState()).thenReturn(new RemoteResourceProviderContext(bobAuthenticationInfo));
        context.registerService(ThreadPoolManager.class, mock(ThreadPoolManager.class));
        context.registerService(ContentParser.class, new JSONContentParser());
        context.registerInjectActivateService(new RemoteResourceProviderFactory());
        Hashtable<String, Object> resourceProviderRegistrationProperties = new Hashtable<>();
        resourceProviderRegistrationProperties.put(RemoteStorageProvider.PROP_RESOURCE_PROVIDER_ROOT, "/");
        resourceProviderRegistrationProperties.put(RemoteStorageProvider.PROP_RESOURCE_PROVIDER_AUTHENTICATE, "lazy");
        resourceProviderRegistrationProperties.put(ResourceProvider.PROPERTY_NAME, MockRemoteStorageProvider.class.getName());
        context.registerService(RemoteStorageProvider.class, new MockRemoteStorageProvider(whitelistedFiles), resourceProviderRegistrationProperties);
        resourceProvider = context.getService(ResourceProvider.class);
        assertNotNull(resourceProvider);
    }

    @Test
    void testDirectoryReadingAndCacheRetrieval() {
        Resource content = resourceProvider.getResource(resolveContext, "/content", resourceContext, null);
        assertNull(content, "Did not expect to be able to access /content without any authentication info.");

        content = resourceProvider.getResource(alice, "/content", resourceContext, null);
        assertNotNull(content, "Expected to be able to access /content for user alice.");

        Resource test_1_alice = resourceProvider.getResource(alice, "/content/test-1", resourceContext, content);
        assertNotNull(test_1_alice);

        Resource test_1_cached = getChild(alice, content, "test-1");
        assertTrue(wrapsSameResource(test_1_alice, test_1_cached), "Expected to get a cached representation of /content/test-1.");

        Resource test_1_bob = resourceProvider.getResource(bob, "/content/test-1", resourceContext, null);
        assertTrue(wrapsSameResource(test_1_bob, test_1_cached), "Expected to get a cached representation of /content/test-1.");

        checkChildren(alice, test_1_alice, Set.of("test-1.txt"));
        checkChildren(bob, test_1_bob, Set.of("hey", "little", "test-1.txt"));
    }

    @Test
    void testResourceReadingFromMetaFile() {
        Resource hey_bob = resourceProvider.getResource(bob, "/content/test-1/hey", resourceContext, null);
        assertNotNull(hey_bob, "Expected to find /content/test-1/hey for user bob.");
        assertEquals(ServletResolverConstants.DEFAULT_RESOURCE_TYPE, hey_bob.getResourceType(),
                () -> "Expected that /content/test-1/hey's resource type is " + ServletResolverConstants.DEFAULT_RESOURCE_TYPE);
        ValueMap heyProperties = hey_bob.getValueMap();
        assertEquals(1, heyProperties.size());
        assertEquals(ServletResolverConstants.DEFAULT_RESOURCE_TYPE, heyProperties.get(ResourceResolver.PROPERTY_RESOURCE_TYPE,
                String.class));
        assertEquals(ServletResolverConstants.DEFAULT_RESOURCE_TYPE, hey_bob.getResourceType(),
                () -> "Expected that /content/test-1/hey's resource type is " + ServletResolverConstants.DEFAULT_RESOURCE_TYPE);

        Resource joe_bob = resourceProvider.getResource(bob, "/content/test-1/hey/joe", resourceContext, null);
        assertNotNull(joe_bob, "Expected to find /content/test-1/hey/joe");
        assertEquals(ServletResolverConstants.DEFAULT_RESOURCE_TYPE, joe_bob.getResourceType(),
                () -> "Expected that /content/test-1/hey/joe's resource type is " + ServletResolverConstants.DEFAULT_RESOURCE_TYPE);

        ValueMap joeProperties = joe_bob.getValueMap();
        assertEquals(2, joeProperties.size());
        assertEquals(ServletResolverConstants.DEFAULT_RESOURCE_TYPE, joeProperties.get(ResourceResolver.PROPERTY_RESOURCE_TYPE,
                String.class));
        Calendar joelastModified = joeProperties.get("lastModifiedDate", Calendar.class);
        assertNotNull(joelastModified, String.format("Expected a lastModifiedDate property on %s", joe_bob.getPath()));
        assertEquals(1407421980000L, joelastModified.getTimeInMillis());

        assertTrue(wrapsSameResource(joe_bob, getChild(bob, hey_bob, "joe")), "Expected to get a cached " +
                "representation of /content/test-1/hey/joe");

        Iterator<Resource> joeChildren = resourceProvider.listChildren(bob, joe_bob);
        assertNull(joeChildren, String.format("Did not expect children resources for %s", joe_bob.getPath()));

        Resource hey_alice = resourceProvider.getResource(alice, "/content/test-1/hey", resourceContext, null);
        assertNull(hey_alice, "Did not expect user alice to have access to /content/test-1/hey.");
    }

    @Test
    void testFileAccess() throws IOException {
        Resource test_1_txt_alice = resourceProvider.getResource(alice, "/content/test-1/test-1.txt", resourceContext, null);
        assertNotNull(test_1_txt_alice);

        Resource test_1_txt_bob = resourceProvider.getResource(bob, "/content/test-1/test-1.txt", resourceContext, null);
        assertNotNull(test_1_txt_bob);

        assertTrue(wrapsSameResource(test_1_txt_alice, test_1_txt_bob), "Expected to get a cached representation of " +
                "/content/test-1/test-1.txt");

        assertEquals("nt:file", test_1_txt_alice.getResourceType());
        InputStream inputStream = test_1_txt_alice.adaptTo(InputStream.class);
        assertNotNull(inputStream, "Expected to be able to retrieve an InputStream from a Resource identifying a binary file.");
        assertEquals("A simple text file\n", IOUtils.toString(inputStream));
    }




}
