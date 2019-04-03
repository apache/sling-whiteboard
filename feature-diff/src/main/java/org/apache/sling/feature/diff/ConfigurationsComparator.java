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

import java.util.Dictionary;
import java.util.Enumeration;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Configurations;

final class ConfigurationsComparator extends AbstractFeatureElementComparator<Configuration, Configurations> {

    public ConfigurationsComparator() {
        super("configurations");
    }

    @Override
    public String getId(Configuration configuration) {
        return configuration.getPid();
    }

    @Override
    public Configuration find(Configuration configuration, Configurations configurations) {
        return configurations.getConfiguration(getId(configuration));
    }

    @Override
    public DiffSection compare(Configuration previous, Configuration current) {
        Dictionary<String, Object> previousProperties = previous.getConfigurationProperties();
        Dictionary<String, Object> currentProperties = current.getConfigurationProperties();
        final DiffSection dictionaryDiffs = new DiffSection(getId(current));

        Enumeration<String> previousKeys = previousProperties.keys();
        while (previousKeys.hasMoreElements()) {
            String previousKey = previousKeys.nextElement();

            Object previousValue = previousProperties.get(previousKey);
            Object currentValue = currentProperties.get(previousKey);

            if (currentValue == null && previousValue != null) {
                dictionaryDiffs.markRemoved(previousKey);
            } else if (!new EqualsBuilder().reflectionAppend(previousValue, currentValue).isEquals()) {
                dictionaryDiffs.markItemUpdated(previousKey, previousValue, currentValue);
            }
        }

        Enumeration<String> currentKeys = currentProperties.keys();
        while (currentKeys.hasMoreElements()) {
            String currentKey = currentKeys.nextElement();

            Object previousValue = previousProperties.get(currentKey);
            Object currentValue = currentProperties.get(currentKey);

            if (previousValue == null && currentValue != null) {
                dictionaryDiffs.markAdded(currentKey);
            }
        }

        return dictionaryDiffs;
    }

}
