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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Dictionary;

import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Configurations;
import org.apache.sling.feature.diff.impl.ConfigurationsComparator;
import org.junit.Test;

public class ConfigurationsComparatorTest extends AbstractComparatorTest<ConfigurationsComparator> {

    @Override
    protected ConfigurationsComparator newComparatorInstance() {
        return new ConfigurationsComparator();
    }

    @Test
    public void checkRemoved() {
        Configuration previousConfiguration = new Configuration("org.apache.sling.feature.diff.config");
        Configurations previousConfigurations = new Configurations();
        previousConfigurations.add(previousConfiguration);

        Configurations currentConfigurations = new Configurations();

        comparator.computeDiff(previousConfigurations, currentConfigurations, targetFeature);

        assertFalse(targetFeature.getPrototype().getConfigurationRemovals().isEmpty());
        assertEquals(previousConfiguration.getPid(), targetFeature.getPrototype().getConfigurationRemovals().iterator().next());
        assertTrue(targetFeature.getConfigurations().isEmpty());
    }

    @Test
    public void checkAdded() {
        Configurations previousConfigurations = new Configurations();

        Configuration currentConfiguration = new Configuration("org.apache.sling.feature.diff.config");
        Configurations currentConfigurations = new Configurations();
        currentConfigurations.add(currentConfiguration);

        comparator.computeDiff(previousConfigurations, currentConfigurations, targetFeature);

        assertTrue(targetFeature.getPrototype().getConfigurationRemovals().isEmpty());
        assertFalse(targetFeature.getConfigurations().isEmpty());

        assertEquals(currentConfiguration.getPid(), targetFeature.getConfigurations().iterator().next().getPid());
    }

    @Test
    public void checkUpdated() {
        Configuration previousConfiguration = new Configuration("org.apache.sling.feature.diff.config");
        previousConfiguration.getProperties().put("removed", 123);
        previousConfiguration.getProperties().put("updated", new String[] { "/log" });

        Configurations previousConfigurations = new Configurations();
        previousConfigurations.add(previousConfiguration);

        Configuration currentConfiguration = new Configuration("org.apache.sling.feature.diff.config");
        currentConfiguration.getProperties().put("updated", new String[] { "/log", "/etc" });
        currentConfiguration.getProperties().put("added", true);

        Configurations currentConfigurations = new Configurations();
        currentConfigurations.add(currentConfiguration);

        comparator.computeDiff(previousConfigurations, currentConfigurations, targetFeature);

        assertTrue(targetFeature.getPrototype().getConfigurationRemovals().isEmpty());
        assertFalse(targetFeature.getConfigurations().isEmpty());

        assertEquals(currentConfiguration.getPid(), targetFeature.getConfigurations().iterator().next().getPid());
        Dictionary<String, Object> properties = targetFeature.getConfigurations().iterator().next().getProperties();

        assertTrue((boolean) properties.get("added"));
        assertArrayEquals(new String[] { "/log", "/etc" }, (String[]) properties.get("updated"));
    }

}
