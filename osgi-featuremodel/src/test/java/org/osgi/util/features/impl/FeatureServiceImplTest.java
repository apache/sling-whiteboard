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
package org.osgi.util.features.impl;

import org.junit.Test;
import org.osgi.util.features.BuilderFactory;
import org.osgi.util.features.Feature;
import org.osgi.util.features.FeatureBuilder;
import org.osgi.util.features.FeatureBundle;
import org.osgi.util.features.FeatureConfiguration;
import org.osgi.util.features.FeatureExtension;
import org.osgi.util.features.Features;
import org.osgi.util.features.ID;
import org.osgi.util.features.MergeContext;

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
        BuilderFactory bf = Features.getBuilderFactory();

        URL res = getClass().getResource("/features/test-feature.json");
        try (Reader r = new InputStreamReader(res.openStream())) {
            Feature f = Features.readFeature(r);

            assertNull(f.getName());
            assertEquals("The feature description", f.getDescription());

            List<FeatureBundle> bundles = f.getBundles();
            assertEquals(3, bundles.size());

            FeatureBundle bundle = bf.newBundleBuilder(new ID("org.osgi", "osgi.promise", "7.0.1"))
                    .addMetadata("hash", "4632463464363646436")
                    .addMetadata("start-order", 1L)
                    .build();

            FeatureBundle ba = bundles.get(0);
            ba.equals(bundle);

            assertTrue(bundles.contains(bundle));
            assertTrue(bundles.contains(bf.newBundleBuilder(new ID("org.slf4j", "slf4j-api", "1.7.29")).build()));
            assertTrue(bundles.contains(bf.newBundleBuilder(new ID("org.slf4j", "slf4j-simple", "1.7.29")).build()));
        }
    }

    @Test
    public void testMergeFeatures() throws IOException {
        BuilderFactory bf = Features.getBuilderFactory();

        URL res1 = getClass().getResource("/features/test-feature.json");
        Feature f1;
        try (Reader r = new InputStreamReader(res1.openStream())) {
            f1 = Features.readFeature(r);
        }

        URL res2 = getClass().getResource("/features/test-feature2.json");
        Feature f2;
        try (Reader r = new InputStreamReader(res2.openStream())) {
            f2 = Features.readFeature(r);
        }

        MergeContext ctx = bf.newMergeContextBuilder()
                .bundleConflictHandler((cf1, b1, cf2, b2) -> Arrays.asList(b1, b2))
                .configConflictHandler((cf1, c1, cf2, c2) -> new ConfigurationBuilderImpl(c1)
                        .addValues(c2.getValues()).build())
                .build();


        ID tid = new ID("foo", "bar", "1.2.3");
        Feature f3 = Features.mergeFeatures(tid, f1, f2, ctx);
        assertEquals(tid, f3.getID());

        List<FeatureBundle> bundles = f3.getBundles();
        assertEquals(5, bundles.size());

        assertTrue(bundles.contains(bf.newBundleBuilder(new ID("org.slf4j", "slf4j-api", "1.7.29")).build()));
        assertTrue(bundles.contains(bf.newBundleBuilder(new ID("org.slf4j", "slf4j-api", "1.7.30")).build()));
        assertTrue(bundles.contains(bf.newBundleBuilder(new ID("org.slf4j", "slf4j-nop", "1.7.30")).build()));

        Map<String, FeatureConfiguration> configs = f3.getConfigurations();
        assertEquals(2, configs.size());

        FeatureConfiguration cfg1 = configs.get("my.factory.pid~name");
        assertEquals("my.factory.pid~name", cfg1.getPid());
        assertEquals("my.factory.pid", cfg1.getFactoryPid());
        assertEquals(Collections.singletonMap("a.value", "yeah"), cfg1.getValues());

        FeatureConfiguration cfg2 = configs.get("my.pid");
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
        BuilderFactory bf = Features.getBuilderFactory();

        URL res1 = getClass().getResource("/features/test-exfeat1.json");
        Feature f1;
        try (Reader r = new InputStreamReader(res1.openStream())) {
            f1 = Features.readFeature(r);
        }

        URL res2 = getClass().getResource("/features/test-exfeat2.json");
        Feature f2;
        try (Reader r = new InputStreamReader(res2.openStream())) {
            f2 = Features.readFeature(r);
        }

        MergeContext ctx = bf.newMergeContextBuilder()
                .extensionConflictHandler((cf1, e1, cf2, e2) ->
                    new ExtensionBuilderImpl(e1.getName(), e1.getType(), e1.getKind())
                        .addText(e1.getText())
                        .addText(e2.getText())
                        .build())
                .build();

        ID tid = new ID("g", "a", "1.2.3");
        Feature f3 = Features.mergeFeatures(tid, f1, f2, ctx);

        Map<String, FeatureExtension> extensions = f3.getExtensions();
        assertEquals(3, extensions.size());
        FeatureExtension txtEx = extensions.get("my-text-ex");
        assertEquals("ABCDEF", txtEx.getText());
        assertEquals(FeatureExtension.Kind.OPTIONAL, txtEx.getKind());
        assertEquals(FeatureExtension.Type.TEXT, txtEx.getType());

        FeatureExtension artEx = extensions.get("my-art-ex");
        assertEquals(FeatureExtension.Kind.MANDATORY, artEx.getKind());
        assertEquals(FeatureExtension.Type.ARTIFACTS, artEx.getType());
        List<ID> artifacts = artEx.getArtifacts();
        assertEquals(2, artifacts.size());
        assertTrue(artifacts.contains(new ID("g", "a", "1")));
        assertTrue(artifacts.contains(new ID("g", "a", "2")));

        FeatureExtension jsonEx = extensions.get("my-json-ex");
        assertEquals(FeatureExtension.Kind.TRANSIENT, jsonEx.getKind());
        assertEquals(FeatureExtension.Type.JSON, jsonEx.getType());
        assertEquals("{\"foo\":[1,2,3]}", jsonEx.getJSON());
    }

    @Test
    public void testCreateFeature() {
        BuilderFactory factory = Features.getBuilderFactory();

        FeatureBuilder builder = factory.newFeatureBuilder(new ID("org.acme", "acmeapp", "1.0.0"));
        builder.setName("The ACME app");
        builder.setDescription("This is the main ACME app, "
                + "from where all functionality can be reached.");

        Feature f = builder.build();
        System.out.println("***" + f);
    }

    @Test
    public void testCreateFeatureBundle() {
        BuilderFactory factory = Features.getBuilderFactory();

        FeatureBuilder builder = factory.newFeatureBuilder(
            new ID("org.acme", "acmeapp", "1.0.1"));
        builder.setName("The Acme Application");
        builder.setLicense("https://opensource.org/licenses/Apache-2.0");
        builder.setComplete(true);

        FeatureBundle b1 = factory.newBundleBuilder(
                ID.fromMavenID("org.osgi:org.osgi.util.function:1.1.0"))
                .build();
        FeatureBundle b2 = factory.newBundleBuilder(
                ID.fromMavenID("org.osgi:org.osgi.util.promise:1.1.1"))
                .build();

        FeatureBundle b3 = factory.newBundleBuilder(
                ID.fromMavenID("org.apache.commons:commons-email:1.1.5"))
                .addMetadata("org.acme.javadoc.link",
                        "https://commons.apache.org/proper/commons-email/javadocs/api-1.5")
                .build();

        FeatureBundle b4 = factory.newBundleBuilder(
                ID.fromMavenID("com.acme:acmelib:1.7.2"))
                .build();

        builder.addBundles(b1, b2, b3, b4);

        Feature f = builder.build();
        System.out.println("***" + f);
    }
}
