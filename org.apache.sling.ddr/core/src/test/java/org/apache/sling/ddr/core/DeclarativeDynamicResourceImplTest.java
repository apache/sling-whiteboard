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

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.ddr.api.DeclarativeDynamicResource;
import org.apache.sling.testing.resourceresolver.MockResource;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DeclarativeDynamicResourceImplTest {

    private ResourceResolverFactory resourceResolverFactory;
    private ResourceResolver resourceResolver;

    @Before
    public void setup() throws LoginException {
        resourceResolverFactory = new MockResourceResolverFactory();
        resourceResolver = resourceResolverFactory.getServiceResourceResolver(null);
    }

    @Test
    public void testBasics() throws Exception {
        final String resourceType = "test/conf";
        final String resourceSuperType = "test/static";
        String confResourceRoot = "/conf/test/settings/dynamic";
        String dynamicResourceRoot = "/apps/dynamic";
        String resourceName = "test1";
        final String testPropertyKey = "jcr:title";
        final String testPropertyValue = "Test-1";
        Resource source = new MockResource(
            confResourceRoot + "/" + resourceName,
            new HashMap<String, Object>() {{
                put("sling:resourceType", resourceType);
                put("sling:resourceSuperType", resourceSuperType);
                put(testPropertyKey, testPropertyValue);
            }},
            resourceResolver
        );
        DeclarativeDynamicResource declarativeDynamicResource = DeclarativeDynamicResourceImpl.createSyntheticFromResource(
            source, dynamicResourceRoot + "/" + resourceName, true
        );
        assertEquals("Wrong DD-Resource Name", resourceName, declarativeDynamicResource.getName());
        assertEquals("Wrong Resource Type", resourceType, declarativeDynamicResource.getResourceType());
        assertEquals("Wrong Resource Super Type", resourceSuperType, declarativeDynamicResource.getResourceSuperType());
        assertEquals("Wrong Resource Name", resourceName, declarativeDynamicResource.getName());
        assertEquals("Wrong Resource Path", dynamicResourceRoot + "/" + resourceName, declarativeDynamicResource.getPath());
        ValueMap properties = declarativeDynamicResource.getValueMap();
        assertNotNull("Missing Value Map", properties);
        assertNotNull("Missing Title Property", properties.get(testPropertyKey, String.class));
        assertEquals("Wrong Title Property", testPropertyValue, properties.get(testPropertyKey, String.class));
    }
}
