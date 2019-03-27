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
package org.apache.sling.feature.cpconverter.handlers;

import static org.apache.jackrabbit.vault.packaging.PackageProperties.NAME_VERSION;
import static org.apache.sling.feature.cpconverter.ContentPackage2FeatureModelConverter.NAME_ARTIFACT_ID;
import static org.apache.sling.feature.cpconverter.ContentPackage2FeatureModelConverter.NAME_GROUP_ID;
import static org.apache.sling.feature.cpconverter.ContentPackage2FeatureModelConverter.POM_TYPE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.fs.io.Archive.Entry;
import org.apache.sling.feature.cpconverter.ContentPackage2FeatureModelConverter;
import org.apache.sling.feature.cpconverter.spi.ArtifactWriter;
import org.apache.sling.feature.cpconverter.writers.InputStreamArtifactWriter;
import org.apache.sling.feature.cpconverter.writers.MavenPomSupplierWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BundleEntryHandler extends AbstractRegexEntryHandler {

    private static final String JAR_TYPE = "jar";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Pattern pomPropertiesPattern = Pattern.compile("META-INF/maven/[^/]+/[^/]+/pom.properties");

    private final Pattern pomXmlPattern = Pattern.compile("META-INF/maven/[^/]+/[^/]+/pom.xml");

    public BundleEntryHandler() {
        super("(jcr_root)?/apps/[^/]+/install(\\.([^/]+))?/.+\\.jar");
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

        String groupId = getCheckedProperty(properties, NAME_GROUP_ID);
        String artifactId = getCheckedProperty(properties, NAME_ARTIFACT_ID);
        String version = getCheckedProperty(properties, NAME_VERSION);

        Matcher matcher = getPattern().matcher(path);
        String runMode = null;
        // we are pretty sure it matches, here
        if (matcher.matches()) {
            // there is a specified RunMode
            runMode = matcher.group(3);
        } else {
            throw new IllegalStateException("Something went terribly wrong: pattern '"
                                            + getPattern().pattern()
                                            + "' should have matched already with path '"
                                            + path
                                            + "' but it does not, currently");
        }

        try (InputStream input = archive.openInputStream(entry)) {
            converter.getArtifactDeployer().deploy(new InputStreamArtifactWriter(input),
                                                   groupId,
                                                   artifactId,
                                                   version,
                                                   null,
                                                   JAR_TYPE);

            converter.attach(runMode,
                             groupId,
                             artifactId,
                             version,
                             null,
                             JAR_TYPE);
        }

        ArtifactWriter pomWriter;
        if (pomXml == null) {
            pomWriter = new MavenPomSupplierWriter(groupId, artifactId, version, JAR_TYPE);
        } else {
            pomWriter = new InputStreamArtifactWriter(new ByteArrayInputStream(pomXml));
        }

        converter.getArtifactDeployer().deploy(pomWriter, groupId, artifactId, version, null, POM_TYPE);
    }

    private static String getCheckedProperty(Properties properties, String name) {
        String property = properties.getProperty(name).trim();
        Objects.requireNonNull(property, "Bundle can not be defined as a valid Maven artifact without specifying a valid '"
                                         + name
                                         + "' property.");
        return property;
    }

}
