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
package org.apache.sling.feature.whitelist.impl;

import org.apache.sling.feature.whitelist.WhitelistService;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WhitelistServiceImplTest {
    @Test
    public void testListRegions() {
        Map<String, Set<String>> frm = new HashMap<>();
        frm.put("myfeature", new HashSet<>(Arrays.asList("rega", "regb", "regc")));
        frm.put("myotherfeature", Collections.emptySet());
        WhitelistService wls = new WhitelistServiceImpl(Collections.emptyMap(), frm);

        assertEquals(new HashSet<>(Arrays.asList("rega", "regb", "regc")),
                wls.listRegions("myfeature"));
        assertEquals(0, wls.listRegions("myotherfeature").size());
        assertNull(wls.listRegions("nonexistent"));
    }

    @Test
    public void testRegionContainsPackage() {
        Map<String, Set<String>> rpm = new HashMap<>();
        rpm.put("region1", new HashSet<>(Arrays.asList("org.foo", "org.bar", "org.foo.bar")));
        WhitelistService wls = new WhitelistServiceImpl(rpm, Collections.emptyMap());

        assertTrue(wls.regionWhitelistsPackage("region1", "org.foo"));
        assertTrue(wls.regionWhitelistsPackage("region1", "org.foo.bar"));
        assertFalse(wls.regionWhitelistsPackage("region1", "org.bar.foo"));
        assertNull(wls.regionWhitelistsPackage("nonexitent", "org.foo"));
    }
}
