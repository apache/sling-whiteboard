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
import org.apache.sling.ddr.api.DeclarativeDynamicResourceListener;
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
import javax.jcr.query.Query;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import static org.apache.sling.ddr.api.Constants.DDR_NODE_TYPE;
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
        String resourceName = "test1";
        String confResourceRoot = "/conf/test/settings/dynamic";
        String dynamicResourceRoot = "/apps/dynamic";
        final String testPropertyKey = "jcr:title";
        final String testPropertyValue = "Test-1";

        log.info("Before Loading Test Resources");
        context.load().json("/ddr-sources/ddr-conf-settings.json", "/conf");
        context.load().json("/ddr-sources/ddr-apps-settings.json", "/apps");
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

//    @Test
//    public void testFilters() throws Exception {
//        String resourceName = "test1";
//        String confResourceRoot = "/conf/testFilter/settings/dynamic";
//        String dynamicResourceRoot = "/apps/dynamicFilter";
////        final String testPropertyKey = "jcr:title";
////        final String testPropertyValue = "Test-1";
//
//        log.info("Before Loading Test Resources");
//        context.load().json("/ddr-filter/ddr-conf-settings.json", "/conf");
//        context.load().json("/ddr-filter/ddr-apps-settings.json", "/apps");
//        log.info("After Loading Test Resources");
//        Resource sourceRoot = resourceResolver.getResource("/conf/testFilter/settings/dynamic");
//        log.info("Dynamic Test Resource Root: '{}'", sourceRoot);
//        when(resourceResolver.findResources(anyString(), anyString())).thenReturn(
//            Arrays.asList(sourceRoot).iterator()
//        );
//
//        // Listen to newly created DDRs
//        doAnswer(new ListenerAnswer()).when(declarativeDynamicResourceListener).addDeclarativeDynamicResource(anyString(), any(Resource.class));
//
//        // Test a basic, already installed configuration
//        log.info("DDR-Manager Service: '{}'", declarativeDynamicResourceManagerService);
//        declarativeDynamicResourceManagerService.activate(context.bundleContext(), createConfiguration(
//            null, new String[] {"sling:resourceType=nt:file", "sling:resourceType=nt:resource"}));
//
//// The Query for Node Type sling:DDR will not return even though a Resource is there
//        assertFalse("No DDR Registered", ddrMap.isEmpty());
//        assertEquals("Only one DDR should be Registered", 1, ddrMap.size());
//        Entry<String, Resource> entry = ddrMap.entrySet().iterator().next();
//        assertEquals("Wrong DDR Dynamic Path", dynamicResourceRoot + "/" + resourceName, entry.getKey());
//        Resource source = entry.getValue();
//        assertEquals("Wrong DDR Source Path", confResourceRoot + "/" + resourceName, source.getPath());
//    }

    private DeclarativeDynamicResourceManagerService.Configuration createConfiguration(String[] allowed, String[] prohibited) {
        final DeclarativeDynamicResourceManagerService.Configuration configuration = mock(DeclarativeDynamicResourceManagerService.Configuration.class);
        when(configuration.allowed_ddr_filter()).thenReturn(allowed == null ? new String[] {}: allowed);
        when(configuration.prohibited_ddr_filter()).thenReturn(prohibited == null ? new String[] {}: prohibited);
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
