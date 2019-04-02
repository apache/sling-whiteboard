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

import java.io.IOException;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Extensions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;

final class ExtensionsComparator extends AbstractFeatureElementComparator<Extension, Extensions> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExtensionsComparator() {
        super("extensions");
    }

    @Override
    public String getId(Extension extension) {
        return extension.getName();
    }

    @Override
    public Extension find(Extension extension, Extensions extensions) {
        return extensions.getByName(extension.getName());
    }

    @Override
    public DiffSection compare(Extension previous, Extension current) {
        DiffSection diffSection = new DiffSection(current.getName());

        if (previous.getType() != current.getType()) {
            diffSection.markItemUpdated("type", previous.getType(), current.getType());
        }

        switch (previous.getType()) {
            case ARTIFACTS:
                return new ArtifactsComparator("artifacts").apply(previous.getArtifacts(), current.getArtifacts());

            case TEXT:
                if (!previous.getText().equals(previous.getText())) {
                    diffSection.markItemUpdated("text", previous.getType(), current.getType());
                } 
                break;

            case JSON:
                String previousJSON = previous.getJSON();
                String currentJSON = current.getJSON();

                try {
                    JsonNode previousNode = objectMapper.readTree(previousJSON);
                    JsonNode currentNode = objectMapper.readTree(currentJSON); 
                    JsonNode patchNode = JsonDiff.asJson(previousNode, currentNode); 

                    if (patchNode.size() != 0) {
                        diffSection.markItemUpdated("json", previousJSON, currentJSON);
                    }
                } catch (IOException e) {
                    // should not happen
                    throw new RuntimeException("A JSON parse error occurred while parsing previous '"
                                               + previousJSON
                                               + "' and current '"
                                               + currentJSON
                                               + "', see nested errors:", e);
                }
                break;

            // it doesn't happen
            default:
                break;
        }

        return diffSection;
    }

}
