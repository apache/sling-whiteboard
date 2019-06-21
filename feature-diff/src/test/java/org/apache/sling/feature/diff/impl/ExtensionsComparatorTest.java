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

import static org.apache.sling.feature.ExtensionType.ARTIFACTS;
import static org.apache.sling.feature.ExtensionType.JSON;
import static org.apache.sling.feature.ExtensionType.TEXT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Extensions;
import org.junit.Test;

public class ExtensionsComparatorTest extends AbstractComparatorTest<ExtensionsComparator> {

    @Override
    protected ExtensionsComparator newComparatorInstance() {
        return new ExtensionsComparator();
    }

    @Test
    public void checkTextExtensionRemoved() {
        Extension removed = new Extension(TEXT, "removed-TEXT-extension", true);
        removed.setText("This is just a test");
        checkRemovedExtension(removed);
    }

    @Test
    public void checkJSONExtensionRemoved() {
        Extension removed = new Extension(JSON, "removed-JSON-extension", true);
        removed.setJSON("[true, 100, null]");
        checkRemovedExtension(removed);
    }

    @Test
    public void checkArtifactsExtensionRemoved() {
        Extension removed = new Extension(ARTIFACTS, "removed-ARTIFACTS-extension", true);
        removed.getArtifacts().add(new Artifact(ArtifactId.parse("org.apache.sling:org.apache.sling.diff:1.0.0")));
        checkRemovedExtension(removed);
    }

    private void checkRemovedExtension(Extension removedExtension) {
        Extensions previous = new Extensions();
        previous.add(removedExtension);

        Extensions current = new Extensions();

        comparator.computeDiff(previous, current, targetFeature);

        assertTrue(targetFeature.getPrototype().getExtensionRemovals().contains(removedExtension.getName()));
    }

    @Test
    public void checkRemovedArtifacts() {
        Extension previousExtension = new Extension(ARTIFACTS, "content-packages", true);
        ArtifactId removedId = ArtifactId.parse("org.apache.sling:org.apache.sling.diff:1.0.0");
        previousExtension.getArtifacts().add(new Artifact(removedId));

        Extension currentExtension = new Extension(ARTIFACTS, "content-packages", true);

        comparator.computeDiff(previousExtension, currentExtension, targetFeature);

        Map<String, List<ArtifactId>> artifactExtensionRemovals = targetFeature.getPrototype().getArtifactExtensionRemovals(); 
        assertFalse(artifactExtensionRemovals.isEmpty());
        assertTrue(artifactExtensionRemovals.containsKey("content-packages"));
        assertTrue(artifactExtensionRemovals.get("content-packages").contains(removedId));
    }

    @Test
    public void checkTextExtensionUpdated() {
        Extension previous = new Extension(TEXT, "repoinit", true);
        previous.setText("create path /content/example.com(mixin mix:referenceable)");

        Extension current = new Extension(TEXT, "repoinit", true);
        current.setText("create path /content/example.com(mixin mix:referenceable)\ncreate path (nt:unstructured) /var");
        comparator.computeDiff(previous, current, targetFeature);

        assertTrue(targetFeature.getPrototype().getExtensionRemovals().contains(current.getName()));
        assertEquals(current.getText(), targetFeature.getExtensions().getByName(current.getName()).getText());
    }

    @Test
    public void checkJSONExtensionUpdated() {
        Extension previous = new Extension(JSON, "api-regions", true);
        previous.setJSON("{\"name\": \"global\"}");

        Extension current = new Extension(JSON, "api-regions", true);
        current.setJSON("{\"name\": \"deprecated\"}");
        comparator.computeDiff(previous, current, targetFeature);

        assertTrue(targetFeature.getPrototype().getExtensionRemovals().contains(current.getName()));
        assertEquals(current.getJSON(), targetFeature.getExtensions().getByName(current.getName()).getJSON());
    }

    @Test
    public void checkArtifactsExtensionUpdated() {
        Extension previous = new Extension(ARTIFACTS, "content-packages", true);
        ArtifactId removedId = ArtifactId.parse("org.apache.sling:org.apache.sling.diff:1.0.0");
        previous.getArtifacts().add(new Artifact(removedId));

        Extension current = new Extension(ARTIFACTS, "content-packages", true);
        ArtifactId updatedId = ArtifactId.parse("org.apache.sling:org.apache.sling.diff:2.0.0");
        current.getArtifacts().add(new Artifact(updatedId));

        comparator.computeDiff(previous, current, targetFeature);

        Map<String, List<ArtifactId>> artifactExtensionRemovals = targetFeature.getPrototype().getArtifactExtensionRemovals();
        assertTrue(artifactExtensionRemovals.get("content-packages").contains(removedId));
        assertTrue(targetFeature.getExtensions().getByName(current.getName()).getArtifacts().contains(current.getArtifacts().iterator().next()));
    }

}
