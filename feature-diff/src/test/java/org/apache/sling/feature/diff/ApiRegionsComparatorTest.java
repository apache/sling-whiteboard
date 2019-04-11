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
package org.apache.sling.feature.diff;

import static org.junit.Assert.*;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.apiregions.model.ApiRegion;
import org.apache.sling.feature.apiregions.model.ApiRegions;
import org.apache.sling.feature.apiregions.model.io.json.ApiRegionsJSONSerializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ApiRegionsComparatorTest {

    private ExtensionsComparator extensionsComparator;

    @Before
    public void setUp() {
        extensionsComparator = new ExtensionsComparator();
    }

    @After
    public void tearDown() {
        extensionsComparator = null;
    }

    @Test
    public void checkRemoved() {
        ApiRegions previousRegions = new ApiRegions();
        ApiRegion base = previousRegions.addNew("base");
        base.add("org.apache.felix.inventory");
        base.add("org.apache.felix.metatype");

        ApiRegion extended = previousRegions.addNew("extended");
        extended.add("org.apache.felix.scr.component");
        extended.add("org.apache.felix.scr.info");

        ApiRegions currentRegions = new ApiRegions();
        base = currentRegions.addNew("base");
        base.add("org.apache.felix.inventory");
        base.add("org.apache.felix.metatype");

        DiffSection regionsDiff = serializeThenCompare(previousRegions, currentRegions);
        assertEquals("extended", regionsDiff.getRemoved().iterator().next());
    }

    @Test
    public void checkAdded() {
        ApiRegions previousRegions = new ApiRegions();
        ApiRegion base = previousRegions.addNew("base");
        base.add("org.apache.felix.inventory");
        base.add("org.apache.felix.metatype");

        ApiRegions currentRegions = new ApiRegions();
        base = currentRegions.addNew("base");
        base.add("org.apache.felix.inventory");
        base.add("org.apache.felix.metatype");

        ApiRegion extended = currentRegions.addNew("extended");
        extended.add("org.apache.felix.scr.component");
        extended.add("org.apache.felix.scr.info");

        DiffSection regionsDiff = serializeThenCompare(previousRegions, currentRegions);
        assertEquals("extended", regionsDiff.getAdded().iterator().next());
    }

    @Test
    public void checkUpdated() {
        ApiRegions previousRegions = new ApiRegions();
        ApiRegion base = previousRegions.addNew("base");
        base.add("org.apache.felix.inventory");
        base.add("org.apache.felix.metatype");

        ApiRegions currentRegions = new ApiRegions();
        base = currentRegions.addNew("base");
        base.add("org.apache.felix.inventory");
        base.add("org.apache.felix.scr.component");

        DiffSection regionsDiff = serializeThenCompare(previousRegions, currentRegions);
        DiffSection updatedDiff = regionsDiff.getUpdates().iterator().next();
        assertFalse(updatedDiff.isEmpty());
        assertEquals("base", updatedDiff.getId());
        assertEquals("org.apache.felix.metatype", updatedDiff.getRemoved().iterator().next());
        assertEquals("org.apache.felix.scr.component", updatedDiff.getAdded().iterator().next());
    }

    private DiffSection serializeThenCompare(ApiRegions previousRegions, ApiRegions currentRegions) {
        Extension previousExtension = ApiRegionsJSONSerializer.serializeApiRegions(previousRegions);
        Extension currentExtension = ApiRegionsJSONSerializer.serializeApiRegions(currentRegions);
        DiffSection regionsDiff = extensionsComparator.compare(previousExtension, currentExtension);
        assertNotNull(regionsDiff);
        assertFalse(regionsDiff.isEmpty());
        return regionsDiff;
    }

}
