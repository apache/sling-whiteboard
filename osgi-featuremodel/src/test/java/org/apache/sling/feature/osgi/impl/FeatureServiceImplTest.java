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
package org.apache.sling.feature.osgi.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.feature.BuilderFactory;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureBuilder;
import org.osgi.service.feature.FeatureBundle;

public class FeatureServiceImplTest {
	FeatureServiceImpl features;

    @Before
    public void setUp() {
        features = new FeatureServiceImpl();
    }

    @Test
    public void testReadFeature() throws IOException {
        BuilderFactory bf = features.getBuilderFactory();

        URL res = getClass().getResource("/features/test-feature.json");
        try (Reader r = new InputStreamReader(res.openStream())) {
            Feature f = features.readFeature(r);

            assertTrue(f.getName().isEmpty());
            assertEquals("The feature description", f.getDescription().get());

            List<FeatureBundle> bundles = f.getBundles();
            assertEquals(3, bundles.size());

            FeatureBundle bundle = bf.newBundleBuilder(features.getID("org.osgi", "osgi.promise", "7.0.1"))
                    .addMetadata("hash", "4632463464363646436")
                    .addMetadata("start-order", 1L)
                    .build();

            FeatureBundle ba = bundles.get(0);
            ba.equals(bundle);

            assertTrue(bundles.contains(bundle));
            assertTrue(bundles.contains(bf.newBundleBuilder(features.getID("org.slf4j", "slf4j-api", "1.7.29")).build()));
            assertTrue(bundles.contains(bf.newBundleBuilder(features.getID("org.slf4j", "slf4j-simple", "1.7.29")).build()));
        }
    }

    @Test
    public void testCreateFeature() {
        BuilderFactory factory = features.getBuilderFactory();

        FeatureBuilder builder = factory.newFeatureBuilder(features.getID("org.acme", "acmeapp", "1.0.0"));
        builder.setName("The ACME app");
        builder.setDescription("This is the main ACME app, "
                + "from where all functionality can be reached.");

        Feature f = builder.build();
        System.out.println("***" + f);
    }

    @Test
    public void testCreateFeatureBundle() {
        BuilderFactory factory = features.getBuilderFactory();

        FeatureBuilder builder = factory.newFeatureBuilder(
        		features.getID("org.acme", "acmeapp", "1.0.1"));
        builder.setName("The Acme Application");
        builder.setLicense("https://opensource.org/licenses/Apache-2.0");
        builder.setComplete(true);

        FeatureBundle b1 = factory.newBundleBuilder(
                features.getIDfromMavenID("org.osgi:org.osgi.util.function:1.1.0"))
                .build();
        FeatureBundle b2 = factory.newBundleBuilder(
        		features.getIDfromMavenID("org.osgi:org.osgi.util.promise:1.1.1"))
                .build();

        FeatureBundle b3 = factory.newBundleBuilder(
        		features.getIDfromMavenID("org.apache.commons:commons-email:1.1.5"))
                .addMetadata("org.acme.javadoc.link",
                        "https://commons.apache.org/proper/commons-email/javadocs/api-1.5")
                .build();

        FeatureBundle b4 = factory.newBundleBuilder(
        		features.getIDfromMavenID("com.acme:acmelib:1.7.2"))
                .build();

        builder.addBundles(b1, b2, b3, b4);

        Feature f = builder.build();
        System.out.println("***" + f);
    }
}
