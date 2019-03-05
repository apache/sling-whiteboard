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
import java.io.FileWriter;
import java.util.Iterator;
import java.util.ServiceLoader;

import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.fs.io.Archive.Entry;
import org.apache.jackrabbit.vault.packaging.PackageManager;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.apache.jackrabbit.vault.packaging.impl.PackageManagerImpl;
import org.apache.sling.cp2fm.handlers.DefaultEntryHandler;
import org.apache.sling.cp2fm.spi.EntryHandler;
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

    private final ServiceLoader<EntryHandler> entryHandlers = ServiceLoader.load(EntryHandler.class);

    private final EntryHandler defaultEntryHandler = new DefaultEntryHandler();

    private boolean strictValidation = false;

    private int bundlesStartOrder = 0;

    private File contentPackage;

    private File outputDirectory;

    private VaultPackage vaultPackage = null;

    private Feature targetFeature = null;

    public ContentPackage2FeatureModelConverter setStrictValidation(boolean strictValidation) {
        this.strictValidation = strictValidation;
        return this;
    }

    public boolean isStrictValidation() {
        return strictValidation;
    }

    public ContentPackage2FeatureModelConverter setBundlesStartOrder(int bundlesStartOrder) {
        this.bundlesStartOrder = bundlesStartOrder;
        return this;
    }

    public int getBundlesStartOrder() {
        return bundlesStartOrder;
    }

    public ContentPackage2FeatureModelConverter setContentPackage(File contentPackage) {
        this.contentPackage = contentPackage;
        return this;
    }

    public File getContentPackage() {
        return contentPackage;
    }

    public ContentPackage2FeatureModelConverter setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
        return this;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public Feature getTargetFeature() {
        return targetFeature;
    }

    public void convert() throws Exception {
        if (contentPackage == null) {
            throw new IllegalStateException("Null content-package can not be converted.");
        }

        if (!contentPackage.exists() || !contentPackage.isFile()) {
            throw new IllegalStateException("Content-package "
                                            + contentPackage
                                            + " does not exist or it is not a valid file.");
        }

        if (outputDirectory == null) {
            throw new IllegalStateException("Null output directory not supported, it must be specified.");
        }

        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw new IllegalStateException("output directory "
                                            + outputDirectory
                                            + " does not exist and can not be created, please make sure current user '"
                                            + System.getProperty("")
                                            + " has enough rights to write on the File System.");
        }

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
            process(archive);

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

    public void process(Archive archive) throws Exception {
        try {
            archive.open(strictValidation);

            Entry jcrRoot = archive.getJcrRoot();
            traverse(null, archive, jcrRoot);
        } finally {
            archive.close();
        }
    }

    private void traverse(String path, Archive archive, Entry entry) throws Exception {
        String entryPath = newPath(path, entry.getName());

        if (entry.isDirectory()) {
            for (Entry child : entry.getChildren()) {
                traverse(entryPath, archive, child);
            }

            return;
        }

        logger.info("Processing entry {}...", entryPath);

        getEntryHandlerByEntryPath(entryPath).handle(entryPath, archive, entry, this);

        logger.info("Entry {} successfully processed.", entryPath);
    }

    private static String newPath(String path, String entryName) {
        if (path == null) {
            return entryName;
        }

        return path + '/' + entryName;
    }

    private EntryHandler getEntryHandlerByEntryPath(String path) {
        Iterator<EntryHandler> entryHandlersIterator = entryHandlers.iterator();
        while (entryHandlersIterator.hasNext()) {
            EntryHandler entryHandler = entryHandlersIterator.next();

            if (entryHandler.matches(path)) {
                return entryHandler;
            }
        }

        return defaultEntryHandler;
    }

}
