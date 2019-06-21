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

import static javax.json.Json.createReader;

import java.io.StringReader;
import java.util.LinkedList;

import javax.json.JsonValue;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Extensions;
import org.apache.sling.feature.Feature;

import com.google.auto.service.AutoService;

@AutoService(FeatureElementComparator.class)
public final class ExtensionsComparator extends AbstractFeatureElementComparator {

    public ExtensionsComparator() {
        super("extensions");
    }

    @Override
    public void computeDiff(Feature previous, Feature current, Feature target) {
        computeDiff(previous.getExtensions(), current.getExtensions(), target);
    }

    protected void computeDiff(Extensions previousExtensions, Extensions currentExtensions, Feature target) {
        for (Extension previousExtension : previousExtensions) {
            Extension currentExtension = currentExtensions.getByName(previousExtension.getName());

            if (currentExtension == null) {
                target.getPrototype().getExtensionRemovals().add(previousExtension.getName());
            } else {
                computeDiff(previousExtension, currentExtension, target);
            }
        }

        for (Extension currentExtension : currentExtensions) {
            Extension previousConfiguration = previousExtensions.getByName(currentExtension.getName());

            if (previousConfiguration == null) {
                target.getExtensions().add(currentExtension);
            }
        }
    }

    protected void computeDiff(Extension previousExtension, Extension currentExtension, Feature target) {
        boolean replace = false;

        switch (previousExtension.getType()) {
            case ARTIFACTS:
                Extension targetExtension = new Extension(previousExtension.getType(), previousExtension.getName(), previousExtension.isRequired());

                for (Artifact previous : previousExtension.getArtifacts()) {
                    Artifact current = currentExtension.getArtifacts().getSame(previous.getId());

                    boolean add = false;

                    if (current == null || (add = !previous.getId().equals(current.getId()))) {
                        target.getPrototype().getArtifactExtensionRemovals()
                                             .computeIfAbsent(previousExtension.getName(), k -> new LinkedList<ArtifactId>())
                                             .add(previous.getId());
                    }

                    if (add) {
                        targetExtension.getArtifacts().add(current);
                    }
                }

                for (Artifact current : currentExtension.getArtifacts()) {
                    Artifact previous = previousExtension.getArtifacts().getSame(current.getId());

                    if (previous == null) {
                        targetExtension.getArtifacts().add(current);
                    }
                }

                if (!targetExtension.getArtifacts().isEmpty()) {
                    target.getExtensions().add(targetExtension);
                }

                break;

            case TEXT:
                if (!previousExtension.getText().equals(currentExtension.getText())) {
                    replace = true;
                }
                break;

            case JSON:
                String previousJSON = previousExtension.getJSON();
                String currentJSON = currentExtension.getJSON();

                try {
                    JsonValue previousNode = parseJSON(previousJSON);
                    JsonValue currentNode = parseJSON(currentJSON); 

                    if (!previousNode.equals(currentNode)) {
                        replace = true;
                    }
                } catch (Throwable t) {
                    // should not happen
                    throw new RuntimeException("A JSON parse error occurred while parsing previous '"
                                               + previousJSON
                                               + "' and current '"
                                               + currentJSON
                                               + "', see nested errors:", t);
                }
                break;

            // it doesn't happen
            default:
                break;
        }

        if (replace) {
            target.getPrototype().getExtensionRemovals().add(currentExtension.getName());
            target.getExtensions().add(currentExtension);
        }
    }

    private static JsonValue parseJSON(String json) {
        return createReader(new StringReader(json)).read();
    }

}
