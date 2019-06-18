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
package org.apache.sling.feature.r2f.impl;

import static java.nio.file.Files.newBufferedReader;
import static org.apache.sling.feature.io.json.FeatureJSONReader.read;
import static org.apache.sling.feature.io.json.FeatureJSONWriter.write;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.felix.inventory.Format;
import org.apache.felix.inventory.InventoryPrinter;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.r2f.ConversionRequest;
import org.apache.sling.feature.r2f.DefaultConversionRequest;
import org.apache.sling.feature.r2f.RuntimeEnvironment2FeatureModel;
import org.osgi.framework.BundleContext;

abstract class AbstractRuntimeEnvironment2FeatureModelPrinter implements InventoryPrinter {

    private static final String SLING_FEATURE_PROPERTY_NAME = "sling.feature";

    private final RuntimeEnvironment2FeatureModel generator;

    private final BundleContext bundleContext;

    public AbstractRuntimeEnvironment2FeatureModelPrinter(RuntimeEnvironment2FeatureModel generator,
                                                          BundleContext bundleContext) {
        this.generator = generator;
        this.bundleContext = bundleContext;
    }

    protected final BundleContext getBundleContext() {
        return bundleContext;
    }

    @Override
    public final void print(PrintWriter printWriter, Format format, boolean isZip) {
        String previousFeatureLocation = getBundleContext().getProperty(SLING_FEATURE_PROPERTY_NAME);
        URI previousFeatureURI = URI.create(previousFeatureLocation);
        Path previousFeaturePath = Paths.get(previousFeatureURI);
        Feature previousFeature = null;

        try (Reader reader = newBufferedReader(previousFeaturePath)) {
            previousFeature = read(reader, previousFeatureLocation);
        } catch (IOException e) {
            throw new RuntimeException("An error occurred while reading 'sling.feature' framework-property "
                    + previousFeatureLocation
                    + ", see causing error(s):",
                    e);
        }

        String groupId = previousFeature.getId().getGroupId();
        String artifactId = previousFeature.getId().getArtifactId();
        String version = previousFeature.getId().getArtifactId();
        String classifier = previousFeature.getId().getArtifactId() + "-RUNTIME";

        ConversionRequest request = new DefaultConversionRequest()
                                    .setBundleContext(bundleContext)
                                    .setResultId(new ArtifactId(groupId, artifactId, version, classifier, null));
        Feature currentFeature = generator.scanAndAssemble(request);

        Feature computedFeature = compute(previousFeature, currentFeature);

        try {
            write(printWriter, computedFeature);
        } catch (IOException e) {
            printWriter.append("An error occured while searlizing ")
                       .append(computedFeature.toString())
                       .append(":\n");

            e.printStackTrace(printWriter);
        }
    }

    protected abstract Feature compute(Feature previousFeature, Feature currentFeature);

}
