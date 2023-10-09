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
package org.apache.sling.feature.analyser.task.classes.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.analyser.Analyser;
import org.apache.sling.feature.analyser.AnalyserResult;
import org.apache.sling.feature.analyser.AnalyserResult.ArtifactReport;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.io.artifacts.ArtifactManager;
import org.apache.sling.feature.io.artifacts.ArtifactManagerConfig;
import org.apache.sling.feature.io.json.FeatureJSONReader;
import org.apache.sling.feature.scanner.Scanner;
import org.hamcrest.Description;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.Test;


class CheckProviderTypeImplementationsTest {

    @Test
    void test() throws Exception {
        AnalyserResult result = analyse("/metadata-feature.json", new CheckProviderTypeImplementations());
        assertEquals(7, result.getArtifactErrors().size());
        MatcherAssert.assertThat(result.getArtifactErrors(), Matchers.hasItem(
                new ArtifactReportMatcher("org.apache.jackrabbit:oak-jackrabbit-api:1.56.0", 
                        "Type org.apache.jackrabbit.api.security.JackrabbitAccessControlList extends provider type org.apache.jackrabbit.api.security.JackrabbitAccessControlPolicy. This is not allowed!")));
        
    }

    private static final class ArtifactReportMatcher extends TypeSafeMatcher<ArtifactReport> {

        private final ArtifactId id;
        private final String value;
        private final String taskId;

        ArtifactReportMatcher(String mvnCoordinates, String value) {
            this.id = ArtifactId.fromMvnId(mvnCoordinates);
            this.value = value;
            this.taskId = "prevent-provider-type-impls";
        }

        @Override
        protected boolean matchesSafely(ArtifactReport item) {
            return (item.getKey().equals(id)
                && item.getValue().equals(value)
                && item.getTaskId().equals(taskId));
        }

        @Override
        public void describeTo(Description description) {
            description.appendText( "ArtifactReport [").appendText(taskId).appendText("] ").appendText(id.toString()).appendText(": ").appendText(value);
        }
    }

    static AnalyserResult analyse(String featureResourceName, AnalyserTask... tasks) throws Exception {
        final Feature feature;
        try (Reader reader = new InputStreamReader(CheckProviderTypeImplementationsTest.class.getResourceAsStream(featureResourceName), StandardCharsets.UTF_8)) {
            feature = FeatureJSONReader.read(reader, "");
        }
        ArtifactManagerConfig config = new ArtifactManagerConfig();
        final Scanner scanner = new Scanner(ArtifactManager.getArtifactManager(config));
        Analyser analyser = new Analyser(scanner, tasks);
        return analyser.analyse(feature);
    }
}
