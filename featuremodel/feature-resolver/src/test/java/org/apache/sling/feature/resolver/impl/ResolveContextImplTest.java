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
package org.apache.sling.feature.resolver.impl;

import org.apache.felix.utils.resource.CapabilityImpl;
import org.apache.felix.utils.resource.RequirementImpl;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolveContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ResolveContextImplTest {
    @Test
    public void testMandatory() {
        Resource mainRes = new BundleResourceImpl("a", "1", null, null, Collections.emptyMap(), Collections.emptyMap());
        List<Resource> available = Arrays.asList();
        ResolveContext ctx = new ResolveContextImpl(mainRes, available);

        assertEquals(Collections.singleton(mainRes), ctx.getMandatoryResources());
    }

    @Test
    public void testFindProviders() {
        Resource res1 = exportBundle("org.foo", "2");
        Resource res2 = exportBundle("org.bar", "1.2");
        Resource res3 = exportBundle("org.foo", "1.0.0.TESTING");
        Resource res4 = exportBundle("org.foo", "1.9");

        Resource mainRes = new BundleResourceImpl("b", "2", null, null, Collections.emptyMap(), Collections.emptyMap());
        List<Resource> available = Arrays.asList(res1, res2, res3, res4);
        ResolveContext ctx = new ResolveContextImpl(mainRes, available);

        Requirement req = new RequirementImpl(null, PackageNamespace.PACKAGE_NAMESPACE,
                Collections.singletonMap("filter",
                        "(&(osgi.wiring.package=org.foo)(&(version>=1.0.0)(!(version>=2.0.0))))"), null);

        List<Capability> expected = new ArrayList<>();
        expected.addAll(res3.getCapabilities(null));
        expected.addAll(res4.getCapabilities(null));
        List<Capability> providers = ctx.findProviders(req);
        assertEquals(expected, providers);
    }

    private Resource exportBundle(String pkg, String version) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(PackageNamespace.PACKAGE_NAMESPACE, pkg);
        attrs.put(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, new Version(version));
        Capability cap = new CapabilityImpl(null, PackageNamespace.PACKAGE_NAMESPACE,
                null, attrs);
        return new BundleResourceImpl("c", "3", null, null,
                Collections.singletonMap(PackageNamespace.PACKAGE_NAMESPACE,
                        Collections.singletonList(cap)),
                Collections.emptyMap());
    }

    @Test
    public void testInsertHostedCapability() {
        ResolveContext ctx = new ResolveContextImpl(Mockito.mock(Resource.class),
                Collections.emptyList());

        Capability cap1 =
                new CapabilityImpl(null, "abc1", null, null);
        Capability cap2 =
                new CapabilityImpl(null, "abc2", null, null);
        List<Capability> caps = new ArrayList<>();
        caps.add(cap1);
        caps.add(cap2);

        HostedCapability hc = Mockito.mock(HostedCapability.class);
        int idx = ctx.insertHostedCapability(caps, hc);
        assertSame(hc, caps.get(idx));
        assertEquals(3, caps.size());
    }

    @Test
    public void testEffectiveRequirement() {
        ResolveContext ctx = new ResolveContextImpl(Mockito.mock(Resource.class),
                Collections.emptyList());

        Map<String, String> dirs = new HashMap<>();
        dirs.put("filter", "(somekey=someval)");
        dirs.put("effective", "resolve ");
        Requirement ereq1 = new RequirementImpl(null, PackageNamespace.PACKAGE_NAMESPACE,
                dirs, null);
        assertTrue(ctx.isEffective(ereq1));

        Requirement ereq2 = new RequirementImpl(null, PackageNamespace.PACKAGE_NAMESPACE,
                Collections.singletonMap("filter", "(a=b)"), null);
        assertTrue(ctx.isEffective(ereq2));

        Requirement req3 = new RequirementImpl(null, PackageNamespace.PACKAGE_NAMESPACE,
                Collections.singletonMap("effective", "active"), null);
        assertFalse(ctx.isEffective(req3));
    }
}
