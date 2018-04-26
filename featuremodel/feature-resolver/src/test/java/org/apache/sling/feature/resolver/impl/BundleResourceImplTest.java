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
import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.resolver.FeatureResource;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.impl.BundleDescriptorImpl;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BundleResourceImplTest {
    @Test
    public void testResource() throws Exception {
        String bmf = "Bundle-SymbolicName: foobar\n"
            + "Bundle-Version: 1.2.3\n"
            + "Bundle-ManifestVersion: 2\n"
            + "Provide-Capability: ns.1;ns.1=\"c1\",ns.1;ns.1=\"c2\",ns.2;ns.2=\"c3\"\n"
            + "Require-Capability: ns.1;mydir:=\"myvalue\"\n";
        File f = createBundle(bmf);

        Artifact artifact = new Artifact(new ArtifactId("grp", "a.b.c", "1.2.3", null, null));
        BundleDescriptor bd = new BundleDescriptorImpl(artifact, f, 1);
        Feature feat = Mockito.mock(Feature.class);
        FeatureResource res = new BundleResourceImpl(bd, feat);

        Capability c1 = new CapabilityImpl(res, "ns.1", null,
                Collections.singletonMap("ns.1", "c1"));
        Capability c2 = new CapabilityImpl(res, "ns.1", null,
                Collections.singletonMap("ns.1", "c2"));
        List<Capability> expectedCaps1 = Arrays.asList(c1, c2);

        Capability c3 = new CapabilityImpl(res, "ns.2", null,
                Collections.singletonMap("ns.2", "c3"));
        List<Capability> expectedCaps2 = Collections.singletonList(c3);

        Requirement r1 = new RequirementImpl(res, "ns.1",
                Collections.singletonMap("mydir", "myvalue"), null);
        List<Requirement> expectedReqs = Collections.singletonList(r1);

        assertEquals(0, res.getCapabilities("nonexistent").size());
        assertEquals(0, res.getRequirements("ns.2").size());
        assertEquals(expectedCaps1, res.getCapabilities("ns.1"));
        assertEquals(expectedReqs, res.getRequirements("ns.1"));

        List<Capability> mergedCaps = res.getCapabilities(null);
        assertTrue(mergedCaps.containsAll(expectedCaps1));
        assertTrue(mergedCaps.containsAll(expectedCaps2));
        assertEquals(expectedReqs, res.getRequirements(null));

        assertEquals("foobar", res.getId());
        assertEquals(new Version("1.2.3"), res.getVersion());
        assertSame(feat, res.getFeature());
    }

    @Test
    public void testBundleResource() throws Exception {
        ArtifactId id = new ArtifactId("grp", "art", "1.2.3", null, null);
        Artifact artifact = new Artifact(id);

        String bmf = "Bundle-SymbolicName: " + Constants.SYSTEM_BUNDLE_SYMBOLICNAME + "\n"
            + "Bundle-Version: 1.2.3\n"
            + "Bundle-ManifestVersion: 2\n"
            + "Export-Package: org.foo.a;version=0.0.1.SNAPSHOT\n"
            + "Import-Package: org.bar;version=\"[1,2)\",org.tar;resolution:=\"optional\"\n";
        File f = createBundle(bmf);

        BundleDescriptor bd = new BundleDescriptorImpl(artifact, f, 1);
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
        assertEquals("(&(osgi.wiring.package=org.bar)(version>=1.0.0)(!(version>=2.0.0)))",
                reqBar.getDirectives().get(PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE));

        assertEquals(2, reqTar.getDirectives().size());
        assertEquals("(osgi.wiring.package=org.tar)",
                reqTar.getDirectives().get(PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE));
        assertEquals(PackageNamespace.RESOLUTION_OPTIONAL,
                reqTar.getDirectives().get(PackageNamespace.REQUIREMENT_RESOLUTION_DIRECTIVE));
    }

    @Test
    public void testBundleResourceGenericCapReq() throws Exception {
        String bmf = "Bundle-SymbolicName: org.apache.someartifact\n"
            + "Bundle-Version: 0.0.0\n"
            + "Bundle-ManifestVersion: 2\n"
            + "Provide-Capability: org.example.cap1;intAttr=\"999\";somedir:=\"mydir\"\n"
            + "Require-Capability: org.example.req1;adir:=\"aval\";verAttr=\"1.2.3\","
                + "org.example.req2;adir:=\"aval2\";verAttr=\"3.2.1\"\n";
        File f = createBundle(bmf);

        ArtifactId id = new ArtifactId("org.apache", "org.apache.someartifact", "0.0.0", null, null);
        Artifact artifact = new Artifact(id);

        BundleDescriptorImpl bd = new BundleDescriptorImpl(artifact, f, 1);
        Resource res = new BundleResourceImpl(bd, null);

        Capability cap = new CapabilityImpl(res, "org.example.cap1",
                Collections.singletonMap("somedir", "mydir"),
                Collections.singletonMap("intAttr", "999"));
        Set<Capability> caps = Collections.singleton(cap);

        Requirement req1 = new RequirementImpl(res, "org.example.req1",
                Collections.singletonMap("adir", "aval"),
                Collections.singletonMap("verAttr", "1.2.3"));
        Requirement req2 = new RequirementImpl(res, "org.example.req2",
                Collections.singletonMap("adir", "aval2"),
                Collections.singletonMap("verAttr", "3.2.1"));
        assertEquals(caps, new HashSet<>(res.getCapabilities("org.example.cap1")));

        assertEquals(Collections.singleton(req1),
                new HashSet<>(res.getRequirements("org.example.req1")));
        assertEquals(Collections.singleton(req2),
                new HashSet<>(res.getRequirements("org.example.req2")));
        assertEquals(new HashSet<>(Arrays.asList(req1, req2)),
                new HashSet<>(res.getRequirements(null)));
    }

    private File createBundle(String manifest) throws IOException
    {
        File f = File.createTempFile("bundle", ".jar");
        f.deleteOnExit();
        Manifest mf = new Manifest(new ByteArrayInputStream(manifest.getBytes("UTF-8")));
        mf.getMainAttributes().putValue("Manifest-Version", "1.0");
        JarOutputStream os = new JarOutputStream(new FileOutputStream(f), mf);
        os.close();
        return f;
    }

    private Object getCapAttribute(Resource res, String ns, String attr) {
        List<Capability> caps = res.getCapabilities(ns);
        if (caps.size() == 0)
            return null;

        Capability cap = caps.iterator().next();
        return cap.getAttributes().get(attr);
    }
}
