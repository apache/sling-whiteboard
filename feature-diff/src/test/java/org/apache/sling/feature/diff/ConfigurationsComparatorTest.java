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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Configurations;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConfigurationsComparatorTest {

    private ConfigurationsComparator comparator;

    @Before
    public void setUp() {
        comparator = new ConfigurationsComparator();
    }

    @After
    public void tearDown() {
        comparator = null;
    }

    @Test
    public void checkRemoved() {
        Configuration previousConfiguration = new Configuration("org.apache.sling.feature.diff.config");
        Configurations previousConfigurations = new Configurations();
        previousConfigurations.add(previousConfiguration);

        Configurations currentConfigurations = new Configurations();

        DiffSection configurationsDiff = comparator.apply(previousConfigurations, currentConfigurations);
        assertFalse(configurationsDiff.isEmpty());

        assertEquals(previousConfiguration.getPid(), configurationsDiff.getRemoved().iterator().next());
    }

    @Test
    public void checkAdded() {
        Configurations previousConfigurations = new Configurations();

        Configuration currentConfiguration = new Configuration("org.apache.sling.feature.diff.config");
        Configurations currentConfigurations = new Configurations();
        currentConfigurations.add(currentConfiguration);

        DiffSection configurationsDiff = comparator.apply(previousConfigurations, currentConfigurations);
        assertFalse(configurationsDiff.isEmpty());

        assertEquals(currentConfiguration.getPid(), configurationsDiff.getAdded().iterator().next());
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

        DiffSection configurationsDiff = comparator.apply(previousConfigurations, currentConfigurations);
        assertFalse(configurationsDiff.isEmpty());

        DiffSection configurationDiff = configurationsDiff.getUpdates().iterator().next();

        assertEquals("removed", configurationDiff.getRemoved().iterator().next());
        assertEquals("added", configurationDiff.getAdded().iterator().next());

        @SuppressWarnings("unchecked") // type known by design
        UpdatedItem<String[]> updated = (UpdatedItem<String[]>) configurationDiff.getUpdatedItems().iterator().next();
        assertEquals("updated", updated.getId());
        assertArrayEquals(new String[] { "/log" }, updated.getPrevious());
        assertArrayEquals(new String[] { "/log", "/etc" }, updated.getCurrent());
    }

}
