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

import java.util.LinkedList;
import java.util.List;

import org.apache.sling.feature.Feature;

public final class FeatureDiff {

    public static FeatureDiff compareFeatures(Feature previous, Feature current) {
        previous = requireNonNull(previous, "Impossible to compare null previous feature.");
        current = requireNonNull(current, "Impossible to compare null current feature.");

        if (!previous.getId().isSame(current.getId())) {
            throw new IllegalArgumentException("Feature comparison has to be related to different versions of the same Feature.");
        }

        if (previous.getId().equals(current.getId())) {
            throw new IllegalArgumentException("Input Features refer to the the same Feature version.");
        }

        FeatureDiff featureDiff = new FeatureDiff(previous, current);

        featureDiff.addSection(new GenericMapComparator("framework-properties").compare(previous.getFrameworkProperties(), current.getFrameworkProperties()));
        featureDiff.addSection(new ArtifactsComparator("bundles").apply(previous.getBundles(), current.getBundles()));
        featureDiff.addSection(new ConfigurationsComparator().apply(previous.getConfigurations(), current.getConfigurations()));
        featureDiff.addSection(new RequirementsComparator().apply(previous.getRequirements(), current.getRequirements()));
        featureDiff.addSection(new ExtensionsComparator().apply(previous.getExtensions(), current.getExtensions()));
        featureDiff.addSection(new GenericMapComparator("variables").compare(previous.getVariables(), current.getVariables()));

        return featureDiff;
    }

    private final List<DiffSection> diffSections = new LinkedList<>();

    private final Feature previous;

    private final Feature current;

    // this class can not be instantiated from outside
    private FeatureDiff(Feature previous, Feature current) {
        this.previous = previous;
        this.current = current;
    }

    public Feature getPrevious() {
        return previous;
    }

    public Feature getCurrent() {
        return current;
    }

    protected void addSection(DiffSection diffSection) {
        DiffSection checkedDiffSection = requireNonNull(diffSection, "Null diff section can not be added to the resulting diff");
        if (!diffSection.isEmpty()) {
            diffSections.add(checkedDiffSection);
        }
    }

    public boolean isEmpty() {
        return diffSections.isEmpty();
    }

    public Iterable<DiffSection> getSections() {
        return diffSections;
    }

}
