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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.sling.feature.Feature;

import com.google.auto.service.AutoService;

@AutoService(FeatureElementComparator.class)
public final class FrameworkPropertiesComparator extends AbstractFeatureElementComparator {

    public FrameworkPropertiesComparator() {
        super("framework-properties");
    }

    @Override
    public void computeDiff(Feature previous, Feature current, Feature target) {
        computeDiff(previous.getFrameworkProperties(), current.getFrameworkProperties(), target);
    }

    public void computeDiff(Map<String, String> previous, Map<String, String> current, Feature target) {
        for (Entry<String, String> previousEntry : previous.entrySet()) {
            String previousKey = previousEntry.getKey();

            if (!current.containsKey(previousKey)) {
                target.getPrototype().getFrameworkPropertiesRemovals().add(previousKey);
            } else {
                String previousValue = previousEntry.getValue();
                String currentValue = current.get(previousKey);

                // override the previous set value
                if (!Objects.equals(previousValue, currentValue)) {
                    target.getFrameworkProperties().put(previousKey, currentValue);
                }
            }
        }

        for (Entry<String, String> currentEntry : current.entrySet()) {
            if (!previous.containsKey(currentEntry.getKey())) {
                target.getFrameworkProperties().put(currentEntry.getKey(), currentEntry.getValue());
            }
        }
    }

}
