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
package org.apache.sling.feature.apiregions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ApiRegionsTest {

    private ApiRegions apiRegions;

    @Before
    public void setUp() {
        apiRegions = new ApiRegions();
    }

    @After
    public void tearDown() {
        apiRegions = null;
    }

    @Test(expected = IllegalArgumentException.class)
    public void canNotCreateRegionWithNullName() {
        apiRegions.addNew(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void canNotCreateRegionWithEmptyName() {
        apiRegions.addNew("");
    }

    @Test
    public void impossibleToGetRegionWithNullName() {
        assertNull(apiRegions.getByName(null));
    }

    @Test
    public void impossibleToGetRegionWithEmptyName() {
        assertNull(apiRegions.getByName(""));
    }

    @Test
    public void impossibleToGetRegionWithNotRegisteredRegion() {
        assertNull(apiRegions.getByName("not-registered-yet"));
    }

    @Test
    public void getRegionWithRegisteredRegion() {
        assertTrue(apiRegions.isEmpty());

        String regionName = "registered";
        ApiRegion region = apiRegions.addNew(regionName);
        assertNotNull(region);
        assertFalse(apiRegions.isEmpty());
        assertNotNull(apiRegions.getByName(regionName));
    }

    @Test
    public void sequentialInheritance() {
        ApiRegion granpa = apiRegions.addNew("granpa");
        ApiRegion father = apiRegions.addNew("father");
        ApiRegion child = apiRegions.addNew("child");

        assertSame(father, child.getParent());
        assertSame(granpa, father.getParent());
        assertNull(granpa.getParent());
    }

    @Test
    public void iteratingOverRegions() {
        apiRegions.addNew("granpa");
        apiRegions.addNew("father");
        apiRegions.addNew("child");

        List<String> expected = Arrays.asList("granpa", "father", "child");

        List<String> actual = new LinkedList<>();
        for (ApiRegion region : apiRegions) {
            actual.add(region.getName());
        }

        assertEquals(expected, actual);
    }

}
