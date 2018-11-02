/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.feature.extension.apiregions;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.junit.Test;

import java.io.FileReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class BundleArtifactFeatureHandlerTest {
    @Test
    public void testBundleToFeatureMap() throws Exception {
        BundleArtifactFeatureHandler bafh = new BundleArtifactFeatureHandler();

        Feature f = new Feature(ArtifactId.fromMvnId("org.sling:something:1.2.3:slingosgifeature:myclassifier"));
        Artifact b1 = new Artifact(ArtifactId.fromMvnId("org.sling:b1:1"));
        Artifact b2 = new Artifact(ArtifactId.fromMvnId("org.sling:b2:1"));
        Artifact b3a = new Artifact(ArtifactId.fromMvnId("org.sling:b3:1"));
        b3a.getMetadata().put("org-feature", "some.other:feature:123");
        Artifact b3b = new Artifact(ArtifactId.fromMvnId("org.sling:b3:1"));
        f.getBundles().addAll(Arrays.asList(b1, b2, b3a, b3b));

        Extension ex = new Extension(ExtensionType.JSON, "api-regions", false);
        ex.setJSON("[]");
        bafh.postProcess(null, f, ex);

        String p = System.getProperty("whitelisting.bundles.properties");
        Properties actual = new Properties();
        actual.load(new FileReader(p));

        Properties expected = new Properties();
        expected.put("org.sling:b1:1", "org.sling:something:1.2.3:slingosgifeature:myclassifier");
        expected.put("org.sling:b2:1", "org.sling:something:1.2.3:slingosgifeature:myclassifier");
        expected.put("org.sling:b3:1", "some.other:feature:123,org.sling:something:1.2.3:slingosgifeature:myclassifier");
        assertEquals(expected, actual);
    }

    @Test
    public void testFeatureToRegionMap() throws Exception {
        BundleArtifactFeatureHandler bafh = new BundleArtifactFeatureHandler();

        Feature f = new Feature(ArtifactId.fromMvnId("org.sling:something:1.2.3"));
        Extension ex = new Extension(ExtensionType.JSON, "api-regions", false);
        ex.setJSON("[{\"name\":\"global\","
                + "\"exports\": [\"a.b.c\",\"d.e.f\"]},"
                + "{\"name\":\"internal\","
                + "\"exports\":[\"xyz\"]},"
                + "{\"name\":\"global\","
                + "\"exports\":[\"test\"],"
                + "\"org-feature\":\"an.other:feature:123\"}]");

        bafh.postProcess(null, f, ex);

        String p = System.getProperty("whitelisting.features.properties");
        Properties actual = new Properties();
        actual.load(new FileReader(p));

        Properties expected = new Properties();
        expected.put("an.other:feature:123", "global");
        expected.put("org.sling:something:1.2.3", "internal,global");

        String[] al = ((String) actual.remove("org.sling:something:1.2.3")).split(",");
        String[] el = ((String) expected.remove("org.sling:something:1.2.3")).split(",");
        assertEquals(new HashSet<>(Arrays.asList(el)), new HashSet<>(Arrays.asList(al)));
        assertEquals(expected, actual);

        String p2 = System.getProperty("whitelisting.regions.properties");
        Properties actual2 = new Properties();
        actual2.load(new FileReader(p2));

        Properties expected2 = new Properties();
        expected2.put("internal", "xyz");
        expected2.put("global", "test,a.b.c,d.e.f");

        String[] agl2 = ((String) actual2.remove("global")).split(",");
        String[] egl2 = ((String) expected2.remove("global")).split(",");
        assertEquals(new HashSet<>(Arrays.asList(egl2)), new HashSet<>(Arrays.asList(agl2)));
        assertEquals(expected2, actual2);
    }

    @Test
    public void testUnrelatedExtension() {
        BundleArtifactFeatureHandler bafh = new BundleArtifactFeatureHandler();
        Extension ex = new Extension(ExtensionType.JSON, "foobar", false);
        bafh.postProcess(null, null, ex);
        // Should not do anything and definitely not throw an exception
    }
}
