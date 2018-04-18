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
package org.apache.sling.feature.builder;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Bundles;
import org.apache.sling.feature.builder.BuilderUtil;
import org.apache.sling.feature.builder.BuilderUtil.ArtifactMerge;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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

        target.add(createBundle("g/a/1.0", 1));
        target.add(createBundle("g/b/2.0", 2));
        target.add(createBundle("g/c/2.5", 3));

        final Bundles source = new Bundles();
        source.add(createBundle("g/a/1.1", 1));
        source.add(createBundle("g/b/1.9", 2));
        source.add(createBundle("g/c/2.5", 3));

        BuilderUtil.mergeBundles(target, source, ArtifactMerge.HIGHEST);

        final List<Map.Entry<Integer, Artifact>> result = getBundles(target);
        assertEquals(3, result.size());
        assertContains(result, 1, ArtifactId.parse("g/a/1.1"));
        assertContains(result, 2, ArtifactId.parse("g/b/2.0"));
        assertContains(result, 3, ArtifactId.parse("g/c/2.5"));
    }

    @Test public void testMergeBundlesWithAlgLatest() {
        final Bundles target = new Bundles();

        target.add(createBundle("g/a/1.0", 1));
        target.add(createBundle("g/b/2.0", 2));
        target.add(createBundle("g/c/2.5", 3));

        final Bundles source = new Bundles();
        source.add(createBundle("g/a/1.1", 1));
        source.add(createBundle("g/b/1.9", 2));
        source.add(createBundle("g/c/2.5", 3));

        BuilderUtil.mergeBundles(target, source, ArtifactMerge.LATEST);

        final List<Map.Entry<Integer, Artifact>> result = getBundles(target);
        assertEquals(3, result.size());
        assertContains(result, 1, ArtifactId.parse("g/a/1.1"));
        assertContains(result, 2, ArtifactId.parse("g/b/1.9"));
        assertContains(result, 3, ArtifactId.parse("g/c/2.5"));
    }

    @Test public void testMergeBundlesDifferentStartlevel() {
        final Bundles target = new Bundles();

        target.add(createBundle("g/a/1.0", 1));

        final Bundles source = new Bundles();
        source.add(createBundle("g/a/1.1", 2));

        BuilderUtil.mergeBundles(target, source, ArtifactMerge.LATEST);

        final List<Map.Entry<Integer, Artifact>> result = getBundles(target);
        assertEquals(1, result.size());
        assertContains(result, 2, ArtifactId.parse("g/a/1.1"));
    }

    @Test public void testMergeBundles() {
        final Bundles target = new Bundles();

        target.add(createBundle("g/a/1.0", 1));
        target.add(createBundle("g/b/2.0", 2));
        target.add(createBundle("g/c/2.5", 3));

        final Bundles source = new Bundles();
        source.add(createBundle("g/d/1.1", 1));
        source.add(createBundle("g/e/1.9", 2));
        source.add(createBundle("g/f/2.5", 3));

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

    public static Artifact createBundle(final String id, final int startOrder) {
        final Artifact a = new Artifact(ArtifactId.parse(id));
        a.getMetadata().put(Artifact.KEY_START_ORDER, String.valueOf(startOrder));

        return a;
    }
}
