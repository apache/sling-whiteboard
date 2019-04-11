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

import static org.apache.sling.feature.apiregions.model.io.json.ApiRegionsJSONParser.parseApiRegions;
import static java.lang.String.format;

import java.io.IOException;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Extensions;
import org.apache.sling.feature.apiregions.model.ApiRegion;
import org.apache.sling.feature.apiregions.model.ApiRegions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;

final class ExtensionsComparator extends AbstractFeatureElementComparator<Extension, Extensions> {

    private static final String API_REGIONS = "api-regions";
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
        String diffName = format("%s:%s|%s", current.getName(), current.getType(), current.isRequired());
        DiffSection diffSection = new DiffSection(diffName);

        if (previous.getType() != current.getType()) {
            diffSection.markItemUpdated("type", previous.getType(), current.getType());
        }

        switch (previous.getType()) {
            case ARTIFACTS:
                diffSection.markUpdated(new ArtifactsComparator("artifacts")
                                        .apply(previous.getArtifacts(), current.getArtifacts()));
                break;

            case TEXT:
                if (!previous.getText().equals(previous.getText())) {
                    diffSection.markItemUpdated("text", previous.getType(), current.getType());
                } 
                break;

            case JSON:
                if (API_REGIONS.equals(current.getName())) {
                    ApiRegions previousRegions = parseApiRegions(previous);
                    ApiRegions currentRegions = parseApiRegions(current);

                    for (ApiRegion previousRegion : previousRegions) {
                        String regionName = previousRegion.getName();
                        ApiRegion currentRegion = currentRegions.getByName(regionName);

                        if (currentRegion == null) {
                            diffSection.markRemoved(regionName);
                        } else {
                            DiffSection regionDiff = new DiffSection(regionName);

                            for (String previousApi : previousRegion.getExports()) {
                                if (!currentRegion.exports(previousApi)) {
                                    regionDiff.markRemoved(previousApi);
                                }
                            }

                            for (String currentApi : currentRegion.getExports()) {
                                if (!previousRegion.exports(currentApi)) {
                                    regionDiff.markAdded(currentApi);
                                }
                            }

                            diffSection.markUpdated(regionDiff);
                        }
                    }

                    for (ApiRegion currentRegion : currentRegions) {
                        String regionName = currentRegion.getName();
                        ApiRegion previousRegion = previousRegions.getByName(regionName);

                        if (previousRegion == null) {
                            diffSection.markAdded(regionName);
                        }
                    }
                } else {
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
                }
                break;

            // it doesn't happen
            default:
                break;
        }

        return diffSection;
    }

}
