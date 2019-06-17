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

import static org.apache.sling.feature.io.json.FeatureJSONWriter.write;

import java.io.IOException;
import java.io.PrintWriter;

import org.apache.felix.inventory.Format;
import org.apache.felix.inventory.InventoryPrinter;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.r2f.ConversionRequest;
import org.apache.sling.feature.r2f.DefaultConversionRequest;
import org.apache.sling.feature.r2f.RuntimeEnvironment2FeatureModel;
import org.osgi.framework.BundleContext;

public final class RuntimeEnvironment2FeatureModelPrinter implements InventoryPrinter {

    private final RuntimeEnvironment2FeatureModel converter;

    private final BundleContext bundleContext;

    public RuntimeEnvironment2FeatureModelPrinter(RuntimeEnvironment2FeatureModel converter, BundleContext bundleContext) {
        this.converter = converter;
        this.bundleContext = bundleContext;
    }

    @Override
    public void print(PrintWriter printWriter, Format format, boolean isZip) {
        // TODO
        String groupId = bundleContext.getProperty(null);
        String artifactId = bundleContext.getProperty(null);
        String version = bundleContext.getProperty(null);
        String classifier = bundleContext.getProperty(null);

        ConversionRequest request = new DefaultConversionRequest()
                                    .setBundleContext(bundleContext)
                                    .setResultId(new ArtifactId(groupId, artifactId, version, classifier, null));
        Feature currentFeature = converter.scanAndAssemble(request);

        try {
            write(printWriter, currentFeature);
        } catch (IOException e) {
            printWriter.append("An error occured while searlizing ")
                       .append(currentFeature.toString())
                       .append(":\n");

            e.printStackTrace(printWriter);
        }
    }

}
