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

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.Artifacts;
import org.apache.sling.feature.Feature;

import com.google.auto.service.AutoService;

@AutoService(FeatureElementComparator.class)
public final class BundlesComparator extends AbstractFeatureElementComparator {

    public BundlesComparator() {
        super("bundles");
    }

    @Override
    public void computeDiff(Feature previous, Feature current, Feature target) {
        computeDiff(previous.getBundles(), current.getBundles(), target);
    }

    protected void computeDiff(Artifacts previouses, Artifacts currents, Feature target) {
        for (Artifact previous : previouses) {
            Artifact current = currents.getSame(previous.getId());

            boolean add = false;

            if (current == null || (add = !previous.getId().equals(current.getId()))) {
                target.getPrototype().getBundleRemovals().add(previous.getId());
            }

            if (add) {
                target.getBundles().add(current);
            }
        }

        for (Artifact current : currents) {
            Artifact previous = previouses.getSame(current.getId());

            if (previous == null) {
                target.getBundles().add(current);
            }
        }
    }

}
