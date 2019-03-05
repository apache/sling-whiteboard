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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BundleEntryHandler extends AbstractRegexEntryHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Pattern pomPropertiesPattern = Pattern.compile("META-INF/maven/[^/]+/[^/]+/pom.properties");

    private final Pattern pomXmlPattern = Pattern.compile("META-INF/maven/[^/]+/[^/]+/pom.xml");

    public BundleEntryHandler() {
        super("jcr_root/apps/[^/]+/install/.+\\.jar");
    }

    @Override
    public void handle(String path, Archive archive, Entry entry, ContentPackage2FeatureModelConverter converter) throws Exception {
        logger.info("Processing bundle {}...", entry.getName());

        Properties properties = new Properties();
        byte[] pomXml = null;

        try (JarInputStream jarInput = new JarInputStream(archive.openInputStream(entry));
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            JarEntry jarEntry;
            while ((jarEntry = jarInput.getNextJarEntry()) != null) {
                String entryName = jarEntry.getName();

                if (pomPropertiesPattern.matcher(entryName).matches()) {
                    logger.info("Reading '{}' bundle GAV from {}...", entry.getName(), entryName);

                    properties.load(jarInput);
                } else if (pomXmlPattern.matcher(entryName).matches()) {
                    logger.info("Reading '{}' POM file from {}...", entry.getName(), entryName);

                    IOUtils.copy(jarInput, baos);
                    pomXml = baos.toByteArray();
                }
            }
        }

        File targetDir = new File(converter.getOutputDirectory(), "bundles");

        String groupId = getTrimmedProperty(properties, "groupId");
        StringTokenizer tokenizer = new StringTokenizer(groupId, ".");
        while (tokenizer.hasMoreTokens()) {
            String current = tokenizer.nextToken();
            targetDir = new File(targetDir, current);
        }

        String artifactId = getTrimmedProperty(properties, "artifactId");
        targetDir = new File(targetDir, artifactId);

        String version = getTrimmedProperty(properties, "version");
        targetDir = new File(targetDir, version);

        targetDir.mkdirs();

        try (InputStream input = archive.openInputStream(entry)) {
            write(input, targetDir, artifactId, version, "jar");
        }

        if (pomXml != null) {
            try (ByteArrayInputStream input = new ByteArrayInputStream(pomXml)) {
                write(input, targetDir, artifactId, version, "pom");
            }
        }

        Artifact artifact = new Artifact(new ArtifactId(groupId, artifactId, version, null, null));
        artifact.setStartOrder(converter.getBundlesStartOrder());
        converter.getTargetFeature().getBundles().add(artifact);
    }

    private static String getTrimmedProperty(Properties properties, String name) {
        return properties.getProperty(name).trim();
    }

    private void write(InputStream input, File targetDir, String artifactId, String version, String type) throws IOException {
        File targetFile = new File(targetDir, String.format("%s-%s.%s", artifactId, version, type));

        logger.info("Writing data to {}...", targetFile);

        try (FileOutputStream targetStream = new FileOutputStream(targetFile)) {
            IOUtils.copy(input, targetStream);
        }

        logger.info("Data successfully written to {}.", targetFile);
    }

}
