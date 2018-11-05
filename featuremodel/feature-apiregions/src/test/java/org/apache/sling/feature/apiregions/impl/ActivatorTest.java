/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.feature.apiregions.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

import static org.apache.sling.feature.apiregions.impl.RegionEnforcer.FEATURE_REGION_FILENAME;
import static org.apache.sling.feature.apiregions.impl.RegionEnforcer.PROPERTIES_FILE_PREFIX;

public class ActivatorTest {
    private Properties savedProps;

    @Before
    public void setup() {
        savedProps = new Properties(); // note that new Properties(props) doesn't copy
        savedProps.putAll(System.getProperties());
    }

    @After
    public void teardown() {
        System.setProperties(savedProps);
        savedProps = null;
    }

    @Test
    public void testStart() throws Exception {
        String f = getClass().getResource("/features1.properties").getFile();
        System.setProperty(PROPERTIES_FILE_PREFIX + FEATURE_REGION_FILENAME, f);

        Dictionary<String, Object> expectedProps = new Hashtable<>();
        expectedProps.put(FEATURE_REGION_FILENAME, f);

        BundleContext bc = Mockito.mock(BundleContext.class);

        Activator a = new Activator();
        a.start(bc);

        Mockito.verify(bc, Mockito.times(1)).registerService(
                Mockito.eq(ResolverHookFactory.class),
                Mockito.isA(RegionEnforcer.class),
                Mockito.eq(expectedProps));
    }
}
