/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.commons.log.json.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;

public class LoggerConfigurationTest {

    private ComponentContext componentContext;
    private Dictionary<String, Object> properties;

    @Before
    public void init() {
        componentContext = mock(ComponentContext.class);
        properties = new Hashtable<>();
        when(componentContext.getProperties()).thenReturn(properties);
    }

    @Test
    public void testLoggerConfiguration() {
        String expectedPid = "test";
        properties.put(Constants.SERVICE_PID, expectedPid);
        LoggerConfiguration.Config config = TestHelper.createLoggerConfigurationConfig("logs/test.log", "info",
                new String[] { "com.text" });
        LoggerConfiguration configuration = new LoggerConfiguration(config, componentContext);

        assertEquals(expectedPid, configuration.getPid());
        assertEquals(config, configuration.getConfig());
    }

    @Test
    public void testNoPid() {
        LoggerConfiguration.Config config = TestHelper.createLoggerConfigurationConfig("logs/test.log", "info",
                new String[] { "com.text" });
        LoggerConfiguration configuration = new LoggerConfiguration(config, componentContext);

        assertNotNull(configuration.getPid());
        assertTrue(configuration.getPid().startsWith(LoggerConfiguration.class.getName() + "@"));
        assertEquals(config, configuration.getConfig());
    }
}
