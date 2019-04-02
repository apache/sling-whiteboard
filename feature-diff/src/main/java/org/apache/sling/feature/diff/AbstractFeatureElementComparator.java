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

abstract class AbstractFeatureElementComparator<T, I extends Iterable<T>> implements FeatureElementComparator<T, I> {

    private final String id;

    public AbstractFeatureElementComparator(String id) {
        this.id = id;
    }

    @Override
    public final DiffSection apply(I previouses, I currents) {
        final DiffSection diffDsection = new DiffSection(id);

        previouses.forEach(previous -> {
            T current = find(previous, currents);

            if (current == null) {
                diffDsection.markRemoved(getId(previous));
            } else {
                DiffSection updateSection = compare(previous, current);
                if (!updateSection.isEmpty()) {
                    diffDsection.markUpdated(updateSection);
                }
            }
        });

        currents.forEach(current -> {
            T previous = find(current, previouses);

            if (previous == null) {
                diffDsection.markAdded(getId(current));
            }
        });

        return diffDsection;
    }

}
