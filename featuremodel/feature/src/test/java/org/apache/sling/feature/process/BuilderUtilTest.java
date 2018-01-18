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
package org.apache.sling.feature.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Bundles;
import org.apache.sling.feature.BundlesTest;
import org.apache.sling.feature.process.BuilderUtil.ArtifactMerge;
import org.junit.Test;

public class BuilderUtilTest {

    private List<Map.Entry<Integer, Artifact>> getBundles(final Bundles f) {
        final List<Map.Entry<Integer, Artifact>> result = new ArrayList<>();
        for(final Map.Entry<Integer, List<Artifact>> entry : f.getBundlesByStartOrder().entrySet()) {
            for(final Artifact artifact : entry.getValue()) {
                result.add(new Map.Entry<Integer, Artifact>() {

                    @Override
                    public Integer getKey() {
                        return entry.getKey();
                    }

                    @Override
                    public Artifact getValue() {
                        return artifact;
                    }

                    @Override
                    public Artifact setValue(Artifact value) {
                        return null;
                    }
                });
            }
        }

        return result;
    }

    private void assertContains(final List<Map.Entry<Integer, Artifact>> bundles,
            final int level, final ArtifactId id) {
        for(final Map.Entry<Integer, Artifact> entry : bundles) {
            if ( entry.getKey().intValue() == level
                 && entry.getValue().getId().equals(id) ) {
                return;
            }
        }
        fail(id.toMvnId());
    }

    @Test public void testMergeBundlesWithAlgHighest() {
        final Bundles target = new Bundles();

        target.add(BundlesTest.createBundle("g/a/1.0", 1));
        target.add(BundlesTest.createBundle("g/b/2.0", 2));
        target.add(BundlesTest.createBundle("g/c/2.5", 3));

        final Bundles source = new Bundles();
        source.add(BundlesTest.createBundle("g/a/1.1", 1));
        source.add(BundlesTest.createBundle("g/b/1.9", 2));
        source.add(BundlesTest.createBundle("g/c/2.5", 3));

        BuilderUtil.mergeBundles(target, source, ArtifactMerge.HIGHEST);

        final List<Map.Entry<Integer, Artifact>> result = getBundles(target);
        assertEquals(3, result.size());
        assertContains(result, 1, ArtifactId.parse("g/a/1.1"));
        assertContains(result, 2, ArtifactId.parse("g/b/2.0"));
        assertContains(result, 3, ArtifactId.parse("g/c/2.5"));
    }

    @Test public void testMergeBundlesWithAlgLatest() {
        final Bundles target = new Bundles();

        target.add(BundlesTest.createBundle("g/a/1.0", 1));
        target.add(BundlesTest.createBundle("g/b/2.0", 2));
        target.add(BundlesTest.createBundle("g/c/2.5", 3));

        final Bundles source = new Bundles();
        source.add(BundlesTest.createBundle("g/a/1.1", 1));
        source.add(BundlesTest.createBundle("g/b/1.9", 2));
        source.add(BundlesTest.createBundle("g/c/2.5", 3));

        BuilderUtil.mergeBundles(target, source, ArtifactMerge.LATEST);

        final List<Map.Entry<Integer, Artifact>> result = getBundles(target);
        assertEquals(3, result.size());
        assertContains(result, 1, ArtifactId.parse("g/a/1.1"));
        assertContains(result, 2, ArtifactId.parse("g/b/1.9"));
        assertContains(result, 3, ArtifactId.parse("g/c/2.5"));
    }

    @Test public void testMergeBundlesDifferentStartlevel() {
        final Bundles target = new Bundles();

        target.add(BundlesTest.createBundle("g/a/1.0", 1));

        final Bundles source = new Bundles();
        source.add(BundlesTest.createBundle("g/a/1.1", 2));

        BuilderUtil.mergeBundles(target, source, ArtifactMerge.LATEST);

        final List<Map.Entry<Integer, Artifact>> result = getBundles(target);
        assertEquals(1, result.size());
        assertContains(result, 2, ArtifactId.parse("g/a/1.1"));
    }

    @Test public void testMergeBundles() {
        final Bundles target = new Bundles();

        target.add(BundlesTest.createBundle("g/a/1.0", 1));
        target.add(BundlesTest.createBundle("g/b/2.0", 2));
        target.add(BundlesTest.createBundle("g/c/2.5", 3));

        final Bundles source = new Bundles();
        source.add(BundlesTest.createBundle("g/d/1.1", 1));
        source.add(BundlesTest.createBundle("g/e/1.9", 2));
        source.add(BundlesTest.createBundle("g/f/2.5", 3));

        BuilderUtil.mergeBundles(target, source, ArtifactMerge.LATEST);

        final List<Map.Entry<Integer, Artifact>> result = getBundles(target);
        assertEquals(6, result.size());
        assertContains(result, 1, ArtifactId.parse("g/a/1.0"));
        assertContains(result, 2, ArtifactId.parse("g/b/2.0"));
        assertContains(result, 3, ArtifactId.parse("g/c/2.5"));
        assertContains(result, 1, ArtifactId.parse("g/d/1.1"));
        assertContains(result, 2, ArtifactId.parse("g/e/1.9"));
        assertContains(result, 3, ArtifactId.parse("g/f/2.5"));
    }
}
