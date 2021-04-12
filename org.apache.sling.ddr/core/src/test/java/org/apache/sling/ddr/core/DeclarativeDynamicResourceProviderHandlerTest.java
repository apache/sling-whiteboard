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
import org.apache.sling.ddr.api.DeclarativeDynamicResourceManager;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.apache.sling.ddr.api.Constants.SLASH;
import static org.apache.sling.ddr.core.TestUtils.filterResourceByName;
import static org.apache.sling.ddr.core.TestUtils.getResourcesFromProvider;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
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
    private DeclarativeDynamicResourceManager declarativeDynamicResourceManager;

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
            resourceResolver, null, null,
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
            resourceResolver, null, null,
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
        String confResourceRoot = "/conf/testReference/settings/dynamic";
        String dynamicResourceRoot = "/apps/dynamicReference";

        context.load().json("/ddr-reference/ddr-conf-settings.json", "/conf");
        context.load().json("/ddr-reference/ddr-apps-settings.json", "/apps");

        Resource dynamicParent = resourceResolver.getResource(dynamicResourceRoot);

        doNothing().when(declarativeDynamicResourceManager).addReference(anyString(), anyString());

        declarativeDynamicResourceProviderHandler.registerService(
            context.bundleContext().getBundle(), dynamicResourceRoot, confResourceRoot,
            resourceResolver, declarativeDynamicResourceManager,null, null,
            Arrays.asList("sling:ddrRef")
        );

        Resource noRef = checkAndGetResource(dynamicParent, "noRef");
        Resource refNoChild = checkAndGetResource(dynamicParent, "refNoChild");
        Resource refWithChild = checkAndGetResource(dynamicParent, "refWithChild");
        Resource referencedChild = checkAndGetResource(refWithChild, "child");
        log.info("Ref Child: '{}'", referencedChild);
        Resource refWithGrandChild = checkAndGetResource(dynamicParent, "refWithGrandChild");
        log.info("Ref Grand Child: '{}'", refWithGrandChild);
        Resource referencedChild2 = checkAndGetResource(refWithGrandChild, "child");
        log.info("Ref Child: '{}'", referencedChild);
        Resource refGrandchild = checkAndGetResource(referencedChild2, "grandChild");
        log.info("Ref Grandchild: '{}'", refGrandchild);
    }

    @Test
    public void testListChildrenWithFailingContextResourceResolver() throws Exception {
        String confResourceRoot = "/conf/testReference/settings/dynamic";
        String dynamicResourceRoot = "/apps/dynamicReference";

        context.load().json("/ddr-reference/ddr-conf-settings.json", "/conf");
        context.load().json("/ddr-reference/ddr-apps-settings.json", "/apps");

        ResourceResolver contextResourceResolver = mock(ResourceResolver.class);
        when(contextResourceResolver.getResource(anyString())).thenReturn(null);
        when(resolveContext.getResourceResolver()).thenReturn(contextResourceResolver);

        Resource dynamicParent = resourceResolver.getResource(dynamicResourceRoot);

        doNothing().when(declarativeDynamicResourceManager).addReference(anyString(), anyString());

        declarativeDynamicResourceProviderHandler.registerService(
            context.bundleContext().getBundle(), dynamicResourceRoot, confResourceRoot,
            resourceResolver, declarativeDynamicResourceManager,null, null,
            Arrays.asList("sling:ddrRef")
        );

        Resource noRef = checkAndGetResource(dynamicParent, "noRef");
    }

    @Test
    public void testUpdates() throws Exception {
        String resourceName1 = "test1";
        String newResourceName1 = "test1a";
        String confResourceRoot = "/conf/testFilter/settings/dynamic";
        String dynamicResourceRoot = "/apps/dynamicFilter";

        log.info("Before Loading Test Resources");
        context.load().json("/ddr-filter/ddr-conf-settings.json", "/conf");
        context.load().json("/ddr-filter/ddr-apps-settings.json", "/apps");
        log.info("After Loading Test Resources");

        Resource dynamicParent = resourceResolver.getResource(dynamicResourceRoot);
        declarativeDynamicResourceProviderHandler.registerService(
            context.bundleContext().getBundle(), dynamicResourceRoot, confResourceRoot,
            resourceResolver, null, null,
            new HashMap<String, List<String>>() {{
                put("jcr:primaryType", Arrays.asList("nt:file"));
            }},
            null
        );

        Resource test1 = declarativeDynamicResourceProviderHandler.getResource(
            resolveContext, dynamicResourceRoot + "/" + resourceName1, resourceContext, dynamicParent
        );

        Resource container = declarativeDynamicResourceProviderHandler.getResource(
            resolveContext, dynamicResourceRoot + "/container", resourceContext, dynamicParent
        );
        assertNotNull("Container Resource not found", container);

        Resource newTest1 = declarativeDynamicResourceProviderHandler.getResource(
            resolveContext, dynamicResourceRoot + "/container/" + resourceName1, resourceContext, dynamicParent
        );
        assertNull("To Be Moved Resource must not exist beforehand", newTest1);

        resourceResolver.move(
            confResourceRoot + SLASH + resourceName1, confResourceRoot + SLASH + "container"
        );

        // Check that the resource is not found in list
        List<Resource> resources = getResourcesFromProvider(declarativeDynamicResourceProviderHandler, resolveContext, container);
        resources = filterResourceByName(resources, true, resourceName1);
        assertTrue("Moved resource should not have been found here", resources.isEmpty());

        declarativeDynamicResourceProviderHandler.update(confResourceRoot + SLASH + newResourceName1);

        resources = getResourcesFromProvider(declarativeDynamicResourceProviderHandler, resolveContext, container);
        resources = filterResourceByName(resources, true, resourceName1);
        assertFalse("Moved resource was not found after update", resources.isEmpty());
        newTest1 = resources.get(0);
        assertNotNull("Moved Resource not found", newTest1);
    }

    private Resource checkAndGetResource(Resource parent, String expectedChildName) {
        List<Resource> resources = getResourcesFromProvider(declarativeDynamicResourceProviderHandler, resolveContext, parent);
        resources = filterResourceByName(resources, true, expectedChildName);
        Resource answer = resources.isEmpty() ? null : resources.get(0);
        assertNotNull("Child: '" + expectedChildName + "' not found", answer);
        return answer;
    }
}
