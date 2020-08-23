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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.contentparser.api.ContentParser;
import org.apache.sling.contentparser.json.internal.JSONContentParser;
import org.apache.sling.remote.resourceprovider.RemoteStorageProvider;
import org.apache.sling.remote.resourceprovider.impl.mocks.MockRemoteStorageProvider;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(OsgiContextExtension.class)
@ExtendWith(MockitoExtension.class)
class UnauthenticatedRemoteResourceProviderTest extends RemoteResourceProviderTestBase {

    @SuppressWarnings("unchecked")
    @BeforeEach
    void beforeEach() {
        context.registerService(ThreadPoolManager.class, mock(ThreadPoolManager.class));
        context.registerService(ContentParser.class, new JSONContentParser());
        context.registerInjectActivateService(new RemoteResourceProviderFactory());
        Hashtable<String, Object> resourceProviderRegistrationProperties = new Hashtable<>();
        resourceProviderRegistrationProperties.put(RemoteStorageProvider.PROP_RESOURCE_PROVIDER_ROOT, "/");
        resourceProviderRegistrationProperties.put(RemoteStorageProvider.PROP_RESOURCE_PROVIDER_AUTHENTICATE, "no");
        resourceProviderRegistrationProperties.put(ResourceProvider.PROPERTY_NAME, MockRemoteStorageProvider.class.getName());
        context.registerService(RemoteStorageProvider.class, new MockRemoteStorageProvider(), resourceProviderRegistrationProperties);
        resourceProvider = context.getService(ResourceProvider.class);
        assertNotNull(resourceProvider);
    }

    @Test
    void testDirectoryReadingAndCacheRetrieval() {
        Resource content = resourceProvider.getResource(resolveContext, "/content", resourceContext, null);
        assertNotNull(content);

        Resource test_1 = resourceProvider.getResource(resolveContext, "/content/test-1", resourceContext, content);
        assertNotNull(test_1);

        Resource test_1_cached = getChild(content, "test-1");
        assertTrue(wrapsSameResource(test_1, test_1_cached), "Expected to get a cached representation of /content/test-1");

        Set<String> expected_test_1_children = new HashSet<>(Set.of("hey", "little", "test-1.txt"));
        AtomicInteger iterations = new AtomicInteger(0);
        Iterator<Resource> test_1_children = resourceProvider.listChildren(resolveContext, test_1);
        assertNotNull(test_1_children);
        test_1_children.forEachRemaining(resource -> {
            expected_test_1_children.remove(resource.getName());
            iterations.getAndIncrement();
        });
        assertAll("children",
                () -> assertTrue(expected_test_1_children.isEmpty()),
                () -> assertEquals(3, iterations.get())
        );
    }

    @Test
    void testResourceReadingFromMetaFile() {
        Resource hey = resourceProvider.getResource(resolveContext, "/content/test-1/hey", resourceContext, null);
        assertNotNull(hey, "Expected to find /content/test-1/hey");
        assertEquals(ServletResolverConstants.DEFAULT_RESOURCE_TYPE, hey.getResourceType(),
                "Expected that /content/test-1/hey's resource type is " + ServletResolverConstants.DEFAULT_RESOURCE_TYPE);
        ValueMap heyProperties = hey.getValueMap();
        assertEquals(1, heyProperties.size());
        assertEquals(ServletResolverConstants.DEFAULT_RESOURCE_TYPE, heyProperties.get(ResourceResolver.PROPERTY_RESOURCE_TYPE,
                String.class));
        assertEquals(ServletResolverConstants.DEFAULT_RESOURCE_TYPE, hey.getResourceType(),
                "Expected that /content/test-1/hey's resource type is " + ServletResolverConstants.DEFAULT_RESOURCE_TYPE);

        Resource joe = resourceProvider.getResource(resolveContext, "/content/test-1/hey/joe", resourceContext, null);
        assertNotNull(joe, "Expected to find /content/test-1/hey/joe");
        assertEquals(ServletResolverConstants.DEFAULT_RESOURCE_TYPE, joe.getResourceType(),
                "Expected that /content/test-1/hey/joe's resource type is " + ServletResolverConstants.DEFAULT_RESOURCE_TYPE);

        ValueMap joeProperties = joe.getValueMap();
        assertEquals(2, joeProperties.size());
        assertEquals(ServletResolverConstants.DEFAULT_RESOURCE_TYPE, joeProperties.get(ResourceResolver.PROPERTY_RESOURCE_TYPE,
                String.class));
        Calendar lastModified = joeProperties.get("lastModifiedDate", Calendar.class);
        assertNotNull(lastModified, String.format("Expected a lastModifiedDate property on %s", joe.getPath()));
        assertEquals(1407421980000L, lastModified.getTimeInMillis());
        assertTrue(wrapsSameResource(joe, getChild(hey, "joe")), "Expected to get a cached representation of /content/test-1/hey/joe");

        Iterator<Resource> joeChildren = resourceProvider.listChildren(resolveContext, joe);
        assertNull(joeChildren, String.format("Did not expect children resources for %s", joe.getPath()));
    }

    @Test
    void testFileAccess() throws IOException {
        Resource test_1_txt = resourceProvider.getResource(resolveContext, "/content/test-1/test-1.txt", resourceContext, null);
        assertNotNull(test_1_txt, "Expected to find resource /content/test-1/test-1.txt.");
        assertEquals("nt:file", test_1_txt.getResourceType());
        InputStream inputStream = test_1_txt.adaptTo(InputStream.class);
        assertNotNull(inputStream, "Expected to be able to retrieve an InputStream from a Resource identifying a binary file.");
        assertEquals("A simple text file\n", IOUtils.toString(inputStream));
    }
}
