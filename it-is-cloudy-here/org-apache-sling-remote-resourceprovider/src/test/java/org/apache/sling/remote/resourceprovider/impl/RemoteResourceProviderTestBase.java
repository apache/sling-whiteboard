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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.apache.sling.testing.mock.osgi.junit5.OsgiContext;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

abstract class RemoteResourceProviderTestBase {

    protected final OsgiContext context = new OsgiContext();

    protected @Mock ResolveContext<RemoteResourceProviderContext> resolveContext;
    protected @Mock ResourceContext resourceContext;
    protected ResourceProvider<RemoteResourceProviderContext> resourceProvider;

    protected Resource getChild(Resource parent, String childName) {
        return getChild(resolveContext, parent, childName);
    }

    protected Resource getChild(ResolveContext<RemoteResourceProviderContext> resolveContext, Resource parent, String childName) {
        Iterator<Resource> children = resourceProvider.listChildren(resolveContext, parent);
        if (children != null) {
            while (children.hasNext()) {
                Resource child = children.next();
                if (childName.equals(child.getName())) {
                    return child;
                }
            }
        }
        return null;
    }

    protected void checkChildren(Resource resource, Set<String> childrenNames) {
        checkChildren(resolveContext, resource, childrenNames);
    }

    protected void checkChildren(ResolveContext<RemoteResourceProviderContext> resolveContext, Resource resource, Set<String> childrenNames) {
        Set<String> expectedChildren = new HashSet<>(childrenNames);
        int childrenNumber = expectedChildren.size();
        AtomicInteger iterations = new AtomicInteger(0);
        Iterator<Resource> children = resourceProvider.listChildren(resolveContext, resource);
        assertNotNull(children);
        children.forEachRemaining(child -> {
            expectedChildren.remove(child.getName());
            iterations.getAndIncrement();
        });
        assertAll("children",
                () -> assertTrue(expectedChildren.isEmpty(),
                        () -> String.format("The following children have not been found: %s.", String.join(", ", expectedChildren))),
                () -> assertEquals(childrenNumber, iterations.get(),
                        String.format(
                                "Expected to find %d " + (childrenNumber == 1 ? "child" : "children") + ". Got %d instead.",
                                childrenNumber,
                                iterations.get()
                        )
                )
        );
    }

    boolean wrapsSameResource(Resource a, Resource b) {
        if (!(a instanceof ResourceWrapper) || !(b instanceof ResourceWrapper)) {
            return false;
        }
        ResourceWrapper wrapper1 = (ResourceWrapper) a;
        ResourceWrapper wrapper2 = (ResourceWrapper) b;
        return wrapper1.getResource().equals(wrapper2.getResource());
    }
}
