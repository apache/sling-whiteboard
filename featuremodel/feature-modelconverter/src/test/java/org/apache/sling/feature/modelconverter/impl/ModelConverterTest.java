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

import org.apache.sling.feature.Bundles;
import org.apache.sling.feature.Configurations;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Extensions;
import org.apache.sling.feature.support.ArtifactManager;
import org.apache.sling.feature.support.ArtifactManagerConfig;
import org.apache.sling.feature.support.FeatureUtil;
import org.apache.sling.feature.support.json.FeatureJSONReader.SubstituteVariables;
import org.apache.sling.provisioning.model.Artifact;
import org.apache.sling.provisioning.model.ArtifactGroup;
import org.apache.sling.provisioning.model.Configuration;
import org.apache.sling.provisioning.model.Feature;
import org.apache.sling.provisioning.model.KeyValueMap;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.ModelConstants;
import org.apache.sling.provisioning.model.ModelUtility;
import org.apache.sling.provisioning.model.ModelUtility.ResolverOptions;
import org.apache.sling.provisioning.model.ModelUtility.VariableResolver;
import org.apache.sling.provisioning.model.RunMode;
import org.apache.sling.provisioning.model.Section;
import org.apache.sling.provisioning.model.io.ModelReader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
    public void testBootToProvModel() throws Exception {
        testConvertToProvisioningModel("/boot.json", "/boot.txt");
    }

    @Test
    public void testBootToFeature() throws Exception {
        testConvertToFeature("/boot.txt", "/boot.json");
    }

    @Test
    public void testOakToProvModel() throws Exception {
        testConvertToProvisioningModel("/oak.json", "/oak.txt");
    }

    @Test
    public void testOakToFeature() throws Exception {
        testConvertToFeature("/oak.txt", "/oak.json");
    }

    @Test
    public void testRepoinitToProvModel() throws Exception {
        testConvertToProvisioningModel("/repoinit.json", "/repoinit.txt");
    }

    @Test
    public void testRepoinitToFeature() throws Exception {
        testConvertToFeature("/repoinit.txt", "/repoinit.json");
    }

    @Test
    public void testProvModelRoundtripFolder() throws Exception {
        String dir = System.getProperty("test.prov.files.dir");
        File filesDir;
        if (dir != null) {
            filesDir = new File(dir);
        } else {
            filesDir = new File(getClass().getResource("/repoinit.txt").toURI()).
                getParentFile();
        }

        for (File f : filesDir.listFiles((d, n) -> n.endsWith(".txt"))) {
            testConvertFromProvModelRoundTrip(f);
        }
    }

    public void testConvertFromProvModelRoundTrip(File orgProvModel) throws Exception {
        File outJSONFile = new File(tempDir.toFile(), orgProvModel.getName() + ".json.generated");
        File outProvFile = new File(tempDir.toFile(), orgProvModel.getName() + ".txt.generated");

        ProvisioningToFeature.convert(orgProvModel, outJSONFile.getAbsolutePath());
        FeatureToProvisioning.convert(outJSONFile, outProvFile.getAbsolutePath(),
                artifactManager);

        Model expected = readProvisioningModel(orgProvModel);
        Model actual = readProvisioningModel(outProvFile);
        assertModelsEqual(expected, actual);
    }

    public void testConvertToFeature(String originalProvModel, String expectedJSON) throws Exception {
        File inFile = new File(getClass().getResource(originalProvModel).toURI());
        File outFile = new File(tempDir.toFile(), expectedJSON + ".generated");

        String outPath = outFile.getAbsolutePath();
        ProvisioningToFeature.convert(inFile, outPath);

        String expectedFile = new File(getClass().getResource(expectedJSON).toURI()).getAbsolutePath();
        org.apache.sling.feature.Feature expected = FeatureUtil.getFeature(expectedFile, artifactManager, SubstituteVariables.NONE);
        org.apache.sling.feature.Feature actual = FeatureUtil.getFeature(outPath, artifactManager, SubstituteVariables.NONE);
        assertFeaturesEqual(expected, actual);
    }

    public void testConvertToProvisioningModel(String originalJSON, String expectedProvModel) throws URISyntaxException, IOException {
        File inFile = new File(getClass().getResource(originalJSON).toURI());
        File outFile = new File(tempDir.toFile(), expectedProvModel + ".generated");

        FeatureToProvisioning.convert(inFile, outFile.getAbsolutePath(),
                artifactManager);

        File expectedFile = new File(getClass().getResource(expectedProvModel).toURI());
        Model expected = readProvisioningModel(expectedFile);
        Model actual = readProvisioningModel(outFile);
        assertModelsEqual(expected, actual);
    }

    private static Model readProvisioningModel(File modelFile) throws IOException {
        try (final FileReader is = new FileReader(modelFile)) {
            Model model = ModelReader.read(is, modelFile.getAbsolutePath());

            // Fix the configurations up from the internal format to the Dictionary-based format
            return ModelUtility.getEffectiveModel(model,
                    new ResolverOptions().variableResolver(new VariableResolver() {
                @Override
                public String resolve(final Feature feature, final String name) {
                    // Keep variables as-is in the model
                    return "${" + name + "}";
                }
            }));
        }
    }

    private void assertFeaturesEqual(org.apache.sling.feature.Feature expected, org.apache.sling.feature.Feature actual) {
        assertEquals(expected.getTitle(), actual.getTitle());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getVendor(), actual.getVendor());
        assertEquals(expected.getLicense(), actual.getLicense());

        assertFeatureKVMapEquals(expected.getVariables(), actual.getVariables());
        assertBundlesEqual(expected.getBundles(), actual.getBundles());
        assertConfigurationsEqual(expected.getConfigurations(), actual.getConfigurations(), expected.getBundles(), actual.getBundles());
        assertFeatureKVMapEquals(expected.getFrameworkProperties(), actual.getFrameworkProperties());
        assertExtensionEqual(expected.getExtensions(), actual.getExtensions());

        // Ignore caps and reqs, includes and here since they cannot come from the prov model.
    }

    private void assertBundlesEqual(Bundles expected, Bundles actual) {
        for (Iterator<org.apache.sling.feature.Artifact> it = expected.iterator(); it.hasNext(); ) {
            org.apache.sling.feature.Artifact ex = it.next();

            boolean found = false;
            for (Iterator<org.apache.sling.feature.Artifact> it2 = actual.iterator(); it2.hasNext(); ) {
                org.apache.sling.feature.Artifact ac = it2.next();
                if (ac.getId().equals(ex.getId())) {
                    found = true;
                    assertFeatureKVMapEquals(ex.getMetadata(), ac.getMetadata());
                    break;
                }
            }
            assertTrue("Not found: " + ex, found);
        }
    }

    private void assertConfigurationsEqual(Configurations expected, Configurations actual, Bundles exBundles, Bundles acBundles) {
        for (Iterator<org.apache.sling.feature.Configuration> it = expected.iterator(); it.hasNext(); ) {
            org.apache.sling.feature.Configuration ex = it.next();

            boolean found = false;
            for (Iterator<org.apache.sling.feature.Configuration> it2 = actual.iterator(); it2.hasNext(); ) {
                org.apache.sling.feature.Configuration ac = it2.next();
                if (ex.getPid() != null) {
                    if (ex.getPid().equals(ac.getPid())) {
                        found = true;
                        assertConfigProps(ex, ac, exBundles, acBundles);
                    }
                } else {
                    if (ex.getFactoryPid().equals(ac.getFactoryPid())) {
                        found = true;
                        assertConfigProps(ex, ac, exBundles, acBundles);
                    }
                }
            }
            assertTrue(found);
        }
    }

    private void assertConfigProps(org.apache.sling.feature.Configuration expected, org.apache.sling.feature.Configuration actual, Bundles exBundles, Bundles acBundles) {
        assertTrue("Configurations not equal: " + expected.getProperties() + " vs " + actual.getProperties(),
                configPropsEqual(expected.getProperties(), actual.getProperties()));
    }

    private boolean configPropsEqual(Dictionary<String, Object> d1, Dictionary<String, Object> d2) {
        if (d1.size() != d2.size()) {
            return false;
        }

        for (Enumeration<String> e = d1.keys(); e.hasMoreElements(); ) {
            String k = e.nextElement();
            Object v = d1.get(k);
            if (v instanceof Object[]) {
                if (!Arrays.equals((Object[]) v, (Object[]) d2.get(k)))
                    return false;
            } else {
                if (!v.equals(d2.get(k)))
                    return false;
            }
        }
        return true;
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
        assertRunModesEqual(expected.getName(), expected.getRunModes(), actual.getRunModes());
        assertKVMapEquals(expected.getVariables(), actual.getVariables());
        assertSectionsEqual(expected.getAdditionalSections(), actual.getAdditionalSections());
    }

    private void assertRunModesEqual(String featureName, List<RunMode> expected, List<RunMode> actual) {
        assertEquals(expected.size(), actual.size());
        for (RunMode rm : expected) {
            boolean found = false;
            for (RunMode arm : actual) {
                if (runModesEqual(featureName, rm, arm)) {
                    found = true;
                    break;
                }

            }
            if (!found) {
                fail("Run Mode " + rm + " not found in actual list " + actual);
            }
        }
    }

    private boolean runModesEqual(String featureName, RunMode rm1, RunMode rm2) {
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
                if (artifactGroupsEquals(featureName, g1, g2)) {
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

                if (cfg1.getFactoryPid() == null) {
                    if (cfg2.getFactoryPid() != null)
                        return false;
                } else {
                    if (!cfg1.getFactoryPid().equals(cfg2.getFactoryPid())) {
                        return false;
                    }
                }

                if (!configPropsEqual(cfg1.getProperties(), cfg2.getProperties())) {
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

    private Map<String, String> kvToMap(KeyValueMap<String> kvm) {
        Map<String, String> m = new HashMap<>();

        for (Map.Entry<String, String> entry : kvm) {
            m.put(entry.getKey(), entry.getValue());
        }

        return m;
    }

    private Map<String, String> featureKvToMap(org.apache.sling.feature.KeyValueMap kvm) {
        Map<String, String> m = new HashMap<>();

        for (Map.Entry<String, String> entry : kvm) {
            m.put(entry.getKey(), entry.getValue());
        }

        return m;
    }

    private boolean artifactGroupsEquals(String featureName, ArtifactGroup g1, ArtifactGroup g2) {
        int sl1 = effectiveStartLevel(featureName, g1.getStartLevel());
        int sl2 = effectiveStartLevel(featureName, g2.getStartLevel());
        if (sl1 != sl2)
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

    private int effectiveStartLevel(String featureName, int startLevel) {
        if (startLevel != 0)
            return startLevel;

        if (ModelConstants.FEATURE_BOOT.equals(featureName)) {
            return 1;
        } else {
            return 20;
        }
    }

    private void assertKVMapEquals(KeyValueMap<String> expected, KeyValueMap<String> actual) {
        assertEquals(kvToMap(expected), kvToMap(actual));
    }

    private void assertFeatureKVMapEquals(org.apache.sling.feature.KeyValueMap expected,
            org.apache.sling.feature.KeyValueMap actual) {
        assertEquals(featureKvToMap(expected), featureKvToMap(actual));
    }

    private void assertExtensionEqual(Extensions expected, Extensions actual) {
        assertEquals(expected.size(), actual.size());

        for (int i=0; i<expected.size(); i++) {
            // TODO support the fact that they may be out of order
            Extension ex = expected.get(i);
            Extension ac = actual.get(i);

            assertEquals(ex.getType(), ac.getType());
            assertEquals(ex.getName(), ac.getName());
            assertEquals(ex.isRequired(), ac.isRequired());

            String exTxt = ex.getText().replaceAll("\\s+", "");
            String acTxt = ac.getText().replaceAll("\\s+", "");
            assertEquals(exTxt, acTxt);

            /* TODO reinstantiate for Artifacts extentions
            assertEquals(ex.getArtifacts().size(), ac.getArtifacts().size());
            for (int j = 0; j<ex.getArtifacts().size(); j++) {
                org.apache.sling.feature.Artifact exa = ex.getArtifacts().get(j);
                org.apache.sling.feature.Artifact aca = ac.getArtifacts().get(j);
                assertEquals(exa.getId().toMvnId(), aca.getId().toMvnId());
            }
            */
        }
    }

    private void assertSectionsEqual(List<Section> expected, List<Section> actual) {
        assertEquals(expected.size(), actual.size());

        for (int i=0; i<expected.size(); i++) {
            Section esec = expected.get(i);
            Section asec = actual.get(i);
            assertEquals(esec.getName(), asec.getName());
            assertEquals(esec.getContents().replaceAll("\\s+", ""),
                    asec.getContents().replaceAll("\\s+", ""));
            assertEquals(esec.getAttributes(), asec.getAttributes());
        }
    }
}
