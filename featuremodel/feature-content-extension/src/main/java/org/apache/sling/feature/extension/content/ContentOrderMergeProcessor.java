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
package org.apache.sling.feature.extension.content;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.FeatureConstants;
import org.apache.sling.feature.KeyValueMap;
import org.apache.sling.feature.builder.FeatureExtensionHandler;

public class ContentOrderMergeProcessor implements FeatureExtensionHandler {
    
    private static final String DEFAULT_CONTENT_START_ORDER = "default.content.startorder";

    /**
     * Only postprocessing - relying on default merge strategy
     * (non-Javadoc)
     * @see org.apache.sling.feature.builder.FeatureExtensionHandler#canMerge(org.apache.sling.feature.Extension)
     */
    @Override
    public boolean canMerge(Extension extension) {
        return false;
    }

    /*
     * Only postprocessing - relying on default merge strategy
     * (non-Javadoc)
     * @see org.apache.sling.feature.builder.FeatureExtensionHandler#merge(org.apache.sling.feature.Feature, org.apache.sling.feature.Feature, org.apache.sling.feature.Extension)
     */
    @Override
    public void merge(Feature target, Feature source, Extension extension) {
        // not merging
    }

    @Override
    public void postProcess(Feature feature, Extension extension) {
        if (extension.getType() == ExtensionType.ARTIFACTS
                && extension.getName().equals(FeatureConstants.EXTENSION_NAME_CONTENT_PACKAGES)) {
            String defaultOrder = feature.getVariables().get(DEFAULT_CONTENT_START_ORDER);
            if (defaultOrder != null) {
                for (Artifact a : extension.getArtifacts()) {
                    KeyValueMap kvm = a.getMetadata();
                    if(kvm.get(Artifact.KEY_START_ORDER) == null) {
                        kvm.put(Artifact.KEY_START_ORDER, defaultOrder);
                    }
                }
                feature.getVariables().remove(DEFAULT_CONTENT_START_ORDER);
            }
        }
    }
}
