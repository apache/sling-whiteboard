/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.ddr.core;

import com.google.common.collect.ImmutableList;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.apache.sling.testing.mock.sling.NodeTypeDefinitionScanner;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class DeclarativeDynamicResourceProviderHandlerTest {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    @Mock
    private ResolveContext resolveContext;
    @Mock
    private ResourceContext resourceContext;

    @Mock
    private ResourceResolverFactory resourceResolverFactory;
    private ResourceResolver resourceResolver;

    private DeclarativeDynamicResourceProviderHandler declarativeDynamicResourceProviderHandler =
        new DeclarativeDynamicResourceProviderHandler();

    @Before
    public void setup() throws LoginException, RepositoryException {
        resourceResolver = spy(context.resourceResolver());
        log.info("Adapt Context-RR: '{}' to Session: '{}'", resourceResolver, resourceResolver.adaptTo(Session.class));
        when(resourceResolverFactory.getServiceResourceResolver(any(Map.class)))
            .thenReturn(resourceResolver);
        when(resolveContext.getResourceResolver()).thenReturn(resourceResolver);

        try {
            NodeTypeDefinitionScanner.get().register(context.resourceResolver().adaptTo(Session.class),
                ImmutableList.of("SLING-CONTENT/nodetypes/ddr.cnd"),
                ResourceResolverType.JCR_OAK.getNodeTypeMode());
        }
        catch (RepositoryException ex) {
            throw new RuntimeException("Unable to register namespaces.", ex);
        }
    }

    @Test
    public void testGetResource() throws Exception {
        String resourceName1 = "test1";
        String resourceName2 = "test2";
        String resourceName3 = "test3";
        String confResourceRoot = "/conf/testFilter/settings/dynamic";
        String dynamicResourceRoot = "/apps/dynamicFilter";
        final String testPropertyKey = "jcr:title";
        final String testPropertyValue = "Test-1";

        log.info("Before Loading Test Resources");
        context.load().json("/ddr-filter/ddr-conf-settings.json", "/conf");
        context.load().json("/ddr-filter/ddr-apps-settings.json", "/apps");
        log.info("After Loading Test Resources");

        Resource dynamicParent = resourceResolver.getResource(dynamicResourceRoot);
        declarativeDynamicResourceProviderHandler.registerService(
            context.bundleContext().getBundle(), dynamicResourceRoot, confResourceRoot,
            resourceResolverFactory, null,
            new HashMap<String, List<String>>() {{
                put("jcr:primaryType", Arrays.asList("nt:file"));
            }},
            null
        );

        Resource test1 = declarativeDynamicResourceProviderHandler.getResource(
            resolveContext, dynamicResourceRoot + "/" + resourceName1, resourceContext, dynamicParent
        );
        // The Query for Node Type sling:DDR will not return even though a Resource is there
        assertNotNull("Test1 DDR not found", test1);
        assertEquals("Wrong DDR Dynamic Path", dynamicResourceRoot + "/" + resourceName1, test1.getPath());
        ValueMap properties = test1.getValueMap();
        String title = properties.get(testPropertyKey, String.class);
        assertNotNull("Title Property not found", title);
        assertEquals("Title Property wrong", testPropertyValue, title);
        Resource test2 = declarativeDynamicResourceProviderHandler.getResource(
            resolveContext, dynamicResourceRoot + "/" + resourceName2, resourceContext, dynamicParent
        );
        assertNull("Test2 should not be returned", test2);
        Resource test3 = declarativeDynamicResourceProviderHandler.getResource(
            resolveContext, dynamicResourceRoot + "/" + resourceName3, resourceContext, dynamicParent
        );
        assertNotNull("Test1 DDR not found", test3);
        assertEquals("Wrong DDR Dynamic Path", dynamicResourceRoot + "/" + resourceName3, test3.getPath());
    }

    @Test
    public void testListChildren() throws Exception {
        String resourceName = "test1";
        String confResourceRoot = "/conf/testFilter/settings/dynamic";
        String dynamicResourceRoot = "/apps/dynamicFilter";
        final String testPropertyKey = "jcr:title";
        final String testPropertyValue = "Test-1";

        log.info("Before Loading Test Resources");
        context.load().json("/ddr-filter/ddr-conf-settings.json", "/conf");
        context.load().json("/ddr-filter/ddr-apps-settings.json", "/apps");
        log.info("After Loading Test Resources");

        Resource dynamicParent = resourceResolver.getResource(dynamicResourceRoot);

        declarativeDynamicResourceProviderHandler.registerService(
            context.bundleContext().getBundle(), dynamicResourceRoot, confResourceRoot,
            resourceResolverFactory, null,
            new HashMap<String, List<String>>() {{
                put("jcr:primaryType", Arrays.asList("nt:file", "nt:resource"));
            }},
            null
        );

        // List all the children and make sure that only one is returned
        Iterator<Resource> i = declarativeDynamicResourceProviderHandler.listChildren(
            resolveContext, dynamicParent
        );
        List<Resource> children = new ArrayList<>();
        while(i.hasNext()) {
            children.add(i.next());
        }

        // The Query for Node Type sling:DDR will not return even though a Resource is there
        assertFalse("No DDR Registered", children.isEmpty());
        assertEquals("Only one DDR should be Registered", 1, children.size());
        Resource child = children.get(0);
        assertEquals("Wrong DDR Dynamic Path", dynamicResourceRoot + "/" + resourceName, child.getPath());
        ValueMap properties = child.getValueMap();
        String title = properties.get(testPropertyKey, String.class);
        assertNotNull("Title Property not found", title);
        assertEquals("Title Property wrong", testPropertyValue, title);
    }

    @Test
    public void testListReferences() throws Exception {
        String resourceName = "test1";
        String confResourceRoot = "/conf/testReference/settings/dynamic";
        String dynamicResourceRoot = "/apps/dynamicReference";
        final String testPropertyKey = "jcr:title";
        final String testPropertyValue = "Test-1";

        context.load().json("/ddr-reference/ddr-conf-settings.json", "/conf");
        context.load().json("/ddr-reference/ddr-apps-settings.json", "/apps");

        Resource dynamicParent = resourceResolver.getResource(dynamicResourceRoot);

        declarativeDynamicResourceProviderHandler.registerService(
            context.bundleContext().getBundle(), dynamicResourceRoot, confResourceRoot,
            resourceResolverFactory, null, null,
            Arrays.asList("sling:ddrRef")
        );

        Resource noRef = checkAndGetResource(dynamicParent, "noRef");
        Resource refNoChild = checkAndGetResource(dynamicParent, "refNoChild");
        Resource refWithChild = checkAndGetResource(dynamicParent, "refWithChild");
        Resource refChild = checkAndGetResource(refWithChild, "child");
        log.info("Ref Child: '{}'", refChild);
        Resource refGrandchild = checkAndGetResource(refChild, "grandChild");
        log.info("Ref Grandchild: '{}'", refGrandchild);
    }

    private Resource checkAndGetResource(Resource parent, String expectedChildName) {
        Resource answer = null;
        // List all the children and make sure that only one is returned
        Iterator<Resource> i = declarativeDynamicResourceProviderHandler.listChildren(
            resolveContext, parent
        );
        assertNotNull("No Iterator returned", i);
        while(i.hasNext()) {
            Resource child = i.next();
            if(child.getName().equals(expectedChildName)) {
                answer = child;
                break;
            }
        }
        assertNotNull("Child: '" + expectedChildName + "' not found", answer);
        return answer;
    }
}
