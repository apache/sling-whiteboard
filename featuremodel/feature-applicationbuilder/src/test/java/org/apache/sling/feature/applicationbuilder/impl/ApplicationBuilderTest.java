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
package org.apache.sling.feature.applicationbuilder.impl;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonStructure;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

import org.apache.sling.feature.Application;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.builder.ApplicationBuilder;
import org.apache.sling.feature.builder.BuilderContext;
import org.apache.sling.feature.builder.FeatureProvider;
import org.apache.sling.feature.resolver.FrameworkResolver;
import org.apache.sling.feature.support.FeatureUtil;
import org.apache.sling.feature.support.artifact.ArtifactHandler;
import org.apache.sling.feature.support.artifact.ArtifactManager;
import org.apache.sling.feature.support.artifact.ArtifactManagerConfig;
import org.apache.sling.feature.support.json.ApplicationJSONWriter;
import org.apache.sling.feature.support.json.FeatureJSONReader;
import org.apache.sling.feature.support.json.FeatureJSONReader.SubstituteVariables;
import org.apache.sling.feature.support.resolver.FeatureResolver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Constants;

public class ApplicationBuilderTest {
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
    public void testBundleOrdering() throws Exception {
        FeatureProvider fp = new TestFeatureProvider();
        BuilderContext bc = new BuilderContext(fp);
        ArtifactManager am = ArtifactManager.getArtifactManager(new ArtifactManagerConfig());

        Feature fa = readFeature("/featureA.json", am);
        Feature fb = readFeature("/featureB.json", am);
        Feature[] features = {fa, fb};

        try (FeatureResolver fr = new FrameworkResolver(am, getFrameworkProps())) {
            Application app = ApplicationBuilder.assemble(null, bc, FeatureUtil.sortFeatures(fr, features));
            String actualJSON = writeApplication(app);

            String expectedJSON = "{\"features\":["
                    + "\"org.apache.sling.test.features:featureB:1.0.0\","
                    + "\"org.apache.sling.test.features:featureA:1.0.0\"],"
                + "\"bundles\":["
                    + "{\"id\":\"commons-io:commons-io:2.6\",\"start-level\":\"10\",\"start-order\":\"10\"},"
                    + "{\"id\":\"org.apache.felix:org.apache.felix.http.servlet-api:1.1.2\",\"start-level\":\"15\",\"start-order\":\"15\"},"
                    + "{\"id\":\"commons-fileupload:commons-fileupload:1.3.3\",\"start-level\":\"16\",\"start-order\":\"16\"}]}";

            StringWriter expectedWriter = new StringWriter();
            StringWriter actualWriter = new StringWriter();

            canonicalize(expectedJSON, expectedWriter, actualJSON, actualWriter);
            assertEquals(expectedWriter.toString(), actualWriter.toString());
        }
    }

    @Test
    public void testFeatureDependency() throws Exception {
        FeatureProvider fp = new TestFeatureProvider();
        BuilderContext bc = new BuilderContext(fp);
        ArtifactManager am = ArtifactManager.getArtifactManager(new ArtifactManagerConfig());

        // Feature D has a bundle (slf4j-api) with a dependency on feature C,
        // which provides a package for slf4j
        Feature fc = readFeature("/featureC.json", am);
        Feature fd = readFeature("/featureD.json", am);
        Feature[] features = {fd, fc};

        try (FeatureResolver fr = new FrameworkResolver(am, getFrameworkProps())) {
            Application app = ApplicationBuilder.assemble(null, bc, FeatureUtil.sortFeatures(fr, features));
            String genApp = writeApplication(app);

            String expected = "{\"features\":["
                    + "\"org.apache.sling.test.features:featureC:1.0.0\","
                    + "\"org.apache.sling.test.features:featureD:1.0.0\"],"
                    + "\"bundles\":[{\"id\":\"org.slf4j:slf4j-api:1.7.25\",\"start-level\":\"6\",\"start-order\":\"6\"}]}";

            StringWriter expectedWriter = new StringWriter();
            StringWriter actualWriter = new StringWriter();
            canonicalize(expected, expectedWriter, genApp, actualWriter);

            assertEquals(expectedWriter.toString(), actualWriter.toString());
        }
    }

    // Turn JSON into pretty-formatted canoncical JSON that should be comparable using String compare
    private void canonicalize(String expected, StringWriter expectedWriter, String actual, StringWriter actualWriter) {
        JsonStructure es = Json.createReader(new StringReader(expected)).read();
        JsonStructure ea = Json.createReader(new StringReader(actual)).read();

        JsonWriterFactory writerFactory = Json.createWriterFactory(
                Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true));
        JsonWriter w = writerFactory.createWriter(expectedWriter);
        w.write(es);

        JsonWriter w2 = writerFactory.createWriter(actualWriter);
        w2.write(ea);
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

    private static String writeApplication(Application app) throws Exception {
        Writer writer = new StringWriter();
        ApplicationJSONWriter.write(writer, app);
        return writer.toString();
    }

    private static class TestFeatureProvider implements FeatureProvider {
        @Override
        public Feature provide(ArtifactId id) {
            return null;
        }
    }
}
