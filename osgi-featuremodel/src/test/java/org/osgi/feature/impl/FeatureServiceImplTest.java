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
import org.osgi.feature.Feature;
import org.osgi.feature.FeatureService;
import org.osgi.feature.MergeContext;
import org.osgi.feature.builder.MergeContextBuilder;
import org.osgi.feature.impl.FeatureServiceImpl;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

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

            Bundle bundle = new Bundle.Builder("org.osgi", "osgi.promise", "7.0.1")
                    .addMetadata("hash", "4632463464363646436")
                    .addMetadata("start-order", 1L)
                    .build();

            Bundle ba = bundles.get(0);
            ba.equals(bundle);

            assertTrue(bundles.contains(bundle));
            assertTrue(bundles.contains(new Bundle.Builder("org.slf4j", "slf4j-api", "1.7.29").build()));
            assertTrue(bundles.contains(new Bundle.Builder("org.slf4j", "slf4j-simple", "1.7.29").build()));
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

        URL res2 = getClass().getResource("/features/test-feature.json");
        Feature f2;
        try (Reader r = new InputStreamReader(res2.openStream())) {
            f2 = fs.readFeature(r);
        }

        MergeContext ctx = new MergeContextBuilder()
                .setBundleResolver((b1, b2) -> Arrays.asList(b1, b2))
                .build();
        ArtifactID tid = new ArtifactID("foo", "bar", "1.2.3");
        Feature f3 = fs.mergeFeatures(tid, f1, f2, ctx);
        assertEquals(tid, f3.getID());

        List<Bundle> bundles = f3.getBundles();
        assertEquals(4, bundles.size());

        assertTrue(bundles.contains(new Bundle.Builder("org.slf4j", "slf4j-api", "1.7.29").build()));
        assertTrue(bundles.contains(new Bundle.Builder("org.slf4j", "slf4j-api", "1.7.30").build()));
    }
}
