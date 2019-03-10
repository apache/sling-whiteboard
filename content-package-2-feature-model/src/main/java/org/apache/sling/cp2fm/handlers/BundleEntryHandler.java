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

import static org.apache.jackrabbit.vault.packaging.PackageProperties.NAME_VERSION;

import static org.apache.sling.cp2fm.ContentPackage2FeatureModelConverter.NAME_ARTIFACT_ID;
import static org.apache.sling.cp2fm.ContentPackage2FeatureModelConverter.NAME_GROUP_ID;
import static org.apache.sling.cp2fm.ContentPackage2FeatureModelConverter.POM_TYPE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.fs.io.Archive.Entry;
import org.apache.sling.cp2fm.ContentPackage2FeatureModelConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BundleEntryHandler extends AbstractRegexEntryHandler {

    private static final String JAR_TYPE = "jar";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Pattern pomPropertiesPattern = Pattern.compile("META-INF/maven/[^/]+/[^/]+/pom.properties");

    private final Pattern pomXmlPattern = Pattern.compile("META-INF/maven/[^/]+/[^/]+/pom.xml");

    public BundleEntryHandler() {
        super("(jcr_root)?/apps/[^/]+/install/.+\\.jar");
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

        String groupId = getTrimmedProperty(properties, NAME_GROUP_ID);
        String artifactId = getTrimmedProperty(properties, NAME_ARTIFACT_ID);
        String version = getTrimmedProperty(properties, NAME_VERSION);

        try (InputStream input = archive.openInputStream(entry)) {
            converter.deployLocallyAndAttach(input, groupId, artifactId, version, null, JAR_TYPE);
        }

        if (pomXml != null) {
            try (ByteArrayInputStream input = new ByteArrayInputStream(pomXml)) {
                converter.deployLocally(input, groupId, artifactId, version, null, POM_TYPE);
            }
        }
    }

    private static String getTrimmedProperty(Properties properties, String name) {
        return properties.getProperty(name).trim();
    }

}
