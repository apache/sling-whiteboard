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
import org.osgi.service.cm.ConfigurationException;

import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WhitelistEnforcerTest {
    @Test
    public void testWhitelistEnforcerConfigUpdate() throws ConfigurationException {
        WhitelistEnforcer enf = new WhitelistEnforcer(null);

        assertTrue("Precondition",
                enf.whitelistService instanceof WhitelistEnforcer.NullWhitelistService);

        Dictionary<String, Object> props = new Hashtable<>();
        props.put("ignored", "ignored-too");
        props.put("whitelist.region.region1", "org.foo.pkg1");
        props.put("whitelist.region.region2", new String[] {"pkga", "pkgb"});
        props.put("whitelist.region.region.3", Arrays.asList("a.b.c", "d.e.f", "g.h.i"));
        props.put("whitelist.feature.gid:aid:slingfeature:testfeature:1.0.0", WhitelistService.GLOBAL_REGION);
        props.put("whitelist.feature.gid:myfeature:1.0.0", new String [] {"region1", "region2"});
        enf.updated(props);

        assertFalse(enf.whitelistService instanceof WhitelistEnforcer.NullWhitelistService);

        // check that the configuration parsing worked
        assertTrue(enf.whitelistService.regionWhitelistsPackage("region1", "org.foo.pkg1"));
        assertTrue(enf.whitelistService.regionWhitelistsPackage("region2", "pkga"));
        assertTrue(enf.whitelistService.regionWhitelistsPackage("region2", "pkgb"));
        assertFalse(enf.whitelistService.regionWhitelistsPackage("region1", "pkg1"));
        assertTrue(enf.whitelistService.regionWhitelistsPackage("region.3", "d.e.f"));
        assertFalse(enf.whitelistService.regionWhitelistsPackage("region.3", "d.e.f.g"));
        assertNull(enf.whitelistService.regionWhitelistsPackage("unknown", "pkga"));

        Set<String> regions = enf.whitelistService.listRegions("gid:myfeature:1.0.0");
        assertEquals(new HashSet<String>(Arrays.asList("region1", "region2")), regions);
        assertEquals(Collections.singleton("global"), enf.whitelistService
                .listRegions("gid:aid:slingfeature:testfeature:1.0.0"));
        assertNull(enf.whitelistService.listRegions("unknown"));

        enf.updated(null);
        assertTrue("A null configuration should put back the null whitelist service",
                enf.whitelistService instanceof WhitelistEnforcer.NullWhitelistService);
    }
}
