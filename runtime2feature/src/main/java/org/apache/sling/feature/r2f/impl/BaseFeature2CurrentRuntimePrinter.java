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
package org.apache.sling.feature.r2f.impl;

import static org.apache.sling.feature.diff.FeatureDiff.compareFeatures;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.diff.DefaultDiffRequest;
import org.apache.sling.feature.r2f.RuntimeEnvironment2FeatureModel;
import org.osgi.framework.BundleContext;

public class BaseFeature2CurrentRuntimePrinter extends AbstractRuntimeEnvironment2FeatureModelPrinter {

    public BaseFeature2CurrentRuntimePrinter(RuntimeEnvironment2FeatureModel generator, BundleContext bundleContext) {
        super(generator, bundleContext);
    }

    @Override
    protected Feature compute(Feature currentFeature) {
        // TODO
        Feature previousFeature = null;

        StringBuilder classifier = new StringBuilder()
                                   .append(previousFeature.getId().getVersion())
                                   .append("-to-")
                                   .append(currentFeature.getId().getVersion())
                                   .append('-')
                                   .append(currentFeature.getId().getClassifier())
                                   .append("-upgrade");

        Feature featureDiff = compareFeatures(new DefaultDiffRequest()
                                              .setPrevious(previousFeature)
                                              .setCurrent(currentFeature)
                                              .addIncludeComparator("artifacts")
                                              .addIncludeComparator("configurations")
                                              .setResultId(new ArtifactId(currentFeature.getId().getGroupId(),
                                                           currentFeature.getId().getArtifactId(), 
                                                           currentFeature.getId().getVersion(),
                                                           classifier.toString(),
                                                           currentFeature.getId().getType())));

        return featureDiff;
    }

}
