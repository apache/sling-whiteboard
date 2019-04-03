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

import static org.apache.sling.feature.diff.FeatureDiff.compareFeatures;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.junit.Test;

public class FeatureDiffTest {

    @Test(expected = NullPointerException.class)
    public void doesNotAcceptNullPreviousFeature() {
        compareFeatures(null, null);
    }

    @Test(expected = NullPointerException.class)
    public void doesNotAcceptNullCurrentFeature() {
        compareFeatures(new Feature(ArtifactId.fromMvnId("org.apache.sling:org.apache.sling.feature.diff:1.0.0")), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void doesNotAcceptDifferentFeatures() {
        Feature previous = new Feature(ArtifactId.fromMvnId("org.apache.sling:org.apache.sling.feature.apiregions:1.0.0"));
        Feature current = new Feature(ArtifactId.fromMvnId("org.apache.sling:org.apache.sling.feature.diff:1.0.0"));
        compareFeatures(previous, current);
    }

    @Test(expected = IllegalArgumentException.class)
    public void doesNotAcceptSameFeature() {
        Feature previous = new Feature(ArtifactId.fromMvnId("org.apache.sling:org.apache.sling.feature.diff:1.0.0"));
        Feature current = new Feature(ArtifactId.fromMvnId("org.apache.sling:org.apache.sling.feature.diff:1.0.0"));
        compareFeatures(previous, current);
    }

    @Test
    public void keepFeatureInputs() {
        Feature previous = new Feature(ArtifactId.fromMvnId("org.apache.sling:org.apache.sling.feature.diff:0.9.0"));
        Feature current = new Feature(ArtifactId.fromMvnId("org.apache.sling:org.apache.sling.feature.diff:1.0.0"));
        FeatureDiff featureDiff = compareFeatures(previous, current);
        assertTrue(featureDiff.isEmpty());
        assertEquals(previous, featureDiff.getPrevious());
        assertEquals(current, featureDiff.getCurrent());
    }

    @Test
    public void frameworkPropertiesUpdated() {
        Feature previous = new Feature(ArtifactId.fromMvnId("org.apache.sling:org.apache.sling.feature.diff:0.9.0"));
        previous.getFrameworkProperties().put("env", "staging");

        Feature current = new Feature(ArtifactId.fromMvnId("org.apache.sling:org.apache.sling.feature.diff:1.0.0"));
        current.getFrameworkProperties().put("env", "prod");

        FeatureDiff diff = compareFeatures(previous, current);
        assertFalse(diff.isEmpty());

        DiffSection fwPropertiesDiff = diff.getSections().iterator().next();
        assertFalse(fwPropertiesDiff.isEmpty());

        @SuppressWarnings("unchecked") // known type
        UpdatedItem<String> updated = (UpdatedItem<String>) fwPropertiesDiff.getUpdatedItems().iterator().next();
        assertEquals("env", updated.getId());
        assertEquals("staging", updated.getPrevious());
        assertEquals("prod", updated.getCurrent());
    }

}
