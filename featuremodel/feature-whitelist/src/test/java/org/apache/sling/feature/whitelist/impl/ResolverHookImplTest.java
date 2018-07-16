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

import org.apache.sling.feature.service.Features;
import org.apache.sling.feature.whitelist.WhitelistService;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.util.tracker.ServiceTracker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class ResolverHookImplTest {
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testFilterMatches() throws Exception {
        String f = "gid:aid:0.0.9";
        String f2 = "gid2:aid2:1.0.0-SNAPSHOT";
        String f3 = "gid3:aid3:1.2.3";
        String f4 = "gid4:aid4:1.2.3";

        Features fs = Mockito.mock(Features.class);
        Mockito.when(fs.getFeaturesForBundle("a.b.c", new Version(0,0,0)))
            .thenReturn(Collections.singleton(f)); // b7
        Mockito.when(fs.getFeaturesForBundle("some.other.bundle", new Version(9,9,9,"suffix")))
            .thenReturn(Collections.singleton(f2)); // b9
        Mockito.when(fs.getFeaturesForBundle("a-bundle", new Version(1,0,0,"SNAPSHOT")))
            .thenReturn(Collections.singleton(f2)); // b10
        Mockito.when(fs.getFeaturesForBundle("a.b.c", new Version(1,2,3)))
            .thenReturn(Collections.singleton(f3)); // b17
        Mockito.when(fs.getFeaturesForBundle("z.z.z", new Version(3,2,1)))
            .thenReturn(new HashSet<>(Arrays.asList(f, f3))); // b18
        Mockito.when(fs.getFeaturesForBundle("x.y.z", new Version(9,9,9)))
            .thenReturn(Collections.singleton(f3)); // b19
        Mockito.when(fs.getFeaturesForBundle("zzz", new Version(1,0,0)))
            .thenReturn(Collections.singleton(f4)); // b20

        ServiceTracker st = Mockito.mock(ServiceTracker.class);
        Mockito.when(st.waitForService(Mockito.anyLong())).thenReturn(fs);

        Map<String, Set<String>> rpm = new HashMap<>();
        rpm.put("r0", Collections.singleton("org.bar"));
        rpm.put("r1", new HashSet<>(Arrays.asList("org.blah", "org.foo")));
        rpm.put(WhitelistService.GLOBAL_REGION, Collections.singleton("org.bar.tar"));
        rpm.put("r3", Collections.singleton("xyz"));

        Map<String, Set<String>> frm = new HashMap<>();
        frm.put("gid:aid:0.0.9",
                new HashSet<>(Arrays.asList("r1", "r2", WhitelistService.GLOBAL_REGION)));
        frm.put("gid2:aid2:1.0.0-SNAPSHOT", Collections.singleton("r2"));
        frm.put("gid3:aid3:1.2.3", Collections.singleton("r3"));
        frm.put("gid4:aid4:1.2.3", Collections.singleton("r3"));

        WhitelistService wls = new WhitelistServiceImpl(rpm, frm);
        ResolverHookImpl rh = new ResolverHookImpl(st, wls);

        // A requirement from a bundle that has no feature cannot access one in a region
        BundleRequirement req9 = mockRequirement(11, "qqq", new Version(6,6,6));
        BundleCapability bc9 = mockCapability("org.bar", 17, "a.b.c", new Version(1,2,3));
        ArrayList c9 = new ArrayList<>(Arrays.asList(bc9));
        rh.filterMatches(req9, c9);
        assertEquals(0, c9.size());

        // A requirement from a bundle that has no feature can still access on in the global region
        BundleRequirement req10 = mockRequirement(11, "qqq", new Version(6,6,6));
        BundleCapability bc10 = mockCapability("org.bar.tar", 18, "z.z.z", new Version(3,2,1));
        ArrayList c10 = new ArrayList<>(Arrays.asList(bc10));
        rh.filterMatches(req10, c10);
        assertEquals(Collections.singletonList(bc10), c10);

        // A requirement from a bundle that has no feature can be satisfied by a capability
        // from a bundle that has no feature
        BundleRequirement req11 = mockRequirement(11, "qqq", new Version(6,6,6));
        BundleCapability bc11 = mockCapability("org.bar.tar", 16, "x", new Version(3,2,1));
        ArrayList c11 = new ArrayList<>(Arrays.asList(bc11));
        rh.filterMatches(req11, c11);
        assertEquals(Collections.singletonList(bc11), c11);

        // Check that we can get the capability from another bundle in the same region
        // Bundle 7 is in feature f with regions r1, r2. Bundle 9 is in feature f2 with regions r2
        BundleRequirement req = mockRequirement(7, "a.b.c", new Version(0,0,0));
        BundleCapability bc1 = mockCapability("org.foo", 9, "some.other.bundle", new Version(9,9,9,"suffix"));
        List<BundleCapability> candidates = new ArrayList<>(Arrays.asList(bc1));
        rh.filterMatches(req, candidates);
        assertEquals(Collections.singletonList(bc1), candidates);

        // Check that we cannot get the capability from another bundle in a different region
        // Bundle 9 is in feature f2 with region r2
        BundleRequirement req2 = mockRequirement(9, "some.other.bundle", new Version(9,9,9,"suffix"));
        BundleCapability bc2 = mockCapability("org.bar", 17, "a.b.c", new Version(1,2,3));
        Collection<BundleCapability> c2 = new ArrayList<>(Arrays.asList(bc2));
        rh.filterMatches(req2, c2);
        assertEquals(0, c2.size());

        // Check that we can get the capability from the same bundle
        BundleRequirement req3 = mockRequirement(9, "some.other.bundle", new Version(9,9,9,"suffix"));
        BundleCapability bc3 = mockCapability("org.bar", 9, "some.other.bundle", new Version(9,9,9,"suffix"));
        Collection<BundleCapability> c3 = new ArrayList<>(Arrays.asList(bc3));
        rh.filterMatches(req3, c3);
        assertEquals(Collections.singletonList(bc3), c3);

        // Check that we can get the capability from the another bundle in the same feature
        BundleRequirement req4 = mockRequirement(9, "some.other.bundle", new Version(9,9,9,"suffix"));
        BundleCapability bc4 = mockCapability("org.bar", 10, "a-bundle", new Version(1,0,0,"SNAPSHOT"));
        Collection<BundleCapability> c4 = new ArrayList<>(Arrays.asList(bc4));
        rh.filterMatches(req4, c4);
        assertEquals(Collections.singletonList(bc4), c4);

        // Check that we can get the capability from another bundle where the capability
        // is globally visible (from bundle 9, f2)
        BundleRequirement req5 = mockRequirement(17, "a.b.c", new Version(1,2,3));
        BundleCapability bc5 = mockCapability("org.bar.tar", 9, "some.other.bundle", new Version(9,9,9,"suffix"));
        Collection<BundleCapability> c5 = new ArrayList<>(Arrays.asList(bc5));
        rh.filterMatches(req5, c5);
        assertEquals(Collections.singletonList(bc5), c5);

        // Check that we can get the capability from another bundle where the capability
        // is globally visible (from bundle 7, f)
        BundleRequirement req6 = mockRequirement(7, "a.b.c", new Version(0,0,0));
        BundleCapability bc6 = mockCapability("org.bar.tar", 17, "a.b.c", new Version(1,2,3));
        Collection<BundleCapability> c6 = new ArrayList<>(Arrays.asList(bc6));
        rh.filterMatches(req6, c6);
        assertEquals(Collections.singletonList(bc6), c6);

        // Check that capabilities in non-package namespaces are ignored
        BundleRequirement req7 = Mockito.mock(BundleRequirement.class);
        Mockito.when(req7.getNamespace()).thenReturn("some.other.namespace");
        BundleCapability bc7 = mockCapability("org.bar", 17, "a.b.c", new Version(1,2,3));
        Collection<BundleCapability> c7 = new ArrayList<>(Arrays.asList(bc7));
        rh.filterMatches(req7, c7);
        assertEquals(Collections.singletonList(bc7), c7);

        // Check that we can get the capability from another provider in the same region
        BundleRequirement req8 = mockRequirement(20, "zzz", new Version(1,0,0));
        BundleCapability bc8 = mockCapability("xyz", 19, "x.y.z", new Version(9,9,9));
        Collection<BundleCapability> c8 = new ArrayList<>(Arrays.asList(bc8));
        rh.filterMatches(req8, c8);
        assertEquals(Collections.singletonList(bc8), c8);
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
