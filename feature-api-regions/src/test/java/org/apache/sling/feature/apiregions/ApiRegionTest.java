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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class ApiRegionTest {

    @Test
    public void ignoreNullPackageName() {
        ingnoreApi(null);
    }

    @Test
    public void ignoreEmptyPackageName() {
        ingnoreApi("");
    }

    @Test
    public void ignoreInvalidPackageName() {
        ingnoreApi("1inv4l1d.package");
    }

    @Test
    public void ignoreReservedWords() {
        ingnoreApi("org.apache.commons.lang.enum");
    }

    private void ingnoreApi(String api) {
        ApiRegion testRegion = new ApiRegion(getClass().getName(), null);
        testRegion.addApi(api);
        assertFalse(testRegion.contains(api));
    }

    @Test
    public void inheritanceCheck() {
        ApiRegion granpa = new ApiRegion("granpa", null);
        ApiRegion father = new ApiRegion("father", granpa);
        ApiRegion child = new ApiRegion("child", father);

        assertSame(father, child.getParent());
        assertSame(granpa, father.getParent());
        assertNull(granpa.getParent());
    }

    @Test
    public void inheritanceContainsCheck() {
        ApiRegion granpa = new ApiRegion("granpa", null);
        granpa.addApi("org.apache.sling.feature.apiregions");

        ApiRegion father = new ApiRegion("father", granpa);
        father.addApi("org.apache.sling.feature.apiregions.io");

        ApiRegion child = new ApiRegion("child", father);
        child.addApi("org.apache.sling.feature.apiregions.io.json");

        assertTrue(child.contains("org.apache.sling.feature.apiregions.io.json"));
        assertTrue(child.contains("org.apache.sling.feature.apiregions.io")); // inherited
        assertTrue(child.contains("org.apache.sling.feature.apiregions")); // inherited-inherited
    }

    @Test
    public void inheritanceEmptyCheck() {
        ApiRegion granpa = new ApiRegion("granpa", null);
        granpa.addApi("org.apache.sling.feature.apiregions");

        ApiRegion father = new ApiRegion("father", granpa);
        ApiRegion child = new ApiRegion("child", father);

        assertFalse(child.isEmpty());
        assertFalse(father.isEmpty());
        assertFalse(granpa.isEmpty());

        ApiRegion neighbour = new ApiRegion("neighbour", null);
        assertTrue(neighbour.isEmpty());
    }

    @Test
    public void inheritanceAvoidsDuplicates() {
        ApiRegion granpa = new ApiRegion("granpa", null);
        assertTrue(granpa.addApi("org.apache.sling.feature.apiregions"));

        ApiRegion father = new ApiRegion("father", granpa);
        assertFalse(father.addApi("org.apache.sling.feature.apiregions"));
    }

    @Test
    public void inheritaceRemove() {
        ApiRegion father = new ApiRegion("father", null);
        father.addApi("org.apache.sling.feature.apiregions");

        ApiRegion child = new ApiRegion("child", father);
        child.addApi("org.apache.sling.feature.apiregions.io");

        assertFalse(child.remove(null));
        assertFalse(child.remove(""));
        assertFalse(child.remove("does.not.exist"));
        assertTrue(child.remove("org.apache.sling.feature.apiregions")); // inherited
        assertFalse(child.contains("org.apache.sling.feature.apiregions")); // inherited
        assertFalse(child.contains("org.apache.sling.feature.apiregions"));
    }

    @Test
    public void inheritanceIteratorCheck() {
        ApiRegion granpa = new ApiRegion("granpa", null);
        granpa.addApi("org.apache.sling.feature.apiregions");

        ApiRegion father = new ApiRegion("father", granpa);
        father.addApi("org.apache.sling.feature.apiregions.io");

        ApiRegion child = new ApiRegion("child", father);
        child.addApi("org.apache.sling.feature.apiregions.io.json");

        // build the expected packages

        Set<String> packages = new HashSet<>();
        packages.add("org.apache.sling.feature.apiregions.io.json");
        packages.add("org.apache.sling.feature.apiregions.io");
        packages.add("org.apache.sling.feature.apiregions");

        for (String api : child) {
            packages.remove(api);
        }

        assertTrue("Expected all packages removed, still found" + packages, packages.isEmpty());
    }

}
