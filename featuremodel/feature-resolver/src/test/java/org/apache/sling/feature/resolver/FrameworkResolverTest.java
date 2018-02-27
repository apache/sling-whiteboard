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

import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import org.apache.sling.feature.BundleResource;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.FeatureResource;
import org.apache.sling.feature.process.FeatureResolver;
import org.apache.sling.feature.support.ArtifactHandler;
import org.apache.sling.feature.support.ArtifactManager;
import org.apache.sling.feature.support.ArtifactManagerConfig;
import org.apache.sling.feature.support.json.FeatureJSONReader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Constants;

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
    public void testOrderBundles() throws Exception {
        ArtifactManager am = ArtifactManager.getArtifactManager(new ArtifactManagerConfig());

        Feature f1 = readFeature("/feature1.json", am);
        Feature f2 = readFeature("/feature2.json", am);
        Feature f3 = readFeature("/feature3.json", am);

        StringBuilder expected = new StringBuilder();
        expected.append("slf4j.simple 1.7.25\n");
        expected.append("slf4j.api 1.7.25\n");
        expected.append("org.apache.sling.commons.logservice 1.0.6\n");
        expected.append("org.apache.commons.io 2.6.0\n");
        expected.append("org.apache.felix.http.servlet-api 1.1.2\n");

        StringBuilder result = new StringBuilder();
        try (FeatureResolver fr = new FrameworkResolver(am, getFrameworkProps())) {
            for(FeatureResource ordered : fr.orderResources(Arrays.asList(f1, f2, f3))) {
                if (ordered instanceof BundleResource) {
                    BundleResource br = (BundleResource) ordered;
                    result.append(br.getSymbolicName() + " " + br.getVersion() + "\n");
                }
            }
        }
        assertEquals(expected.toString(), result.toString());
    }

    private Feature readFeature(final String res,
            final ArtifactManager artifactManager) throws Exception {
        URL url = getClass().getResource(res);
        String file = new File(url.toURI()).getAbsolutePath();
        final ArtifactHandler featureArtifact = artifactManager.getArtifactHandler(file);

        try (final FileReader r = new FileReader(featureArtifact.getFile())) {
            final Feature f = FeatureJSONReader.read(r, featureArtifact.getUrl());
            return f;
        }
    }
}
