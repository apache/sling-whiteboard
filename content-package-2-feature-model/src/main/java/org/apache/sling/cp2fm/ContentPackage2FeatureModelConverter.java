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
package org.apache.sling.cp2fm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.vault.fs.api.VaultInputSource;
import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.fs.io.Archive.Entry;
import org.apache.jackrabbit.vault.packaging.PackageManager;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.apache.jackrabbit.vault.packaging.impl.PackageManagerImpl;
import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.io.json.FeatureJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ContentPackage2FeatureModelConverter {

    private static final String FEATURE_CLASSIFIER = "cp2fm-converted-feature";

    private static final String SLING_OSGI_FEATURE_TILE_TYPE = "slingosgifeature";

    private static final String JSON_FILE_EXTENSION = ".json";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final PackageManager packageManager = new PackageManagerImpl();

    private final Pattern bundlesLocationPattern = Pattern.compile("jcr_root/apps/[^/]+/install/.+\\.jar");

    private final Pattern embeddedPackagesLocationPattern = Pattern.compile("jcr_root/etc/packages/.+\\.zip");

    private final Pattern pomPropertiesPattern = Pattern.compile("META-INF/maven/[^/]+/[^/]+/pom.properties");

    private boolean strictValidation = false;

    public ContentPackage2FeatureModelConverter setStrictValidation(boolean strictValidation) {
        this.strictValidation = strictValidation;
        return this;
    }

    public void convert(File contentPackage, File outputDirectory) throws IOException {
        if (contentPackage == null) {
            throw new IllegalArgumentException("Null content-package can not be converted.");
        }
        if (outputDirectory == null) {
            throw new IllegalArgumentException("Null output directory not supported, it must be specified.");
        }

        if (!contentPackage.exists() || !contentPackage.isFile()) {
            throw new IllegalArgumentException("Content-package "
                                               + contentPackage
                                               + " does not exist or it is not a valid file.");
        }

        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw new IllegalStateException("output directory "
                                            + outputDirectory
                                            + " does not exist and can not be created, please make sure current user '"
                                            + System.getProperty("")
                                            + " has enough rights to write on the File System.");
        }

        VaultPackage vaultPackage = null;
        Feature targetFeature = null;

        logger.info("Reading content-package '{}'...", contentPackage);

        try {
            vaultPackage = packageManager.open(contentPackage, strictValidation);

            logger.info("content-package '{}' successfully read!", contentPackage);

            PackageProperties packageProperties = vaultPackage.getProperties();

            targetFeature = new Feature(new ArtifactId(packageProperties.getProperty("groupId"), 
                                                       packageProperties.getProperty(PackageProperties.NAME_NAME),
                                                       packageProperties.getProperty(PackageProperties.NAME_VERSION),
                                                       FEATURE_CLASSIFIER,
                                                       SLING_OSGI_FEATURE_TILE_TYPE));

            targetFeature.setDescription(packageProperties.getDescription());

            logger.info("Converting content-package '{}' to Feature File '{}'...", vaultPackage.getId(), targetFeature.getId());

            Archive archive = vaultPackage.getArchive();
            process(archive, outputDirectory, targetFeature);

            File targetFile = new File(outputDirectory, targetFeature.getId().getArtifactId() + JSON_FILE_EXTENSION);

            logger.info("Conversion complete!", targetFile);
            logger.info("Writing resulting Feature File to '{}'...", targetFile);

            try (FileWriter targetWriter = new FileWriter(targetFile)) {
                FeatureJSONWriter.write(targetWriter, targetFeature);

                logger.info("'{}' Feature File successfully written!", targetFile);
            }
        } finally {
            if (vaultPackage != null && !vaultPackage.isClosed()) {
                vaultPackage.close();
            }
        }
    }

    private void process(Archive archive, File outputDirectory, Feature targetFeature) throws IOException {
        try {
            archive.open(strictValidation);

            Entry jcrRoot = archive.getJcrRoot();
            traverse(archive, jcrRoot, outputDirectory, targetFeature);
        } finally {
            archive.close();
        }
    }

    private void traverse(Archive archive, Entry entry, File outputDirectory, Feature targetFeature) throws IOException {
        if (entry.isDirectory()) {
            for (Entry child : entry.getChildren()) {
                traverse(archive, child, outputDirectory, targetFeature);
            }

            return;
        }

        VaultInputSource inputSource = archive.getInputSource(entry);
        String sourceSystemId = inputSource.getSystemId();
        logger.info("Found {} entry", sourceSystemId);

        if (bundlesLocationPattern.matcher(sourceSystemId).matches()) {
            onBundle(archive, entry, outputDirectory, targetFeature);
        } else if (embeddedPackagesLocationPattern.matcher(sourceSystemId).matches()) {
            onContentPackage(archive, entry, outputDirectory, targetFeature);
        } else {
            // TODO
        }
    }

    private void onBundle(Archive archive, Entry entry, File outputDirectory, Feature targetFeature) throws IOException {
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

        File target = new File(outputDirectory, "bundles");

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

        targetFeature.getBundles().add(new Artifact(new ArtifactId(groupId, artifactId, version, null, null)));
    }

    private static String getTrimmedProperty(Properties properties, String name) {
        return properties.getProperty(name).trim();
    }

    private void onContentPackage(Archive archive, Entry entry, File outputDirectory, Feature targetFeature) throws IOException {
        logger.info("Processing content-package {}...", entry.getName());
    }

}
