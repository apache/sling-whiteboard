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

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.FeatureResource;
import org.apache.sling.feature.OSGiCapability;
import org.apache.sling.feature.OSGiRequirement;
import org.apache.sling.feature.analyser.BundleDescriptor;
import org.apache.sling.feature.analyser.Descriptor;
import org.apache.sling.feature.analyser.impl.BundleDescriptorImpl;
import org.apache.sling.feature.support.util.PackageInfo;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BundleResourceImplTest {
    @Test
    public void testResource() {
        Map<String, List<Capability>> caps = new HashMap<>();

        Capability c1 = new OSGiCapability("ns.1",
                Collections.singletonMap("ns.1", "c1"), Collections.emptyMap());
        Capability c2 = new OSGiCapability("ns.1",
                Collections.singletonMap("ns.1", "c2"), Collections.emptyMap());
        List<Capability> capLst1 = Arrays.asList(c1, c2);
        caps.put("ns.1", capLst1);
        Capability c3 = new OSGiCapability("ns.2",
                Collections.singletonMap("ns.2", "c3"), Collections.emptyMap());
        List<Capability> capLst2 = Collections.singletonList(c3);
        caps.put("ns.2", capLst2);

        Requirement r1 = new OSGiRequirement("ns.1",
                Collections.emptyMap(), Collections.singletonMap("mydir", "myvalue"));
        List<Requirement> reqList = Collections.singletonList(r1);
        Artifact art = Mockito.mock(Artifact.class);
        Feature feat = Mockito.mock(Feature.class);
        FeatureResource res = new BundleResourceImpl("a.b.c", "1.2.3", art, feat, caps,
                Collections.singletonMap("ns.1", reqList));

        assertEquals(0, res.getCapabilities("nonexistent").size());
        assertEquals(0, res.getRequirements("ns.2").size());
        assertEquals(capLst1, res.getCapabilities("ns.1"));
        assertEquals(reqList, res.getRequirements("ns.1"));

        List<Capability> mergedCaps = res.getCapabilities(null);
        assertEquals(3, mergedCaps.size());
        assertTrue(mergedCaps.containsAll(capLst1));
        assertTrue(mergedCaps.containsAll(capLst2));
        assertEquals(reqList, res.getRequirements(null));

        assertEquals("a.b.c", res.getId());
        assertEquals(new Version("1.2.3"), res.getVersion());
        assertSame(art, res.getArtifact());
        assertSame(feat, res.getFeature());
    }

    @Test
    public void testBundleResource() throws Exception {
        ArtifactId id = new ArtifactId("grp", "art", "1.2.3", null, null);
        Artifact artifact = new Artifact(id);

        PackageInfo ex1 = new PackageInfo("org.foo.a", "0.0.1.SNAPSHOT", false);
        Set<PackageInfo> pkgs = Collections.singleton(ex1);
        Set<Requirement> reqs = Collections.emptySet();
        Set<Capability> caps = Collections.emptySet();
        BundleDescriptor bd = new BundleDescriptorImpl(artifact, pkgs, reqs, caps);

        setField(Descriptor.class, "locked", bd, false); // Unlock the Bundle Descriptor for the test
        PackageInfo im1 = new PackageInfo("org.bar", "[1,2)", false);
        PackageInfo im2 = new PackageInfo("org.tar", null, true);
        bd.getImportedPackages().add(im1);
        bd.getImportedPackages().add(im2);

        Resource res = new BundleResourceImpl(bd, null);
        assertNotNull(
                getCapAttribute(res, BundleNamespace.BUNDLE_NAMESPACE, BundleNamespace.BUNDLE_NAMESPACE));
        assertEquals(new Version("1.2.3"),
                getCapAttribute(res, BundleNamespace.BUNDLE_NAMESPACE, BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE));

        List<Capability> exports = res.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
        assertEquals(1, exports.size());
        assertEquals("org.foo.a",
                getCapAttribute(res, PackageNamespace.PACKAGE_NAMESPACE, PackageNamespace.PACKAGE_NAMESPACE));
        assertEquals(new Version("0.0.1.SNAPSHOT"),
                getCapAttribute(res, PackageNamespace.PACKAGE_NAMESPACE, PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE));
        assertEquals(getCapAttribute(res, BundleNamespace.BUNDLE_NAMESPACE, BundleNamespace.BUNDLE_NAMESPACE),
                getCapAttribute(res, PackageNamespace.PACKAGE_NAMESPACE, PackageNamespace.CAPABILITY_BUNDLE_SYMBOLICNAME_ATTRIBUTE));
        assertEquals(new Version("1.2.3"),
                getCapAttribute(res, PackageNamespace.PACKAGE_NAMESPACE, PackageNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE));

        List<Requirement> requirements = res.getRequirements(PackageNamespace.PACKAGE_NAMESPACE);
        assertEquals(2, requirements.size());

        Requirement reqBar = null;
        Requirement reqTar = null;
        for (Requirement req : requirements) {
            if (req.getDirectives().get("filter").contains("org.bar"))
                reqBar = req;
            else
                reqTar = req;
        }

        assertEquals(1, reqBar.getDirectives().size());
        assertEquals("(&(osgi.wiring.package=org.bar)(&(version>=1.0.0)(!(version>=2.0.0))))",
                reqBar.getDirectives().get(PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE));

        assertEquals(2, reqTar.getDirectives().size());
        assertEquals("(&(osgi.wiring.package=org.tar))",
                reqTar.getDirectives().get(PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE));
        assertEquals(PackageNamespace.RESOLUTION_OPTIONAL,
                reqTar.getDirectives().get(PackageNamespace.REQUIREMENT_RESOLUTION_DIRECTIVE));
    }

    @Test
    public void testBundleResourceGenericCapReq() throws Exception {
        ArtifactId id = new ArtifactId("org.apache", "org.apache.someartifact", "0.0.0", null, null);
        Artifact artifact = new Artifact(id);

        Capability cap = new OSGiCapability("org.example.cap1",
                Collections.singletonMap("intAttr", 999),
                Collections.singletonMap("somedir", "mydir"));
        Set<Capability> caps = Collections.singleton(cap);

        Requirement req1 = new OSGiRequirement("org.example.req1",
                Collections.singletonMap("boolAttr", true),
                Collections.singletonMap("adir", "aval"));
        Requirement req2 = new OSGiRequirement("org.example.req2",
                Collections.singletonMap("boolAttr", false),
                Collections.singletonMap("adir", "aval2"));
        Set<Requirement> reqs = new HashSet<>(Arrays.asList(req1, req2));
        BundleDescriptorImpl bd = new BundleDescriptorImpl(artifact, Collections.emptySet(), reqs, caps);

        Resource res = new BundleResourceImpl(bd, null);

        assertEquals(caps, new HashSet<>(res.getCapabilities("org.example.cap1")));
        assertEquals(Collections.singleton(req1),
                new HashSet<>(res.getRequirements("org.example.req1")));
        assertEquals(Collections.singleton(req2),
                new HashSet<>(res.getRequirements("org.example.req2")));
        assertEquals(reqs, new HashSet<>(res.getRequirements(null)));
    }

    private Object getCapAttribute(Resource res, String ns, String attr) {
        List<Capability> caps = res.getCapabilities(ns);
        if (caps.size() == 0)
            return null;

        Capability cap = caps.iterator().next();
        return cap.getAttributes().get(attr);
    }

    private void setField(Class<?> cls, String field, Object obj, Object val) throws Exception {
        Field f = cls.getDeclaredField(field);
        f.setAccessible(true);
        f.set(obj, val);
    }
}
