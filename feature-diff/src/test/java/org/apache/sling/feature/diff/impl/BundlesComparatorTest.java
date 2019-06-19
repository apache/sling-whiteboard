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
package org.apache.sling.feature.diff.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Artifacts;
import org.apache.sling.feature.diff.impl.BundlesComparator;
import org.junit.Test;

public class BundlesComparatorTest extends AbstractComparatorTest<BundlesComparator> {

    @Override
    protected BundlesComparator newComparatorInstance() {
        return new BundlesComparator();
    }

    @Test
    public void checkRemoved() {
        Artifacts previousArtifacts = new Artifacts();
        Artifact previousArtifact = new Artifact(ArtifactId.fromMvnId("org.apache.sling:org.apache.sling.feature.diff:1.0.0"));
        previousArtifacts.add(previousArtifact);

        Artifacts currentArtifacts = new Artifacts();

        comparator.computeDiff(previousArtifacts, currentArtifacts, targetFeature);

        assertFalse(targetFeature.getPrototype().getBundleRemovals().isEmpty());
        assertEquals(previousArtifact.getId(), targetFeature.getPrototype().getBundleRemovals().iterator().next());
    }

    @Test
    public void checkAdded() {
        Artifacts previousArtifacts = new Artifacts();

        Artifacts currentArtifacts = new Artifacts();
        Artifact currentArtifact = new Artifact(ArtifactId.fromMvnId("org.apache.sling:org.apache.sling.feature.diff:1.0.0"));
        currentArtifacts.add(currentArtifact);

        comparator.computeDiff(previousArtifacts, currentArtifacts, targetFeature);

        assertTrue(targetFeature.getPrototype().getBundleRemovals().isEmpty());
        assertEquals(currentArtifact.getId(), targetFeature.getBundles().iterator().next().getId());
    }

    @Test
    public void checkUpdated() {
        Artifacts previousArtifacts = new Artifacts();
        Artifact previousArtifact = new Artifact(ArtifactId.fromMvnId("org.apache.sling:org.apache.sling.feature.diff:0.0.1"));
        previousArtifacts.add(previousArtifact);

        Artifacts currentArtifacts = new Artifacts();
        Artifact currentArtifact = new Artifact(ArtifactId.fromMvnId("org.apache.sling:org.apache.sling.feature.diff:1.0.0"));
        currentArtifacts.add(currentArtifact);

        comparator.computeDiff(previousArtifacts, currentArtifacts, targetFeature);

        assertFalse(targetFeature.getPrototype().getBundleRemovals().isEmpty());
        assertEquals(previousArtifact.getId(), targetFeature.getPrototype().getBundleRemovals().iterator().next());

        assertFalse(targetFeature.getBundles().isEmpty());
        assertEquals(currentArtifact.getId(), targetFeature.getBundles().iterator().next().getId());
    }

}
