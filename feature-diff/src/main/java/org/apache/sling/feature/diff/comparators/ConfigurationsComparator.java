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
package org.apache.sling.feature.diff.comparators;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;

import java.util.Dictionary;
import java.util.Enumeration;

import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Configurations;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.diff.spi.FeatureElementComparator;

import com.google.auto.service.AutoService;

@AutoService(FeatureElementComparator.class)
public final class ConfigurationsComparator extends AbstractFeatureElementComparator {

    public ConfigurationsComparator() {
        super("configurations");
    }

    @Override
    public void computeDiff(Feature previous, Feature current, Feature target) {
        computeDiff(previous.getConfigurations(), current.getConfigurations(), target);
    }

    protected void computeDiff(Configurations previouses, Configurations currents, Feature target) {
        for (Configuration previousConfiguration : previouses) {
            Configuration currentConfiguration = currents.getConfiguration(previousConfiguration.getPid());

            if (currentConfiguration == null) {
                target.getPrototype().getConfigurationRemovals().add(previousConfiguration.getPid());
            } else {
                computeDiff(previousConfiguration, currentConfiguration, target);
            }
        }

        for (Configuration currentConfiguration : currents) {
            Configuration previousConfiguration = previouses.getConfiguration(currentConfiguration.getPid());

            if (previousConfiguration == null) {
                target.getConfigurations().add(currentConfiguration);
            }
        }
    }

    protected void computeDiff(Configuration previous, Configuration current, Feature target) {
        Dictionary<String, Object> previousProperties = previous.getProperties();
        Dictionary<String, Object> currentProperties = current.getProperties();

        Configuration targetConfiguration = new Configuration(previous.getPid());
        Dictionary<String, Object> targetProperties = targetConfiguration.getProperties();

        Enumeration<String> previousKeys = previousProperties.keys();
        while (previousKeys.hasMoreElements()) {
            String previousKey = previousKeys.nextElement();

            Object previousValue = previousProperties.get(previousKey);
            Object currentValue = currentProperties.get(previousKey);

            if (currentValue != null && !reflectionEquals(previousValue, currentValue, true)) {
                targetProperties.put(previousKey, currentValue);
            }
        }

        Enumeration<String> currentKeys = currentProperties.keys();
        while (currentKeys.hasMoreElements()) {
            String currentKey = currentKeys.nextElement();

            Object previousValue = previousProperties.get(currentKey);
            Object currentValue = currentProperties.get(currentKey);

            if (previousValue == null && currentValue != null) {
                targetProperties.put(currentKey, currentValue);
            }
        }

        if (!targetProperties.isEmpty()) {
            target.getConfigurations().add(targetConfiguration);
        }
    }

}
