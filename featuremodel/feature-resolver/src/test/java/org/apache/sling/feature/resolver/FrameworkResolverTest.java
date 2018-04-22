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
package org.apache.sling.feature.resolver;

import org.apache.sling.feature.Feature;
import org.apache.sling.feature.io.ArtifactHandler;
import org.apache.sling.feature.io.ArtifactManager;
import org.apache.sling.feature.io.ArtifactManagerConfig;
import org.apache.sling.feature.io.json.FeatureJSONReader;
import org.apache.sling.feature.io.json.FeatureJSONReader.SubstituteVariables;
import org.apache.sling.feature.support.resolver.FeatureResolver;
import org.apache.sling.feature.support.resolver.FeatureResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.IdentityNamespace;

import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class FrameworkResolverTest {
    private Path tempDir;

    @Before
    public void setup() throws Exception {
        tempDir = Files.createTempDirectory(getClass().getSimpleName());
    }

    @After
    public void tearDown() throws Exception {
        // Delete the temp dir again
        Files.walk(tempDir)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
    }

    private Map<String, String> getFrameworkProps() {
        return Collections.singletonMap(Constants.FRAMEWORK_STORAGE, tempDir.toFile().getAbsolutePath());
    }

    @Test
    public void testResolveEmptyFeatureList() throws Exception {
        ArtifactManager am = ArtifactManager.getArtifactManager(new ArtifactManagerConfig());
        try (FeatureResolver fr = new FrameworkResolver(am, getFrameworkProps())) {
            assertEquals(Collections.emptyList(),
                    fr.orderResources(Collections.emptyList()));
        }
    }

    @Test
    @Ignore("This test is broken - FIXME")
    public void testOrderResources() throws Exception {
        ArtifactManager am = ArtifactManager.getArtifactManager(new ArtifactManagerConfig());

        Feature f1 = readFeature("/feature1.json", am);
        Feature f2 = readFeature("/feature2.json", am);
        Feature f3 = readFeature("/feature3.json", am);

        StringBuilder expectedBundles = new StringBuilder();
        expectedBundles.append("slf4j.simple 1.7.25\n");
        expectedBundles.append("slf4j.api 1.7.25\n");
        expectedBundles.append("org.apache.sling.commons.logservice 1.0.6\n");
        expectedBundles.append("org.apache.commons.io 2.6.0\n");
        expectedBundles.append("org.apache.felix.http.servlet-api 1.1.2\n");

        StringBuilder expectedResources = new StringBuilder();
        expectedResources.append("feature3 1.0.0\n");
        expectedResources.append("feature2 1.0.0\n");
        expectedResources.append("feature1 1.0.0\n");

        StringBuilder actualBundles = new StringBuilder();
        StringBuilder actualResources = new StringBuilder();
        try (FeatureResolver fr = new FrameworkResolver(am, getFrameworkProps())) {
            for(FeatureResource ordered : fr.orderResources(Arrays.asList(f1, f2, f3))) {
                if (IdentityNamespace.TYPE_BUNDLE.equals(
                        ordered.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).iterator().next().
                        getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE))) {
                    actualBundles.append(ordered.getId() + " " + ordered.getVersion() + "\n");
                } else {
                    actualResources.append(ordered.getId() + " " + ordered.getVersion() + "\n");
                }
            }
        }
        assertEquals(expectedBundles.toString(), actualBundles.toString());
        assertEquals(expectedResources.toString(), actualResources.toString());
    }

    @Test
    @Ignore("This test is broken - FIXME")
    public void testOrderResourcesWithFeatureProvidingCapability() throws Exception {
        ArtifactManager am = ArtifactManager.getArtifactManager(new ArtifactManagerConfig());

        Feature f4 = readFeature("/feature4.json", am);
        Feature f5 = readFeature("/feature5.json", am);
        Feature f6 = readFeature("/feature6.json", am);

        StringBuilder expectedResources = new StringBuilder();
        expectedResources.append("feature5 1.0.0\n");
        expectedResources.append("feature4 1.0.0\n");
        expectedResources.append("org.apache.sling.commons.logservice 1.0.6\n");
        expectedResources.append("org.apache.felix.http.servlet-api 1.1.2\n");
        expectedResources.append("feature6 1.0.0\n");
        expectedResources.append("org.apache.commons.io 2.6.0\n");

        StringBuilder actualResources = new StringBuilder();
        try (FeatureResolver fr = new FrameworkResolver(am, getFrameworkProps())) {
            for(FeatureResource ordered : fr.orderResources(Arrays.asList(f4, f5, f6))) {
                actualResources.append(ordered.getId() + " " + ordered.getVersion() + "\n");
            }
        }
        assertEquals(expectedResources.toString(), actualResources.toString());
    }

    private Feature readFeature(final String res,
            final ArtifactManager artifactManager) throws Exception {
        URL url = getClass().getResource(res);
        String file = new File(url.toURI()).getAbsolutePath();
        final ArtifactHandler featureArtifact = artifactManager.getArtifactHandler(file);

        try (final FileReader r = new FileReader(featureArtifact.getFile())) {
            final Feature f = FeatureJSONReader.read(r, featureArtifact.getUrl(), SubstituteVariables.RESOLVE);
            return f;
        }
    }
}
