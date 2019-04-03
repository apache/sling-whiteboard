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

import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.builder.EqualsBuilder;

final class GenericMapComparator {

    private final String id;

    public GenericMapComparator(String id) {
        this.id = requireNonNull(id, "Impossible to instantiate a generic Map comparator with null id");
    }

    public <V> DiffSection compare(Map<String, V> previous, Map<String, V> current) {
        DiffSection diffSection = new DiffSection(id);

        for (Entry<String, V> previousEntry : previous.entrySet()) {
            String previousKey = previousEntry.getKey();

            if (!current.containsKey(previousKey)) {
                diffSection.markRemoved(previousKey);
            } else {
                V previousValue = previousEntry.getValue();
                V currentValue = current.get(previousKey);

                if (!new EqualsBuilder().reflectionAppend(previousValue, currentValue).isEquals()) {
                    diffSection.markItemUpdated(previousKey, previousValue, currentValue);
                }
            }
        }

        for (String currentKey : current.keySet()) {
            if (!previous.containsKey(currentKey)) {
                diffSection.markAdded(currentKey);
            }
        }

        return diffSection;
    }


}
