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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.fs.io.Archive.Entry;
import org.apache.jackrabbit.vault.packaging.Dependency;
import org.apache.jackrabbit.vault.packaging.PackageManager;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.apache.jackrabbit.vault.packaging.impl.PackageManagerImpl;
import org.apache.maven.model.Model;
import org.apache.sling.cp2fm.handlers.DefaultEntryHandler;
import org.apache.sling.cp2fm.spi.EntryHandler;
import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Configurations;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.io.json.FeatureJSONWriter;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentPackage2FeatureModelConverter {

    public static final String POM_TYPE = "pom";

    private static final String ZIP_TYPE = "zip";

    public static final String GROUP_ID = "groupId";

    public static final String ARTIFACT_ID = "artifactId";

    public static final String VERSION = "version";

    private static final String FEATURE_CLASSIFIER = "cp2fm-converted-feature";

    private static final String SLING_OSGI_FEATURE_TILE_TYPE = "slingosgifeature";

    private static final String JSON_FILE_EXTENSION = ".json";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final PackageManager packageManager = new PackageManagerImpl();

    private final ServiceLoader<EntryHandler> entryHandlers = ServiceLoader.load(EntryHandler.class);

    private final EntryHandler defaultEntryHandler = new DefaultEntryHandler();

    private final Map<String, Configurations> runModes = new HashMap<>();

    private final Set<String> dependencies = new HashSet<>();

    private boolean strictValidation = false;

    private int bundlesStartOrder = 0;

    private File outputDirectory;

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

    public Configurations getRunMode(String runMode) {
        return runModes.computeIfAbsent(runMode, k -> new Configurations());
    }

    public void convert(File contentPackage) throws Exception {
        if (contentPackage == null) {
            throw new IllegalArgumentException("Null content-package can not be converted.");
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
                                            + System.getProperty("user.name")
                                            + " has enough rights to write on the File System.");
        }

        logger.info("Reading content-package '{}'...", contentPackage);

        VaultPackage vaultPackage = null;
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

            process(vaultPackage);

            // attach all unmatched resources as new content-package

            File deflatedDir = new File(outputDirectory, DefaultEntryHandler.TMP_DEFLATED);

            if (deflatedDir.listFiles().length > 0) {
                Archiver archiver = new JarArchiver();

                File destFile = File.createTempFile(targetFeature.getId().getArtifactId(), '.' + ZIP_TYPE);

                archiver.setDestFile(destFile);
                archiver.addFileSet(new DefaultFileSet(deflatedDir));
                archiver.createArchive();

                try (InputStream input = new FileInputStream(destFile)) {
                    deployLocallyAndAttach(input,
                                           targetFeature.getId().getGroupId(),
                                           targetFeature.getId().getArtifactId(),
                                           targetFeature.getId().getVersion(),
                                           FEATURE_CLASSIFIER,
                                           ZIP_TYPE);
                } finally {
                    destFile.delete();
                }

                Model model = new Model();
                model.setGroupId(targetFeature.getId().getGroupId());
                model.setArtifactId(targetFeature.getId().getArtifactId());
                model.setVersion(targetFeature.getId().getVersion());
                model.setPackaging(ZIP_TYPE);

                // TODO deploy the pom in the local dir
            } else {
                logger.info("No resources to be repackaged.");
            }

            // finally serialize the Feature Model(s) file(s)

            seralize(targetFeature);

            if (!runModes.isEmpty()) {
                for (java.util.Map.Entry<String, Configurations> runMode : runModes.entrySet()) {
                    Feature runModeFeature = new Feature(new ArtifactId(targetFeature.getId().getGroupId(),
                                                                        targetFeature.getId().getArtifactId() + '-' + runMode.getKey(),
                                                                        targetFeature.getId().getVersion(),
                                                                        targetFeature.getId().getClassifier(),
                                                                        targetFeature.getId().getType()));

                    runModeFeature.setDescription(targetFeature.getDescription() + " - " + runMode.getKey());

                    runModeFeature.getConfigurations().addAll(runMode.getValue());

                    seralize(runModeFeature);
                }
            }
        } finally {
            if (vaultPackage != null && !vaultPackage.isClosed()) {
                vaultPackage.close();
            }
        }
    }

    private void seralize(Feature feature) throws Exception {
        File targetFile = new File(outputDirectory, feature.getId().getArtifactId() + JSON_FILE_EXTENSION);

        logger.info("Conversion complete!", targetFile);
        logger.info("Writing resulting Feature File to '{}'...", targetFile);

        try (FileWriter targetWriter = new FileWriter(targetFile)) {
            FeatureJSONWriter.write(targetWriter, feature);

            logger.info("'{}' Feature File successfully written!", targetFile);
        }
    }

    public void process(VaultPackage vaultPackage) throws Exception {
        dependencies.remove(vaultPackage.getId().toString());

        for (Dependency dependency : vaultPackage.getDependencies()) {
            dependencies.add(dependency.toString());
        }

        AccessControlHandling ach = vaultPackage.getACHandling();

        MetaInf metaInf = vaultPackage.getMetaInf();
        WorkspaceFilter filter = metaInf.getFilter();
        for (PathFilterSet pathFilterSet : filter.getFilterSets()) {
            // TODO
        }

        PackageProperties properties = vaultPackage.getProperties();

        Archive archive = vaultPackage.getArchive();
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

    public void deployLocallyAndAttach(InputStream input,
                                       String groupId,
                                       String artifactId,
                                       String version,
                                       String classifier,
                                       String type) throws IOException {
        deployLocally(input, groupId, artifactId, version, classifier, type);

        Artifact artifact = new Artifact(new ArtifactId(groupId, artifactId, version, classifier, type));
        artifact.setStartOrder(bundlesStartOrder);
        getTargetFeature().getBundles().add(artifact);
    }

    public void deployLocally(InputStream input,
                              String groupId,
                              String artifactId,
                              String version,
                              String classifier,
                              String type) throws IOException {
        File targetDir = new File(getOutputDirectory(), "bundles");

        StringTokenizer tokenizer = new StringTokenizer(groupId, ".");
        while (tokenizer.hasMoreTokens()) {
            String current = tokenizer.nextToken();
            targetDir = new File(targetDir, current);
        }

        targetDir = new File(targetDir, artifactId);

        targetDir = new File(targetDir, version);

        targetDir.mkdirs();

        StringBuilder nameBuilder = new StringBuilder()
                                    .append(artifactId)
                                    .append('-')
                                    .append(version);

        if (classifier != null) {
            nameBuilder.append('-').append(classifier);
        }

        nameBuilder.append('.').append(type);

        File targetFile = new File(targetDir, nameBuilder.toString());

        logger.info("Writing data to {}...", targetFile);

        try (FileOutputStream targetStream = new FileOutputStream(targetFile)) {
            IOUtils.copy(input, targetStream);
        }

        logger.info("Data successfully written to {}.", targetFile);
    }

}
