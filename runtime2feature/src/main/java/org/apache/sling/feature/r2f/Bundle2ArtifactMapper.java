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
package org.apache.sling.feature.r2f;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Properties;
import java.util.function.Function;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.osgi.framework.Bundle;
import org.osgi.framework.startlevel.BundleStartLevel;

final class Bundle2ArtifactMapper extends AbstractFeatureElementConsumer<Artifact> implements Function<Bundle, Artifact> {

    private static final String POM_PROPERTIES_RESOURCE_NAME = "pom.properties";

    private static final String MAVEN_METADATA_PATH = "META-INF/maven";

    private static final String GROUP_ID = "groupId";

    private static final String ARTIFACT_ID = "artifactId";

    private static final String VERSION = "version";

    private static final String CLASSIFIER = "classifier";

    public Bundle2ArtifactMapper(Feature targetFeature) {
        super(targetFeature);
    }

    @Override
    public Artifact apply(Bundle bundle) {
        Properties pomProperties = new Properties();

        Enumeration<URL> pomPropertiesURLs = bundle.findEntries(MAVEN_METADATA_PATH, POM_PROPERTIES_RESOURCE_NAME, true);
        if (pomPropertiesURLs.hasMoreElements()) {
            URL pomPropertiesURL = pomPropertiesURLs.nextElement();

            try {
                URLConnection connection = pomPropertiesURL.openConnection();
                connection.connect();

                try (InputStream inStream = connection.getInputStream()) {
                    pomProperties.load(inStream);
                }

                if (connection instanceof HttpURLConnection) {
                    ((HttpURLConnection) connection).disconnect();
                }
            } catch (IOException e) {
                throw new RuntimeException("An error occurred while reading "
                                           + pomPropertiesURL
                                           + " properties file from Bundle "
                                           + bundle.getSymbolicName()
                                           + " does not export valid Maven metadata", e);
            }
        }

        if (pomProperties.isEmpty()) {
            throw new RuntimeException("Bundle "
                                      + bundle.getSymbolicName()
                                      + " does not export valid Maven metadata");
        }

        String groupId = pomProperties.getProperty(GROUP_ID);
        String artifactId = pomProperties.getProperty(ARTIFACT_ID);
        String version = pomProperties.getProperty(VERSION);
        String classifier = pomProperties.getProperty(CLASSIFIER);

        Artifact artifact = new Artifact(new ArtifactId(groupId, artifactId, version, classifier, null));

        BundleStartLevel bundleStartLevel = bundle.adapt(BundleStartLevel.class);
        int startOrder = bundleStartLevel.getStartLevel();
        artifact.setStartOrder(startOrder);

        return artifact;
    }

    @Override
    public void accept(Artifact artifact) {
        getTargetFeature().getBundles().add(artifact);
    }

}
