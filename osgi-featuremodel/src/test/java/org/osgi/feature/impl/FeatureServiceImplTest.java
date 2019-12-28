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
package org.osgi.feature.impl;

import org.junit.Test;
import org.osgi.feature.ArtifactID;
import org.osgi.feature.Bundle;
import org.osgi.feature.Configuration;
import org.osgi.feature.Extension;
import org.osgi.feature.Feature;
import org.osgi.feature.FeatureService;
import org.osgi.feature.MergeContext;
import org.osgi.feature.builder.BundleBuilder;
import org.osgi.feature.builder.ConfigurationBuilder;
import org.osgi.feature.builder.ExtensionBuilder;
import org.osgi.feature.builder.MergeContextBuilder;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FeatureServiceImplTest {
    @Test
    public void testReadFeature() throws IOException {
        FeatureService fs = new FeatureServiceImpl();

        URL res = getClass().getResource("/features/test-feature.json");
        try (Reader r = new InputStreamReader(res.openStream())) {
            Feature f = fs.readFeature(r);

            assertNull(f.getTitle());
            assertEquals("The feature description", f.getDescription());

            List<Bundle> bundles = f.getBundles();
            assertEquals(3, bundles.size());

            Bundle bundle = new BundleBuilder(new ArtifactID("org.osgi", "osgi.promise", "7.0.1"))
                    .addMetadata("hash", "4632463464363646436")
                    .addMetadata("start-order", 1L)
                    .build();

            Bundle ba = bundles.get(0);
            ba.equals(bundle);

            assertTrue(bundles.contains(bundle));
            assertTrue(bundles.contains(new BundleBuilder(new ArtifactID("org.slf4j", "slf4j-api", "1.7.29")).build()));
            assertTrue(bundles.contains(new BundleBuilder(new ArtifactID("org.slf4j", "slf4j-simple", "1.7.29")).build()));
        }
    }

    @Test
    public void testMergeFeatures() throws IOException {
        FeatureService fs = new FeatureServiceImpl();

        URL res1 = getClass().getResource("/features/test-feature.json");
        Feature f1;
        try (Reader r = new InputStreamReader(res1.openStream())) {
            f1 = fs.readFeature(r);
        }

        URL res2 = getClass().getResource("/features/test-feature2.json");
        Feature f2;
        try (Reader r = new InputStreamReader(res2.openStream())) {
            f2 = fs.readFeature(r);
        }

        MergeContext ctx = new MergeContextBuilder()
                .bundleConflictHandler((cf1, b1, cf2, b2) -> Arrays.asList(b1, b2))
                .configConflictHandler((cf1, c1, cf2, c2) -> new ConfigurationBuilder(c1)
                        .addValues(c2.getValues()).build())
                .build();


        ArtifactID tid = new ArtifactID("foo", "bar", "1.2.3");
        Feature f3 = fs.mergeFeatures(tid, f1, f2, ctx);
        assertEquals(tid, f3.getID());

        List<Bundle> bundles = f3.getBundles();
        assertEquals(5, bundles.size());

        assertTrue(bundles.contains(new BundleBuilder(new ArtifactID("org.slf4j", "slf4j-api", "1.7.29")).build()));
        assertTrue(bundles.contains(new BundleBuilder(new ArtifactID("org.slf4j", "slf4j-api", "1.7.30")).build()));
        assertTrue(bundles.contains(new BundleBuilder(new ArtifactID("org.slf4j", "slf4j-nop", "1.7.30")).build()));

        Map<String, Configuration> configs = f3.getConfigurations();
        assertEquals(2, configs.size());

        Configuration cfg1 = configs.get("my.factory.pid~name");
        assertEquals("my.factory.pid~name", cfg1.getPid());
        assertEquals("my.factory.pid", cfg1.getFactoryPid());
        assertEquals(Collections.singletonMap("a.value", "yeah"), cfg1.getValues());

        Configuration cfg2 = configs.get("my.pid");
        assertEquals("my.pid", cfg2.getPid());
        assertNull(cfg2.getFactoryPid());
        Map<String,Object> expected = new HashMap<>();
        expected.put("foo", 5L);
        expected.put("bar", "toast");
        expected.put("number:Integer", 7L); // this is wrong TODO
        assertEquals(expected, cfg2.getValues());
    }

    @Test
    public void testMergeExtensions() throws IOException {
        FeatureService fs = new FeatureServiceImpl();

        URL res1 = getClass().getResource("/features/test-exfeat1.json");
        Feature f1;
        try (Reader r = new InputStreamReader(res1.openStream())) {
            f1 = fs.readFeature(r);
        }

        URL res2 = getClass().getResource("/features/test-exfeat2.json");
        Feature f2;
        try (Reader r = new InputStreamReader(res2.openStream())) {
            f2 = fs.readFeature(r);
        }

        MergeContext ctx = new MergeContextBuilder()
                .extensionConflictHandler((cf1, e1, cf2, e2) ->
                    new ExtensionBuilder(e1.getName(), e1.getType(), e1.getKind())
                        .addText(e1.getText())
                        .addText(e2.getText())
                        .build())
                .build();

        ArtifactID tid = new ArtifactID("g", "a", "1.2.3");
        Feature f3 = fs.mergeFeatures(tid, f1, f2, ctx);

        Map<String, Extension> extensions = f3.getExtensions();
        assertEquals(3, extensions.size());
        Extension txtEx = extensions.get("my-text-ex");
        assertEquals("ABCDEF", txtEx.getText());
        assertEquals(Extension.Kind.OPTIONAL, txtEx.getKind());
        assertEquals(Extension.Type.TEXT, txtEx.getType());

        Extension artEx = extensions.get("my-art-ex");
        assertEquals(Extension.Kind.MANDATORY, artEx.getKind());
        assertEquals(Extension.Type.ARTIFACTS, artEx.getType());
        List<ArtifactID> artifacts = artEx.getArtifacts();
        assertEquals(2, artifacts.size());
        assertTrue(artifacts.contains(new ArtifactID("g", "a", "1")));
        assertTrue(artifacts.contains(new ArtifactID("g", "a", "2")));

        Extension jsonEx = extensions.get("my-json-ex");
        assertEquals(Extension.Kind.TRANSIENT, jsonEx.getKind());
        assertEquals(Extension.Type.JSON, jsonEx.getType());
        assertEquals("{\"foo\":[1,2,3]}", jsonEx.getJSON());
    }
}
