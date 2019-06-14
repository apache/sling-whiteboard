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

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.Prototype;
import org.apache.sling.feature.diff.spi.FeatureElementComparator;

public final class FeatureDiff {

    public static Feature compareFeatures(Feature previous, Feature current, String resultId) {
        resultId = requireNonNull(resultId, "Impossible to create the Feature diff with a null id");

        ArtifactId id = ArtifactId.parse(resultId);
        return compareFeatures(previous, current, id);
    }

    public static Feature compareFeatures(Feature previous, Feature current, ArtifactId resultId) {
        previous = requireNonNull(previous, "Impossible to compare null previous feature.");
        current = requireNonNull(current, "Impossible to compare null current feature.");

        if (previous.getId().equals(current.getId())) {
            throw new IllegalArgumentException("Input Features refer to the the same Feature version.");
        }

        resultId = requireNonNull(resultId, "Impossible to create the Feature diff with a null id");

        Feature target = new Feature(resultId);
        target.setTitle(previous.getId() + " to " + current.getId());
        target.setDescription(previous.getId() + " to " + current.getId() + " Feature upgrade");

        Prototype prototype = new Prototype(previous.getId());
        target.setPrototype(prototype);

        for (FeatureElementComparator comparator : load(FeatureElementComparator.class)) {
            comparator.computeDiff(previous, current, target);
        }

        return target;
    }

    /**
     * this class must not be instantiated directly
     */
    private FeatureDiff() {
        // do nothing
    }

}
