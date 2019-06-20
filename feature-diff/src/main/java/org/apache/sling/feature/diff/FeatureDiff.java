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

import static java.util.Objects.requireNonNull;
import static java.util.ServiceLoader.load;

import java.util.Collection;
import java.util.LinkedList;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.Prototype;
import org.apache.sling.feature.diff.impl.FeatureElementComparator;

public final class FeatureDiff {

    private static final String UPDATER_CLASSIFIER = "updater";

    public static Feature compareFeatures(DiffRequest diffRequest) {
        requireNonNull(diffRequest, "Impossible to compare features without specifying them.");
        Feature previous = requireNonNull(diffRequest.getPrevious(), "Impossible to compare null previous feature.");
        Feature current = requireNonNull(diffRequest.getCurrent(), "Impossible to compare null current feature.");

        if (previous.getId().equals(current.getId())) {
            throw new IllegalArgumentException("Input Features refer to the the same Feature version.");
        }

        StringBuilder classifier = new StringBuilder();
        if (current.getId().getClassifier() != null && !current.getId().getClassifier().isEmpty()) {
            classifier.append(current.getId().getClassifier())
                      .append('_');
        }
        classifier.append(UPDATER_CLASSIFIER);

        ArtifactId resultId = new ArtifactId(current.getId().getGroupId(),
                                             current.getId().getArtifactId(), 
                                             current.getId().getVersion(),
                                             classifier.toString(),
                                             current.getId().getType());

        Feature target = new Feature(resultId);
        target.setTitle(previous.getId() + " to " + current.getId());
        target.setDescription("Computed " + previous.getId() + " to " + current.getId() + " Feature update");

        Prototype prototype = new Prototype(previous.getId());
        target.setPrototype(prototype);

        for (FeatureElementComparator comparator : loadComparators(diffRequest)) {
            comparator.computeDiff(previous, current, target);
        }

        return target;
    }

    protected static Iterable<FeatureElementComparator> loadComparators(DiffRequest diffRequest) {
        Collection<FeatureElementComparator> filteredComparators = new LinkedList<>();

        for (FeatureElementComparator comparator : load(FeatureElementComparator.class)) {
            boolean included = !diffRequest.getIncludeComparators().isEmpty() ? diffRequest.getIncludeComparators().contains(comparator.getId()) : true;
            boolean excluded = diffRequest.getExcludeComparators().contains(comparator.getId());

            if (included && !excluded) {
                filteredComparators.add(comparator);
            }
        }

        return filteredComparators;
    }

    /**
     * this class must not be instantiated directly
     */
    private FeatureDiff() {
        // do nothing
    }

}
