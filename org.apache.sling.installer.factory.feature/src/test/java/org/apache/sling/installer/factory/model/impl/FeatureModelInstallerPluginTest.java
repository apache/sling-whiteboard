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
package org.apache.sling.installer.factory.model.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;

public class FeatureModelInstallerPluginTest {

    @Test
    public void testNoPatterns() throws Exception {
        final FeatureModelInstallerPlugin.Config config = Mockito.mock(FeatureModelInstallerPlugin.Config.class);
        Mockito.when(config.classifierPatterns()).thenReturn(null);

        final FeatureModelInstallerPlugin plugin = new FeatureModelInstallerPlugin(Mockito.mock(BundleContext.class),
                config);
        assertTrue(plugin.classifierMatches("foo"));
        assertTrue(plugin.classifierMatches(null));
    }

    @Test
    public void testNoClassifierPattern() throws Exception {
        final FeatureModelInstallerPlugin.Config config = Mockito.mock(FeatureModelInstallerPlugin.Config.class);
        Mockito.when(config.classifierPatterns()).thenReturn(new String[] { ":" });

        final FeatureModelInstallerPlugin plugin = new FeatureModelInstallerPlugin(Mockito.mock(BundleContext.class),
                config);
        assertFalse(plugin.classifierMatches("foo"));
        assertTrue(plugin.classifierMatches(null));
    }

    @Test
    public void testClassifierPatterns() throws Exception {
        final FeatureModelInstallerPlugin.Config config = Mockito.mock(FeatureModelInstallerPlugin.Config.class);
        Mockito.when(config.classifierPatterns()).thenReturn(new String[] { ":", "*devfar", "*prodfar", "*special*" });

        final FeatureModelInstallerPlugin plugin = new FeatureModelInstallerPlugin(Mockito.mock(BundleContext.class),
                config);

        assertTrue(plugin.classifierMatches(null));
        assertTrue(plugin.classifierMatches("mydevfar"));
        assertTrue(plugin.classifierMatches("myprodfar"));
        assertTrue(plugin.classifierMatches("superspecialfar"));
        assertTrue(plugin.classifierMatches("evenmorespecial"));

        assertFalse(plugin.classifierMatches("foo"));
        assertFalse(plugin.classifierMatches("devmyfar"));
        assertFalse(plugin.classifierMatches("prodmfar"));
    }
}
