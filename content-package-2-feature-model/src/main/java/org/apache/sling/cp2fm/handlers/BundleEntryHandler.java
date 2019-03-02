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
package org.apache.sling.cp2fm.handlers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.fs.io.Archive.Entry;
import org.apache.sling.cp2fm.ContentPackage2FeatureModelConverter;
import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;

public final class BundleEntryHandler extends AbstractRegexEntryHandler {

    private final Pattern pomPropertiesPattern = Pattern.compile("META-INF/maven/[^/]+/[^/]+/pom.properties");

    public BundleEntryHandler() {
        super("jcr_root/apps/[^/]+/install/.+\\.jar");
    }

    @Override
    public void handle(Archive archive, Entry entry, ContentPackage2FeatureModelConverter converter) throws IOException {
        logger.info("Processing bundle {}...", entry.getName());

        Properties properties = new Properties();

        try (JarInputStream jarInput = new JarInputStream(archive.openInputStream(entry))) {
            dance: while (jarInput.available() > 0) {
                JarEntry jarEntry = jarInput.getNextJarEntry();

                if (pomPropertiesPattern.matcher(jarEntry.getName()).matches()) {
                    properties.load(jarInput);
                    break dance;
                }
            }
        }

        File target = new File(converter.getOutputDirectory(), "bundles");

        String groupId = getTrimmedProperty(properties, "groupId");
        StringTokenizer tokenizer = new StringTokenizer(groupId, ".");
        while (tokenizer.hasMoreTokens()) {
            String current = tokenizer.nextToken();
            target = new File(target, current);
        }

        String artifactId = getTrimmedProperty(properties, "artifactId");
        target = new File(target, artifactId);

        String version = getTrimmedProperty(properties, "version");
        target = new File(target, version);

        target.mkdirs();

        target = new File(target, String.format("%s-%s.jar", artifactId, version));

        try (InputStream input = archive.openInputStream(entry);
                FileOutputStream targetStream = new FileOutputStream(target)) {
            IOUtils.copy(input, targetStream);
        }

        Artifact artifact = new Artifact(new ArtifactId(groupId, artifactId, version, null, null));
        artifact.setStartOrder(converter.getBundlesStartOrder());
        converter.getTargetFeature().getBundles().add(artifact);
    }

    private static String getTrimmedProperty(Properties properties, String name) {
        return properties.getProperty(name).trim();
    }

}
