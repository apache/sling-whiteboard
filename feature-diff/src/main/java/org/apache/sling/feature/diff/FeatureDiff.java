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

public final class FeatureDiff {

    private final List<DiffSection> diffSections = new LinkedList<>();

    private FeatureDiff() {
        // this class can not be instantiated from outside
    }

    protected void addSection(DiffSection diffSection) {
        DiffSection checkedDiffSection = requireNonNull(diffSection, "Null diff section can not be added to the resulting diff");
        if (!diffSection.isEmpty()) {
            diffSections.add(checkedDiffSection);
        }
    }

    public Iterable<DiffSection> getSections() {
        return diffSections;
    }

}
