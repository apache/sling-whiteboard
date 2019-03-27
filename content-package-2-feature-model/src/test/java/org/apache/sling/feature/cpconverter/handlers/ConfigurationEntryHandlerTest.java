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
package org.apache.sling.feature.cpconverter.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;

import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.fs.io.Archive.Entry;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Configurations;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.cpconverter.ContentPackage2FeatureModelConverter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ConfigurationEntryHandlerTest {

    private static final String EXPECTED_PID = "org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl";

    private final String resourceConfiguration;

    private final int expectedConfigurationsSize;

    private final AbstractConfigurationEntryHandler configurationEntryHandler;

    public ConfigurationEntryHandlerTest(String resourceConfiguration,
                                         int expectedConfigurationsSize,
                                         AbstractConfigurationEntryHandler configurationEntryHandler) {
        this.resourceConfiguration = resourceConfiguration;
        this.expectedConfigurationsSize = expectedConfigurationsSize;
        this.configurationEntryHandler = configurationEntryHandler;
    }

    @Test
    public void doesNotMatch() {
        assertFalse(configurationEntryHandler.matches("/this/is/a/path/not/pointing/to/a/valid/configuration.asd"));
    }

    @Test
    public void matches() {
        assertTrue(resourceConfiguration, configurationEntryHandler.matches(resourceConfiguration));
    }

    @Test
    public void parseConfiguration() throws Exception {
        Archive archive = mock(Archive.class);
        Entry entry = mock(Entry.class);

        when(entry.getName()).thenReturn(resourceConfiguration.substring(resourceConfiguration.lastIndexOf('/') + 1));
        when(archive.openInputStream(entry)).thenReturn(getClass().getResourceAsStream(resourceConfiguration));

        Feature feature = new Feature(new ArtifactId("org.apache.sling", "org.apache.sling.cp2fm", "0.0.1", null, null));
        ContentPackage2FeatureModelConverter converter = spy(ContentPackage2FeatureModelConverter.class);
        when(converter.getTargetFeature()).thenReturn(feature);
        doCallRealMethod().when(converter).addConfiguration(anyString(), anyString(), any());
        when(converter.getRunMode(anyString())).thenReturn(feature);

        configurationEntryHandler.handle(resourceConfiguration, archive, entry, converter);

        Configurations configurations = converter.getTargetFeature().getConfigurations();

        assertEquals(expectedConfigurationsSize, configurations.size());

        if (expectedConfigurationsSize > 0) {
            Configuration configuration = configurations.get(0);

            assertTrue(configuration.getPid(), configuration.getPid().startsWith(EXPECTED_PID));
            assertEquals("Unmatching size: " + configuration.getProperties().size(), 2, configuration.getProperties().size());
        }
    }

    @Parameters
    public static Collection<Object[]> data() {
        String path = "jcr_root/apps/asd/config/";

        return Arrays.asList(new Object[][] {
            { path + EXPECTED_PID + ".empty.cfg", 0, new PropertiesConfigurationEntryHandler() },
            { path + EXPECTED_PID + ".cfg", 1, new PropertiesConfigurationEntryHandler() },

            { path + EXPECTED_PID + ".empty.cfg.json", 0, new JsonConfigurationEntryHandler() },
            { path + EXPECTED_PID + ".cfg.json", 1, new JsonConfigurationEntryHandler() },

            { path + EXPECTED_PID + ".empty.config", 0, new ConfigurationEntryHandler() },
            { path + EXPECTED_PID + ".config", 1, new ConfigurationEntryHandler() },

            { path + EXPECTED_PID + ".empty.xml", 0, new XmlConfigurationEntryHandler() },
            { path + EXPECTED_PID + ".xml", 1, new XmlConfigurationEntryHandler() },

            { path + EXPECTED_PID + ".empty.xml.cfg", 0, new PropertiesConfigurationEntryHandler() },
            { path + EXPECTED_PID + ".xml.cfg", 1, new PropertiesConfigurationEntryHandler() },

            // runmode aware folders
            { "jcr_root/apps/asd/config.author/" + EXPECTED_PID + ".config", 1, new ConfigurationEntryHandler() },
            { "jcr_root/apps/asd/config.publish/" + EXPECTED_PID + ".config", 1, new ConfigurationEntryHandler() },
        });
    }

}
