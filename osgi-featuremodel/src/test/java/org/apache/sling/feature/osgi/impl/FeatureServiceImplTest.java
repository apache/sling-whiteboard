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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.feature.BuilderFactory;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureBuilder;
import org.osgi.service.feature.FeatureBundle;
import org.osgi.service.feature.FeatureConfiguration;

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
            assertFalse(f.getDocURL().isPresent());
            assertFalse(f.getLicense().isPresent());
            assertFalse(f.getSCM().isPresent());
            assertFalse(f.getVendor().isPresent());

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
            
            Map<String, FeatureConfiguration> configs = f.getConfigurations();
            assertEquals(2, configs.size());
            
            FeatureConfiguration cfg1 = configs.get("my.pid");
            assertEquals("my.pid", cfg1.getPid());
            assertFalse(cfg1.getFactoryPid().isPresent());
            Map<String, Object> values1 = cfg1.getValues();
            assertEquals(3, values1.size());
            assertEquals(Long.valueOf(5), values1.get("foo"));
            assertEquals("test", values1.get("bar"));
            assertEquals(Integer.valueOf(7), values1.get("number"));
            
            FeatureConfiguration cfg2 = configs.get("my.factory.pid~name");
            assertEquals("my.factory.pid~name", cfg2.getPid());
            assertEquals("my.factory.pid", cfg2.getFactoryPid().get());
            Map<String, Object> values2 = cfg2.getValues();
            assertEquals(1, values2.size());
            assertArrayEquals(new String[] {"yeah", "yeah", "yeah"}, (String[]) values2.get("a.value"));
        }
    }
    
    @Test
    public void testReadFeature2() throws Exception {
        URL res = getClass().getResource("/features/test-feature2.json");
        try (Reader r = new InputStreamReader(res.openStream())) {
            Feature f = features.readFeature(r);

            assertEquals("org.apache.sling:test-feature2:osgifeature:cls_abc:1.1", f.getID().toString());
            assertEquals("test-feature2", f.getName().get());
            assertEquals("The feature description", f.getDescription().get());
            assertEquals(List.of("foo", "bar"), f.getCategories());
            assertEquals("http://foo.bar.com/abc", f.getDocURL().get());
            assertEquals("Apache-2.0; link=\"http://opensource.org/licenses/apache2.0.php\"", f.getLicense().get());
            assertEquals("url=https://github.com/apache/sling-aggregator, connection=scm:git:https://github.com/apache/sling-aggregator.git, developerConnection=scm:git:git@github.com:apache/sling-aggregator.git", 
            		f.getSCM().get());
            assertEquals("The Apache Software Foundation", f.getVendor().get());
        }    	
    }

    @Test
    public void testWriteFeature() throws Exception {
        BuilderFactory factory = features.getBuilderFactory();

        String desc = "This is the main ACME app, from where all functionality can be reached.";

        FeatureBuilder builder = factory.newFeatureBuilder(features.getID("org.acme", "acmeapp", "1.0.0"));
        builder.setName("The ACME app");
		builder.setDescription(desc);

        Feature f = builder.build();
        StringWriter sw = new StringWriter();
        features.writeFeature(f, sw);
        
        // Now check the generated JSON
        JsonReader jr = Json.createReader(new StringReader(sw.toString()));
        JsonObject fo = jr.readObject();
        assertEquals("org.acme:acmeapp:1.0.0", fo.getString("id"));
        assertEquals("The ACME app", fo.getString("name"));
        assertEquals(desc, fo.getString("description"));
        assertFalse(fo.containsKey("docURL"));
        assertFalse(fo.containsKey("license"));
        assertFalse(fo.containsKey("scm"));
        assertFalse(fo.containsKey("vendor"));
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
                features.getIDfromMavenCoordinates("org.osgi:org.osgi.util.function:1.1.0"))
                .build();
        FeatureBundle b2 = factory.newBundleBuilder(
        		features.getIDfromMavenCoordinates("org.osgi:org.osgi.util.promise:1.1.1"))
                .build();

        FeatureBundle b3 = factory.newBundleBuilder(
        		features.getIDfromMavenCoordinates("org.apache.commons:commons-email:1.1.5"))
                .addMetadata("org.acme.javadoc.link",
                        "https://commons.apache.org/proper/commons-email/javadocs/api-1.5")
                .build();

        FeatureBundle b4 = factory.newBundleBuilder(
        		features.getIDfromMavenCoordinates("com.acme:acmelib:1.7.2"))
                .build();

        builder.addBundles(b1, b2, b3, b4);

        Feature f = builder.build();
        System.out.println("***" + f);
    }
}
