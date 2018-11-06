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
import org.osgi.framework.Version;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Properties;

import static org.apache.sling.feature.apiregions.impl.RegionEnforcer.BUNDLE_FEATURE_FILENAME;
import static org.apache.sling.feature.apiregions.impl.RegionEnforcer.FEATURE_REGION_FILENAME;
import static org.apache.sling.feature.apiregions.impl.RegionEnforcer.IDBSNVER_FILENAME;
import static org.apache.sling.feature.apiregions.impl.RegionEnforcer.PROPERTIES_FILE_PREFIX;
import static org.apache.sling.feature.apiregions.impl.RegionEnforcer.REGION_PACKAGE_FILENAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RegionEnforcerTest {
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
    public void testRegionEnforcerNoConfiguration() throws Exception {
        RegionEnforcer re = new RegionEnforcer(new Hashtable<>(), "*");
        assertEquals(0, re.bsnVerMap.size());
        assertEquals(0, re.bundleFeatureMap.size());
        assertEquals(0, re.featureRegionMap.size());
        assertEquals(0, re.regionPackageMap.size());
    }

    @Test
    public void testLoadBSNVerMap() throws Exception {
        String f = getClass().getResource("/idbsnver1.properties").getFile();
        System.setProperty(PROPERTIES_FILE_PREFIX + IDBSNVER_FILENAME, f);

        Hashtable<String, Object> props = new Hashtable<>();
        RegionEnforcer re = new RegionEnforcer(props, "*");
        assertEquals(2, re.bsnVerMap.size());
        assertEquals(Collections.singletonList("g:b1:1"),
                re.bsnVerMap.get(new AbstractMap.SimpleEntry<String,Version>("b1", new Version(1,0,0))));
        assertEquals(new HashSet<>(Arrays.asList("g:b2:1.2.3", "g2:b2:1.2.4")),
                new HashSet<>(re.bsnVerMap.get(new AbstractMap.SimpleEntry<String,Version>("b2", new Version(1,2,3)))));
        assertEquals(f, props.get(IDBSNVER_FILENAME));
    }

    @Test
    public void testLoadBundleFeatureMap() throws Exception {
        String f = getClass().getResource("/bundles1.properties").getFile();
        System.setProperty(PROPERTIES_FILE_PREFIX + BUNDLE_FEATURE_FILENAME, f);

        Hashtable<String, Object> props = new Hashtable<>();
        RegionEnforcer re = new RegionEnforcer(props, "*");
        assertEquals(3, re.bundleFeatureMap.size());
        assertEquals(Collections.singleton("org.sling:something:1.2.3:slingosgifeature:myclassifier"),
                re.bundleFeatureMap.get("org.sling:b1:1"));
        assertEquals(Collections.singleton("org.sling:something:1.2.3:slingosgifeature:myclassifier"),
                re.bundleFeatureMap.get("org.sling:b2:1"));
        assertEquals(new HashSet<>(Arrays.asList("some.other:feature:123", "org.sling:something:1.2.3:slingosgifeature:myclassifier")),
                re.bundleFeatureMap.get("org.sling:b3:1"));
        assertEquals(f, props.get(BUNDLE_FEATURE_FILENAME));
    }

    @Test
    public void testLoadFeatureRegionMap() throws Exception {
        String f = getClass().getResource("/features1.properties").getFile();
        System.setProperty(PROPERTIES_FILE_PREFIX + FEATURE_REGION_FILENAME, f);

        Hashtable<String, Object> props = new Hashtable<>();
        RegionEnforcer re = new RegionEnforcer(props, "*");
        assertEquals(2, re.featureRegionMap.size());
        assertEquals(Collections.singleton("global"),
                re.featureRegionMap.get("an.other:feature:123"));
        assertEquals(new HashSet<>(Arrays.asList("global", "internal")),
                re.featureRegionMap.get("org.sling:something:1.2.3"));
        assertEquals(f, props.get(FEATURE_REGION_FILENAME));
    }

    @Test
    public void testLoadRegionPackageMap() throws Exception {
        String f = getClass().getResource("/regions1.properties").getFile();
        System.setProperty(PROPERTIES_FILE_PREFIX + REGION_PACKAGE_FILENAME, f);

        Hashtable<String, Object> props = new Hashtable<>();
        RegionEnforcer re = new RegionEnforcer(props, "*");
        assertEquals(2, re.regionPackageMap.size());
        assertEquals(Collections.singleton("xyz"),
                re.regionPackageMap.get("internal"));
        assertEquals(new HashSet<>(Arrays.asList("a.b.c", "d.e.f", "test")),
                re.regionPackageMap.get("global"));
        assertEquals(f, props.get(REGION_PACKAGE_FILENAME));
    }

    @Test
    public void testBegin() throws Exception {
        System.setProperty(PROPERTIES_FILE_PREFIX + IDBSNVER_FILENAME,
                getClass().getResource("/idbsnver1.properties").getFile());
        System.setProperty(PROPERTIES_FILE_PREFIX + BUNDLE_FEATURE_FILENAME,
                getClass().getResource("/bundles1.properties").getFile());
        System.setProperty(PROPERTIES_FILE_PREFIX + FEATURE_REGION_FILENAME,
                getClass().getResource("/features1.properties").getFile());
        System.setProperty(PROPERTIES_FILE_PREFIX + REGION_PACKAGE_FILENAME,
                getClass().getResource("/regions1.properties").getFile());

        RegionEnforcer re = new RegionEnforcer(new Hashtable<>(), "*");
        assertTrue(re.bsnVerMap.size() > 0);
        assertTrue(re.bundleFeatureMap.size() > 0);
        assertTrue(re.featureRegionMap.size() > 0);
        assertTrue(re.regionPackageMap.size() > 0);

        ResolverHookImpl hook = (ResolverHookImpl) re.begin(null);
        assertEquals(re.bsnVerMap, hook.bsnVerMap);
        assertEquals(re.bundleFeatureMap, hook.bundleFeatureMap);
        assertEquals(re.featureRegionMap, hook.featureRegionMap);
        assertEquals(re.regionPackageMap, hook.regionPackageMap);
    }
}
