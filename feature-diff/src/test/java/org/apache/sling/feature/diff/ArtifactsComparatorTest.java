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
package org.apache.sling.feature.diff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Artifacts;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ArtifactsComparatorTest {

    private ArtifactsComparator comparator;

    @Before
    public void setUp() {
        comparator = new ArtifactsComparator("bundles");
    }

    @After
    public void tearDown() {
        comparator = null;
    }

    @Test(expected = NullPointerException.class)
    public void nullIdNotAcceptedByTheConstructor() {
        new ArtifactsComparator(null);
    }

    @Test
    public void checkRemoved() {
        Artifacts previousArtifacts = new Artifacts();
        Artifact previousArtifact = new Artifact(ArtifactId.fromMvnId("org.apache.sling:org.apache.sling.feature.diff:1.0.0"));
        previousArtifacts.add(previousArtifact);

        Artifacts currentArtifacts = new Artifacts();

        DiffSection artifactsDiff = comparator.apply(previousArtifacts, currentArtifacts);

        assertFalse(artifactsDiff.isEmpty());
        assertEquals(previousArtifact.getId().toMvnId(), artifactsDiff.getRemoved().iterator().next());
    }

    @Test
    public void checkAdded() {
        Artifacts previousArtifacts = new Artifacts();

        Artifacts currentArtifacts = new Artifacts();
        Artifact currentArtifact = new Artifact(ArtifactId.fromMvnId("org.apache.sling:org.apache.sling.feature.diff:1.0.0"));
        currentArtifacts.add(currentArtifact);

        DiffSection artifactsDiff = comparator.apply(previousArtifacts, currentArtifacts);

        assertFalse(artifactsDiff.isEmpty());
        assertEquals(currentArtifact.getId().toMvnId(), artifactsDiff.getAdded().iterator().next());
    }

    @Test
    public void checkUpdated() {
        Artifacts previousArtifacts = new Artifacts();
        Artifact previousArtifact = new Artifact(ArtifactId.fromMvnId("org.apache.sling:org.apache.sling.feature.diff:0.0.1"));
        previousArtifacts.add(previousArtifact);

        Artifacts currentArtifacts = new Artifacts();
        Artifact currentArtifact = new Artifact(ArtifactId.fromMvnId("org.apache.sling:org.apache.sling.feature.diff:1.0.0"));
        currentArtifacts.add(currentArtifact);

        DiffSection artifactsDiff = comparator.apply(previousArtifacts, currentArtifacts);
        assertFalse(artifactsDiff.isEmpty());

        DiffSection artifactDiff = artifactsDiff.getUpdates().iterator().next();
        for (UpdatedItem<?> updatedItem : artifactDiff.getUpdatedItems()) {
            if ("version".equals(updatedItem.getId())) {
                assertEquals(previousArtifact.getId().getVersion(), updatedItem.getPrevious());
                assertEquals(currentArtifact.getId().getVersion(), updatedItem.getCurrent());
            } else if ("start-order".equals(updatedItem.getId())) {
                assertEquals(previousArtifact.getStartOrder(), updatedItem.getPrevious());
                assertEquals(currentArtifact.getStartOrder(), updatedItem.getCurrent());
            }
        }
    }

}
