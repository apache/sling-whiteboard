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

import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class ResolverHookImplTest {
    @Test
    public void testFilterMatches() {
        Map<Entry<String, Version>, List<String>> bsnvermap = new HashMap<>();
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>("system.bundle", new Version(3,2,1)),
                Collections.singletonList("b0"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>("a.b.c", new Version(0,0,0)),
                Collections.singletonList("b7"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>("a.bundle", new Version(1,0,0)),
                Collections.singletonList("b8"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>("some.other.bundle", new Version(9,9,9,"suffix")),
                Collections.singletonList("b9"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>("a-bundle", new Version(1,0,0,"SNAPSHOT")),
                Collections.singletonList("b10"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>("not.in.a.feature", new Version(0,0,1)),
                Collections.singletonList("b11"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>("also.not.in.a.feature", new Version(0,0,1)),
                Collections.singletonList("b12"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>("a.b.c", new Version(1,2,3)),
                Collections.singletonList("b17"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>("x.y.z", new Version(9,9,9)),
                Collections.singletonList("b19"));
        bsnvermap.put(new AbstractMap.SimpleEntry<String,Version>("zzz", new Version(1,0,0)),
                Collections.singletonList("b20"));

        Map<String, Set<String>> bfmap = new HashMap<>();
        bfmap.put("b7", Collections.singleton("f"));
        bfmap.put("b8", Collections.singleton("f1"));
        bfmap.put("b9", Collections.singleton("f2"));
        bfmap.put("b10", Collections.singleton("f2"));
        bfmap.put("b17", Collections.singleton("f3"));
        bfmap.put("b19", Collections.singleton("f3"));
        bfmap.put("b20", Collections.singleton("f4"));

        Map<String, Set<String>> frmap = new HashMap<>();
        frmap.put("f", new HashSet<>(Arrays.asList("r1", "r2", RegionEnforcer.GLOBAL_REGION)));
        frmap.put("f1", Collections.singleton("r1"));
        frmap.put("f2", Collections.singleton("r2"));
        frmap.put("f3", Collections.singleton("r3"));
        frmap.put("f4", Collections.singleton("r3"));

        Map<String, Set<String>> rpmap = new HashMap<>();
        rpmap.put("r0", Collections.singleton("org.bar"));
        rpmap.put("r1", new HashSet<>(Arrays.asList("org.blah", "org.foo")));
        rpmap.put(RegionEnforcer.GLOBAL_REGION, Collections.singleton("org.bar.tar"));
        rpmap.put("r3", Collections.singleton("xyz"));

        ResolverHookImpl rh = new ResolverHookImpl(bsnvermap, bfmap, frmap, rpmap);

        // Check that we can get the capability from another bundle in the same region
        // where that region exports the package
        // Bundle 7 is in feature f with regions r1, r2. Bundle 8 is in feature f1 with regions r1
        // r1 exports the org.foo package
        BundleRequirement req0 = mockRequirement("b7", bsnvermap);
        BundleCapability bc0 = mockCapability("org.foo", "b8", bsnvermap);
        List<BundleCapability> candidates0 = new ArrayList<>(Arrays.asList(bc0));
        rh.filterMatches(req0, candidates0);
        assertEquals(Collections.singletonList(bc0), candidates0);

        // Check that we cannot get the capability from another bundle in the same region
        // but that region doesn't export the pacakge.
        // Bundle 7 is in feature f with regions r1, r2. Bundle 9 is in feature f2 with regions r2
        // r2 does not export any packages
        BundleRequirement req1 = mockRequirement("b7", bsnvermap);
        BundleCapability bc1 = mockCapability("org.foo", "b9", bsnvermap);
        List<BundleCapability> candidates1 = new ArrayList<>(Arrays.asList(bc1));
        rh.filterMatches(req1, candidates1);
        assertEquals(Collections.emptyList(), candidates1);

        // Check that we cannot get the capability from another bundle in a different region
        // Bundle 9 is in feature f2 with region r2
        // Bundle 17 is in feature f3 with region r3
        BundleRequirement req2 = mockRequirement("b9", bsnvermap);
        BundleCapability bc2 = mockCapability("org.bar", "b17", bsnvermap);
        Collection<BundleCapability> c2 = new ArrayList<>(Arrays.asList(bc2));
        rh.filterMatches(req2, c2);
        assertEquals(0, c2.size());

        // Check that we can get the capability from the same bundle
        BundleRequirement req3 = mockRequirement("b9", bsnvermap);
        BundleCapability bc3 = mockCapability("abc.xyz", "b9", bsnvermap);
        Collection<BundleCapability> c3 = new ArrayList<>(Arrays.asList(bc3));
        rh.filterMatches(req3, c3);
        assertEquals(Collections.singletonList(bc3), c3);

        // Check that we can get the capability from the another bundle in the same feature
        BundleRequirement req4 = mockRequirement("b9", bsnvermap);
        BundleCapability bc4 = mockCapability("some.cool.package", "b10", bsnvermap);
        Collection<BundleCapability> c4 = new ArrayList<>(Arrays.asList(bc4));
        rh.filterMatches(req4, c4);
        assertEquals(Collections.singletonList(bc4), c4);

        // Check that we can get the capability from another bundle where the capability
        // is globally visible b7 exposes org.bar.tar in the global region, so b17 can see it
        BundleRequirement req5 = mockRequirement("b17", bsnvermap);
        BundleCapability bc5 = mockCapability("org.bar.tar", "b7", bsnvermap);
        Collection<BundleCapability> c5 = new ArrayList<>(Arrays.asList(bc5));
        rh.filterMatches(req5, c5);
        assertEquals(Collections.singletonList(bc5), c5);

        // Check that capabilities in non-package namespaces are ignored
        BundleRequirement req7 = Mockito.mock(BundleRequirement.class);
        Mockito.when(req7.getNamespace()).thenReturn("some.other.namespace");
        BundleCapability bc7 = mockCapability("org.bar", "b17", bsnvermap);
        Collection<BundleCapability> c7 = new ArrayList<>(Arrays.asList(bc7));
        rh.filterMatches(req7, c7);
        assertEquals(Collections.singletonList(bc7), c7);

        // Check that we can get the capability from another provider in the same region
        BundleRequirement req8 = mockRequirement("b20", bsnvermap);
        BundleCapability bc8 = mockCapability("xyz", "b19", bsnvermap);
        Collection<BundleCapability> c8 = new ArrayList<>(Arrays.asList(bc8));
        rh.filterMatches(req8, c8);
        assertEquals(Collections.singletonList(bc8), c8);

        // A requirement from a bundle that has no feature cannot access one in a region
        // b17 provides package xyz which is in region r3, but b11 is not in any region.
        BundleRequirement req9 = mockRequirement("b11", bsnvermap);
        BundleCapability bc9 = mockCapability("xyz", "b17", bsnvermap);
        Collection<BundleCapability> c9 = new ArrayList<>(Arrays.asList(bc9));
        rh.filterMatches(req9, c9);
        assertEquals(0, c9.size());

        // A requirement from a bundle that has no feature can still access one in the global region
        // b7 exposes org.bar.tar in the global region, so b11 can see it
        BundleRequirement req10 = mockRequirement("b11", bsnvermap);
        BundleCapability bc10 = mockCapability("org.bar.tar", "b7", bsnvermap);
        Collection<BundleCapability> c10 = new ArrayList<>(Arrays.asList(bc10));
        rh.filterMatches(req10, c10);
        assertEquals(Collections.singletonList(bc10), c10);

        // A requirement from a bundle that has no feature can be satisfied by a capability
        // from a bundle that has no feature
        BundleRequirement req11 = mockRequirement("b11", bsnvermap);
        BundleCapability bc11 = mockCapability("ding.dong", "b12", bsnvermap);
        Collection<BundleCapability> c11 = new ArrayList<>(Arrays.asList(bc11));
        rh.filterMatches(req11, c11);
        assertEquals(Collections.singletonList(bc11), c11);

        // A capability from the system bundle is always accessible
        BundleRequirement req12 = mockRequirement("b11", bsnvermap);
        BundleCapability bc12 = mockCapability("ping.pong", "b0", bsnvermap);
        Collection<BundleCapability> c12 = new ArrayList<>(Arrays.asList(bc12));
        rh.filterMatches(req12, c12);
        assertEquals(Collections.singletonList(bc12), c12);
    }

    private BundleCapability mockCapability(String pkgName, String bid, Map<Entry<String, Version>, List<String>> bsnvermap) {
        for (Map.Entry<Map.Entry<String, Version>, List<String>> entry : bsnvermap.entrySet()) {
            if (entry.getValue().contains(bid)) {
                // Remove first letter and use rest as bundle ID
                long id = Long.parseLong(bid.substring(1));
                return mockCapability(pkgName, id, entry.getKey().getKey(), entry.getKey().getValue());
            }
        }
        throw new IllegalStateException("Bundle not found " + bid);
    }

    private BundleCapability mockCapability(String pkg, long bundleID, String bsn, Version version) {
        Map<String, Object> attrs =
                Collections.singletonMap(PackageNamespace.PACKAGE_NAMESPACE, pkg);

        Bundle bundle = Mockito.mock(Bundle.class);
        Mockito.when(bundle.getBundleId()).thenReturn(bundleID);
        Mockito.when(bundle.getSymbolicName()).thenReturn(bsn);
        Mockito.when(bundle.getVersion()).thenReturn(version);

        BundleRevision br = Mockito.mock(BundleRevision.class);
        Mockito.when(br.getBundle()).thenReturn(bundle);


        BundleCapability cap = Mockito.mock(BundleCapability.class);
        Mockito.when(cap.getAttributes()).thenReturn(attrs);
        Mockito.when(cap.getRevision()).thenReturn(br);
        return cap;
    }

    private BundleRequirement mockRequirement(String bid, Map<Map.Entry<String, Version>, List<String>> bsnvermap) {
        for (Map.Entry<Map.Entry<String, Version>, List<String>> entry : bsnvermap.entrySet()) {
            if (entry.getValue().contains(bid)) {
                // Remove first letter and use rest as bundle ID
                long id = Long.parseLong(bid.substring(1));
                return mockRequirement(id, entry.getKey().getKey(), entry.getKey().getValue());
            }
        }
        throw new IllegalStateException("Bundle not found " + bid);
    }

    private BundleRequirement mockRequirement(long bundleID, String bsn, Version version) {
        Bundle bundle = Mockito.mock(Bundle.class);
        Mockito.when(bundle.getBundleId()).thenReturn(bundleID);
        Mockito.when(bundle.getSymbolicName()).thenReturn(bsn);
        Mockito.when(bundle.getVersion()).thenReturn(version);

        BundleRevision br = Mockito.mock(BundleRevision.class);
        Mockito.when(br.getBundle()).thenReturn(bundle);

        BundleRequirement req = Mockito.mock(BundleRequirement.class);
        Mockito.when(req.getNamespace()).thenReturn(PackageNamespace.PACKAGE_NAMESPACE);
        Mockito.when(req.getRevision()).thenReturn(br);

        return req;
    }
}
