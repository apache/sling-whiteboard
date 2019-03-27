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
package org.apache.sling.feature.cpconverter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.io.json.FeatureJSONReader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ContentPackage2FeatureModelConverterTest {

    private ContentPackage2FeatureModelConverter converter;

    @Before
    public void setUp() {
        converter = new ContentPackage2FeatureModelConverter();
    }

    @After
    public void tearDowd() {
        converter = null;
    }

    @Test(expected = NullPointerException.class)
    public void convertRequiresNonNullPackage() throws Exception {
        converter.convert(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void convertRequiresExistingFile() throws Exception {
        converter.convert(new File("this/file/does/not/exist.zip"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void convertRequiresNotDirectoryFile() throws Exception {
        File testDirectory = new File(System.getProperty("user.dir"));
        converter.convert(testDirectory);
    }

    @Test(expected = IllegalStateException.class)
    public void getRunModeRequiresConvertInvoked() {
        converter.getRunMode(null);
    }

    @Test(expected = IllegalStateException.class)
    public void addConfigurationRequiresConvertInvoked() {
        converter.setMergeConfigurations(true).getRunMode(null);
    }

    @Test(expected = NullPointerException.class)
    public void processRequiresNotNullPackage() throws Exception {
        converter.processSubPackage("", null);
    }

    @Test(expected = IllegalStateException.class)
    public void processRequiresConvertInvoked() throws Exception {
        converter.processSubPackage("", mock(File.class));
    }

    @Test(expected = NullPointerException.class)
    public void deployLocallyAndAttachRequiresNonNullInput() throws Exception {
        converter.attach(null, null, null, null, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void deployLocallyAndAttachRequiresNonNullGroupId() throws Exception {
        converter.attach(null, null, null, null, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void deployLocallyAndAttachRequiresNonNullArtifactId() throws Exception {
        converter.attach(null, "org.apache.sling", null, null, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void deployLocallyAndAttachRequiresNonNullVersion() throws Exception {
        converter.attach(null, "org.apache.sling", "org.apache.sling.cm2fm", null, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void deployLocallyAndAttachRequiresNonNullType() throws Exception {
        converter.attach(null, "org.apache.sling", "org.apache.sling.cm2fm", "0.0.1", null, null);
    }

    @Test
    public void convertContentPackage() throws Exception {
        URL packageUrl = getClass().getResource("test-content-package.zip");
        File packageFile = FileUtils.toFile(packageUrl);

        File outputDirectory = new File(System.getProperty("testDirectory"), getClass().getName() + '_' + System.currentTimeMillis());

        converter.setBundlesStartOrder(5).setOutputDirectory(outputDirectory).convert(packageFile);

        verifyFeatureFile(outputDirectory,
                          "asd.retail.all.json",
                          "org.apache.sling:asd.retail.all:slingosgifeature:cp2fm-converted-feature:0.0.1",
                          Arrays.asList("org.apache.felix:org.apache.felix.framework:6.0.1"),
                          Arrays.asList("org.apache.sling.commons.log.LogManager.factory.config-asd-retail"),
                          Arrays.asList("org.apache.sling:asd.retail.all:zip:cp2fm-converted-feature:0.0.1"));
        verifyFeatureFile(outputDirectory,
                          "asd.retail.all-author.json",
                          "org.apache.sling:asd.retail.all:slingosgifeature:cp2fm-converted-feature-author:0.0.1",
                          Arrays.asList("org.apache.sling:org.apache.sling.api:2.20.0"),
                          Collections.emptyList(),
                          Collections.emptyList());
        verifyFeatureFile(outputDirectory,
                          "asd.retail.all-publish.json",
                          "org.apache.sling:asd.retail.all:slingosgifeature:cp2fm-converted-feature-publish:0.0.1",
                          Arrays.asList("org.apache.sling:org.apache.sling.models.api:1.3.8"),
                          Arrays.asList("org.apache.sling.serviceusermapping.impl.ServiceUserMapperImpl.amended-asd-retail"),
                          Collections.emptyList());

        ZipFile zipFile = new ZipFile(new File(outputDirectory, "bundles/org/apache/sling/asd.retail.all/0.0.1/asd.retail.all-0.0.1-cp2fm-converted-feature.zip"));
        for (String expectedEntry : new String[] {
                "jcr_root/content/asd/.content.xml",
                "jcr_root/content/asd/resources.xml",
                "jcr_root/apps/.content.xml",
                "META-INF/vault/properties.xml",
                "META-INF/vault/config.xml",
                "META-INF/vault/settings.xml",
                "META-INF/vault/definition/.content.xml",
                "jcr_root/etc/packages/asd/test-bundles.zip",
                "jcr_root/etc/packages/asd/test-configurations.zip",
                "jcr_root/etc/packages/asd/test-content.zip",
                }) {
            assertNotNull(zipFile.getEntry(expectedEntry));
        }
        zipFile.close();
    }

    private void verifyFeatureFile(File outputDirectory,
                                   String name,
                                   String expectedArtifactId,
                                   List<String> expectedBundles,
                                   List<String> expectedConfigurations,
                                   List<String> expectedContentPackagesExtensions) throws Exception {
        File featureFile = new File(outputDirectory, name);
        assertTrue(featureFile + " was not correctly created", featureFile.exists());

        try (Reader reader = new FileReader(featureFile)) {
            Feature feature = FeatureJSONReader.read(reader, featureFile.getAbsolutePath());

            assertEquals(expectedArtifactId, feature.getId().toMvnId());

            for (String expectedBundle : expectedBundles) {
                assertTrue(expectedBundle + " not found in Feature " + expectedArtifactId, feature.getBundles().containsExact(ArtifactId.fromMvnId(expectedBundle)));
                verifyInstalledBundle(outputDirectory, expectedBundle);
            }

            for (String expectedConfiguration : expectedConfigurations) {
                assertNotNull(expectedConfiguration + " not found in Feature " + expectedArtifactId, feature.getConfigurations().getConfiguration(expectedConfiguration));
            }

            for (String expectedContentPackagesExtension : expectedContentPackagesExtensions) {
                assertTrue(expectedContentPackagesExtension + " not found in Feature " + expectedArtifactId,
                           feature.getExtensions().getByName("content-packages").getArtifacts().containsExact(ArtifactId.fromMvnId(expectedContentPackagesExtension)));
                verifyInstalledBundle(outputDirectory, expectedContentPackagesExtension);
            }
        }
    }

    private void verifyInstalledBundle(File outputDirectory, String coordinates) {
        ArtifactId bundleId = ArtifactId.fromMvnId(coordinates);

        File bundleDirectory = new File(outputDirectory, "bundles");

        StringTokenizer tokenizer = new StringTokenizer(bundleId.getGroupId(), ".");
        while (tokenizer.hasMoreTokens()) {
            bundleDirectory = new File(bundleDirectory, tokenizer.nextToken());
        }

        bundleDirectory = new File(bundleDirectory, bundleId.getArtifactId());
        bundleDirectory = new File(bundleDirectory, bundleId.getVersion());

        StringBuilder bundleFileName = new StringBuilder()
                                       .append(bundleId.getArtifactId())
                                       .append('-')
                                       .append(bundleId.getVersion());
        if (bundleId.getClassifier() != null) {
            bundleFileName.append('-').append(bundleId.getClassifier());
        }
        bundleFileName.append('.').append(bundleId.getType());

        File bundleFile = new File(bundleDirectory, bundleFileName.toString());
        assertTrue("Bundle " + bundleFile + " does not exist", bundleFile.exists());

        File pomFile = new File(bundleDirectory, String.format("%s-%s.pom", bundleId.getArtifactId(), bundleId.getVersion()));
        assertTrue("POM file " + pomFile + " does not exist", pomFile.exists());
    }

    @Test(expected = IllegalArgumentException.class)
    public void verifyFilteringOutUndesiredPackages() throws Exception {
        converter.addFilteringPattern(".*\\/install(?!(\\.runMode1\\/|\\.runMode2\\/|\\/))(.*)(?=\\.zip$).*");

        URL packageUrl = getClass().getResource("test-content-package-unacceptable.zip");
        File packageFile = FileUtils.toFile(packageUrl);

        File outputDirectory = new File(System.getProperty("testDirectory"), getClass().getName() + '_' + System.currentTimeMillis());

        converter.setBundlesStartOrder(5).setOutputDirectory(outputDirectory).convert(packageFile);
    }

}
