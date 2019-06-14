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

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Configurations;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.diff.spi.FeatureElementComparator;

final class ConfigurationsComparator implements FeatureElementComparator {

    @Override
    public String getId() {
        return "configurations";
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
            } else if (!reflectionEquals(previousConfiguration.getConfigurationProperties(),
                                         currentConfiguration.getConfigurationProperties(),
                                         true)) {
                target.getConfigurations().add(currentConfiguration);
            }
        }

        for (Configuration currentConfiguration : currents) {
            Configuration previousConfiguration = previouses.getConfiguration(currentConfiguration.getPid());

            if (previousConfiguration == null) {
                target.getConfigurations().add(currentConfiguration);
            }
        }
    }

}
