/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.feature.analyser;

import org.apache.sling.feature.Application;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.analyser.service.Analyser;
import org.apache.sling.feature.analyser.service.Scanner;
import org.apache.sling.feature.process.FeatureResolver;
import org.apache.sling.feature.support.ArtifactManager;
import org.apache.sling.feature.support.ArtifactManagerConfig;
import org.apache.sling.feature.support.FeatureUtil;
import org.apache.sling.feature.support.json.FeatureJSONReader;
import org.junit.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import static junit.framework.TestCase.fail;

public class AnalyserTest {
    @Test
    public void testAnalyserWithCompleteFeature() throws Exception {
        final Scanner scanner = new Scanner(new ArtifactManagerConfig());
        final Analyser analyser = new Analyser(scanner);
        try ( final Reader reader = new InputStreamReader(AnalyserTest.class.getResourceAsStream("/feature_complete.json"),
                "UTF-8") ) {
            Feature feature = FeatureJSONReader.read(reader, "feature");

            Application app = FeatureUtil.assembleApplication(null, ArtifactManager.getArtifactManager(new ArtifactManagerConfig()),
                    getTestResolver(), feature);

            analyser.analyse(app);
        }
    }

    @Test
    public void testAnalyserWithInCompleteFeature() throws Exception {
        final Scanner scanner = new Scanner(new ArtifactManagerConfig());
        final Analyser analyser = new Analyser(scanner);
        try ( final Reader reader = new InputStreamReader(AnalyserTest.class.getResourceAsStream("/feature_incomplete.json"),
                "UTF-8") ) {
            Feature feature = FeatureJSONReader.read(reader, "feature");

            Application app = FeatureUtil.assembleApplication(null, ArtifactManager.getArtifactManager(new ArtifactManagerConfig()),
                    getTestResolver(), feature);

            try {
                analyser.analyse(app);

                fail("Expected an exception");
            }
            catch (Exception ex) {
                // Pass
            }
        }
    }

    private FeatureResolver getTestResolver() {
        return new FeatureResolver() {
            @Override
            public void close() throws Exception {
            }

            @Override
            public List<Feature> orderFeatures(List<Feature> features) {
                return features;
            }
        };
    }
}
