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
import org.apache.sling.ddr.api.DeclarativeDynamicResourceListener;
import org.apache.sling.ddr.api.DeclarativeDynamicResourceProvider;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.apache.sling.testing.mock.sling.NodeTypeDefinitionScanner;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.apache.sling.ddr.core.TestUtils.filterResourceByName;
import static org.apache.sling.ddr.core.TestUtils.getResourcesFromProvider;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class DeclarativeDynamicResourceManagerServiceTest {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    @Mock
    private ResolveContext resolveContext;
    @Mock
    private ResourceResolverFactory resourceResolverFactory;
    @Mock
    private DeclarativeDynamicResourceListener declarativeDynamicResourceListener;

    private DeclarativeDynamicResourceManagerService declarativeDynamicResourceManagerService = new DeclarativeDynamicResourceManagerService();

    private ResourceResolver resourceResolver;

    private final Map<String, Resource> ddrMap = new HashMap<>();

    @Before
    public void setup() throws LoginException, RepositoryException {
        resourceResolver = spy(context.resourceResolver());
        log.info("Adapt Context-RR: '{}' to Session: '{}'", resourceResolver, resourceResolver.adaptTo(Session.class));
        when(resolveContext.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolverFactory.getServiceResourceResolver(any(Map.class)))
            .thenReturn(resourceResolver);
        declarativeDynamicResourceManagerService.resourceResolverFactory = resourceResolverFactory;
        declarativeDynamicResourceManagerService.dynamicComponentFilterNotifier = declarativeDynamicResourceListener;

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
    public void testBasics() throws Exception {
        String confResourceRoot = "/conf/test/settings/dynamic";
        String dynamicResourceRoot = "/apps/dynamic";

        log.info("Before Loading Test Resources");
        context.load().json("/ddr-installation/ddr-conf-settings.json", "/conf");
        context.load().json("/ddr-installation/ddr-apps-settings.json", "/apps");
        log.info("After Loading Test Resources");
        Resource sourceRoot = resourceResolver.getResource("/conf/test/settings/dynamic");
        log.info("Dynamic Test Resource Root: '{}'", sourceRoot);
        when(resourceResolver.findResources(anyString(), anyString())).thenReturn(
            Arrays.asList(sourceRoot).iterator()
        );

        // Listen to newly created DDRs
        doAnswer(new ListenerAnswer()).when(declarativeDynamicResourceListener).addDeclarativeDynamicResource(anyString(), any(Resource.class));

        // Test a basic, already installed configuration
        log.info("DDR-Manager Service: '{}'", declarativeDynamicResourceManagerService);
        declarativeDynamicResourceManagerService.activate(context.bundleContext(), createConfiguration(null, null));

// The Query for Node Type sling:DDR will not return even though a Resource is there
        assertFalse("No DDR Registered", ddrMap.isEmpty());
        assertEquals("More than one DDR Registered", 1, ddrMap.size());
        Entry<String, Resource> entry = ddrMap.entrySet().iterator().next();
        assertEquals("Wrong DDR Dynamic Path", dynamicResourceRoot, entry.getKey());
        Resource source = entry.getValue();
        assertEquals("Wrong DDR Source Path", confResourceRoot, source.getPath());
    }

    @Test
    public void testResourceUpdates() throws Exception {
        String confResourceRoot = "/conf/test/settings/dynamic";
        String dynamicResourceRoot = "/apps/dynamic";

        log.info("Before Loading Test Resources");
        context.load().json("/ddr-installation/ddr-conf-settings.json", "/conf");
        context.load().json("/ddr-installation/ddr-apps-settings.json", "/apps");
        log.info("After Loading Test Resources");
        Resource sourceRoot = resourceResolver.getResource(confResourceRoot);
        Resource targetRoot = resourceResolver.getResource(dynamicResourceRoot);
        log.info("Dynamic Test Resource Root: '{}'", sourceRoot);
        when(resourceResolver.findResources(anyString(), anyString())).thenReturn(
            Arrays.asList(sourceRoot).iterator()
        );

        // Listen to newly created DDRs
        doAnswer(new ListenerAnswer()).when(declarativeDynamicResourceListener).addDeclarativeDynamicResource(anyString(), any(Resource.class));

        // Test a basic, already installed configuration
        log.info("DDR-Manager Service: '{}'", declarativeDynamicResourceManagerService);
        declarativeDynamicResourceManagerService.activate(context.bundleContext(), createConfiguration(null, null));

        // Get the children from the DDR
        Map<String, DeclarativeDynamicResourceProvider> providerMap = declarativeDynamicResourceManagerService.getRegisteredServicesByTarget();
        assertEquals("Expected only one DDR Provider", 1, providerMap.size());
        DeclarativeDynamicResourceProvider provider = providerMap.values().iterator().next();
        assertNotNull("DDR Provider must be defined", provider);

        List<Resource> resources = getResourcesFromProvider((ResourceProvider) provider, resolveContext, targetRoot);
        assertFalse("Expected a resource but none found", resources.isEmpty());
        assertEquals("Did not get 'test1' resource", 1, filterResourceByName(resources, true, "test1").size());
        assertEquals("Got another resource than 'test1'", new ArrayList<>(), filterResourceByName(resources, false, "test1"));

        // Move the resource in from the source
        resourceResolver.move("/apps/sources/test2", confResourceRoot);
        Thread.sleep(10000);

        resources = getResourcesFromProvider((ResourceProvider) provider, resolveContext, targetRoot);
        assertFalse("Expected resources but none found", resources.isEmpty());
        assertEquals("Did not get 'test1' resource", 1, filterResourceByName(resources, true, "test1").size());
//AS TODO: The move() of the resource is not triggering the onEvent() on the Resource Manager.
//        assertEquals("Did not get 'test2' resource", 1, filterResourceByName(resources, true, "test2").size());
//        assertEquals("Got another resource than 'test1'", new ArrayList<>(), filterResourceByName(resources, false, "test1"));
    }

    private DeclarativeDynamicResourceManagerService.Configuration createConfiguration(
        String[] allowed, String[] prohibited, String ... followedLinkNames
    ) {
        final DeclarativeDynamicResourceManagerService.Configuration configuration = mock(DeclarativeDynamicResourceManagerService.Configuration.class);
        when(configuration.allowed_ddr_filter()).thenReturn(allowed == null ? new String[] {}: allowed);
        when(configuration.prohibited_ddr_filter()).thenReturn(prohibited == null ? new String[] {}: prohibited);
        when(configuration.followed_link_names()).thenReturn(followedLinkNames == null ? new String[] {} : followedLinkNames);
        log.info("DDR-Manager Service: '{}'", declarativeDynamicResourceManagerService);
        return configuration;
    }

    private class ListenerAnswer implements Answer {
        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            String dynamicPath = invocation.getArgument(0);
            Resource source = invocation.getArgument(1);
            ddrMap.put(dynamicPath, source);
            return null;
        }
    }
}
