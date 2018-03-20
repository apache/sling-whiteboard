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
package org.apache.sling.feature.modelconverter.impl;

import org.apache.sling.feature.support.ArtifactManager;
import org.apache.sling.feature.support.ArtifactManagerConfig;
import org.apache.sling.provisioning.model.Artifact;
import org.apache.sling.provisioning.model.ArtifactGroup;
import org.apache.sling.provisioning.model.Configuration;
import org.apache.sling.provisioning.model.Feature;
import org.apache.sling.provisioning.model.KeyValueMap;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.ModelConstants;
import org.apache.sling.provisioning.model.RunMode;
import org.apache.sling.provisioning.model.Section;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ModelConverterTest {
    private Path tempDir;
    private ArtifactManager artifactManager;

    @Before
    public void setup() throws Exception {
        tempDir = Files.createTempDirectory(getClass().getSimpleName());
        artifactManager = ArtifactManager.getArtifactManager(
                new ArtifactManagerConfig());
    }

    @After
    public void tearDown() throws Exception {
        // Delete the temp dir again
        Files.walk(tempDir)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
    }

    @Test
    public void testBoot() throws Exception {
        testConvertToProvisioningModel("/boot.json", "/boot.txt");
    }

    @Test
    public void testOak() throws Exception {
        testConvertToProvisioningModel("/oak.json", "/oak.txt");
    }

    private void testConvertToProvisioningModel(String originalJSON, String expectedProvModel) throws URISyntaxException, IOException {
        File inFile = new File(getClass().getResource(originalJSON).toURI());
        File outFile = new File(tempDir.toFile(), expectedProvModel + ".generated");

        FeatureToProvisioning.convert(inFile, outFile.getAbsolutePath(),
                artifactManager);

        File expectedFile = new File(getClass().getResource(expectedProvModel).toURI());
        Model expected = ProvisioningToFeature.readProvisioningModel(expectedFile);
        Model actual = ProvisioningToFeature.readProvisioningModel(outFile);
        assertModelsEqual(expected, actual);
    }

    private void assertModelsEqual(Model expected, Model actual) {
        for (int i=0; i<expected.getFeatures().size(); i++) {
            Feature expectedFeature = expected.getFeatures().get(i);
            Feature actualFeature = actual.getFeatures().get(i);
            assertFeaturesEqual(expectedFeature, actualFeature);
        }
    }

    private void assertFeaturesEqual(Feature expected, Feature actual) {
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getVersion(), actual.getVersion());
        assertEquals(expected.getType(), actual.getType());
        assertRunModesEqual(expected.getRunModes(), actual.getRunModes());
        assertKVMapEquals(expected.getVariables(), actual.getVariables());
        assertSectionsEqual(expected.getAdditionalSections(), actual.getAdditionalSections());
    }

    private void assertRunModesEqual(List<RunMode> expected, List<RunMode> actual) {
        assertEquals(expected.size(), actual.size());
        for (RunMode rm : expected) {
            boolean found = false;
            for (RunMode arm : actual) {
                if (runModesEqual(rm, arm)) {
                    found = true;
                    break;
                }

            }
            if (!found) {
                fail("Run Mode " + rm + " not found in actual list " + actual);
            }
        }
    }

    private boolean runModesEqual(RunMode rm1, RunMode rm2) {
        if (rm1.getNames() == null) {
            if (rm2.getNames() != null)
                return false;
        } else {
            if (rm2.getNames() == null)
                return false;

            HashSet<String> names1 = new HashSet<>(Arrays.asList(rm1.getNames()));
            HashSet<String> names2 = new HashSet<>(Arrays.asList(rm2.getNames()));

            if (!names1.equals(names2))
                return false;
        }

        List<ArtifactGroup> ag1 = rm1.getArtifactGroups();
        List<ArtifactGroup> ag2 = rm2.getArtifactGroups();
        if (ag1.size() != ag2.size())
            return false;

        for (ArtifactGroup g1 : ag1) {
            boolean found = false;
            for (ArtifactGroup g2 : ag2) {
                if (artifactGroupsEquals(g1, g2)) {
                    found = true;
                    break;
                }
            }
            if (!found)
                return false;
        }

        List<Configuration> configs1 = new ArrayList<>();
        rm1.getConfigurations().iterator().forEachRemaining(configs1::add);
        List<Configuration> configs2 = new ArrayList<>();
        rm2.getConfigurations().iterator().forEachRemaining(configs2::add);
        if (configs1.size() != configs2.size())
            return false;

        for (int i=0; i < configs1.size(); i++) {
            Configuration cfg1 = configs1.get(i);

            boolean found = false;
            for (Configuration cfg2 : configs2) {
                if (!cfg2.getPid().equals(cfg1.getPid())) {
                    continue;
                }
                found = true;

                Map<String, Object> m1 = cfgMap(cfg1.getProperties());
                Map<String, Object> m2 = cfgMap(cfg2.getProperties());
                if (!m1.equals(m2)) {
                    return false;
                }
                break;
            }
            assertTrue("Configuration with PID " + cfg1.getPid() + " not found", found);
        }

        Map<String, String> m1 = kvToMap(rm1.getSettings());
        Map<String, String> m2 = kvToMap(rm2.getSettings());

        return m1.equals(m2);
    }

    private Map<String, Object> cfgMap(Dictionary<String, Object> properties) {
        Map<String, Object> m = new HashMap<>();
        for (Enumeration<String> e = properties.keys(); e.hasMoreElements(); ) {
            String key = e.nextElement();
            Object value = properties.get(key);
            if (ModelConstants.CFG_UNPROCESSED.equals(key) && value instanceof String) {
                String val = (String) value;
                // Collapse line continuation characters
                val = val.replaceAll("[\\\\]\\r?\\n", "");
                for (String line : val.split("\\r?\\n")) {

                    String[] kv = line.trim().split("=");
                    if (kv.length >= 2) {
                        String v = kv[1].trim().replaceAll("[" +Pattern.quote("[") + "]\\s+[\"]", "[\"");
                        v = v.replaceAll("[\"][,]\\s*[]]","\"]");
                        m.put(kv[0].trim(), v.trim());
                    }
                }
            } else {
                m.put(key, value);
            }
        }
        return m;
    }

    private Map<String, String> kvToMap(KeyValueMap<String> kvm) {
        Map<String, String> m = new HashMap<>();

        for (Map.Entry<String, String> entry : kvm) {
            m.put(entry.getKey(), entry.getValue());
        }

        return m;
    }

    private boolean artifactGroupsEquals(ArtifactGroup g1, ArtifactGroup g2) {
        if (g1.getStartLevel() != g2.getStartLevel())
            return false;

        List<Artifact> al1 = new ArrayList<>();
        g1.iterator().forEachRemaining(al1::add);

        List<Artifact> al2 = new ArrayList<>();
        g2.iterator().forEachRemaining(al2::add);

        for (int i=0; i < al1.size(); i++) {
            Artifact a1 = al1.get(i);
            Artifact a2 = al2.get(i);
            if (a1.compareTo(a2) != 0)
                return false;
        }
        return true;
    }

    private void assertKVMapEquals(KeyValueMap<String> expected, KeyValueMap<String> actual) {
        assertEquals(kvToMap(expected), kvToMap(actual));
    }

    private void assertSectionsEqual(List<Section> expected, List<Section> actual) {
        assertEquals(expected.size(), actual.size());

        for (int i=0; i<expected.size(); i++) {
            Section esec = expected.get(i);
            Section asec = actual.get(i);
            assertEquals(esec.getName(), asec.getName());
            assertEquals(esec.getContents(), asec.getContents());
            assertEquals(esec.getAttributes(), asec.getAttributes());
        }
    }
}
