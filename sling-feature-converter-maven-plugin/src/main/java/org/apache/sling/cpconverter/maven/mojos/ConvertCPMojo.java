/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.cpconverter.maven.mojos;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.transfer.artifact.install.ArtifactInstallerException;
import org.apache.sling.feature.cpconverter.ContentPackage2FeatureModelConverter;
import org.apache.sling.feature.cpconverter.acl.DefaultAclManager;
import org.apache.sling.feature.cpconverter.artifacts.DefaultArtifactsDeployer;
import org.apache.sling.feature.cpconverter.features.DefaultFeaturesManager;
import org.apache.sling.feature.cpconverter.handlers.DefaultEntryHandlersManager;
import org.apache.sling.feature.cpconverter.vltpkg.DefaultPackagesEventsEmitter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.sling.feature.cpconverter.ContentPackage2FeatureModelConverter.PACKAGE_CLASSIFIER;
import static org.apache.sling.feature.cpconverter.ContentPackage2FeatureModelConverter.ZIP_TYPE;

/**
 * Converts the given Content Packages into Feature Models
 * and places the converted Content Package into the local
 * Maven Repository
 */
@Mojo(
    name = "convert-cp",
    requiresProject = true,
    threadSafe = true
)
public final class ConvertCPMojo
    extends AbstractBaseMojo
{
    public static final String CFG_STRICT_VALIDATION = "strictValidation";

    public static final String CFG_MERGE_CONFIGURATIONS = "mergeConfigurations";

    public static final String CFG_BUNDLE_START_ORDER = "bundleStartOrder";

    public static final String CFG_ARTIFACT_ID_OVERRIDE = "artifactIdOverride";

    public static final String CFG_INSTALL_CONVERTED_PACKAGE = "installConvertedContentPackage";

    public static final String CFG_FM_OUTPUT_DIRECTORY = "featureModelsOutputDirectory";

    public static final String CFG_CONVERTED_CP_OUTPUT_DIRECTORY = "convertedContentPackageOutputDirectory";

    public static final String CFG_CONVERTED_CP_FM_PREFIX = "fmPrefix";

    public static final String CFG_SYSTEM_PROPERTIES = "cpSystemProperties";

    public static final String CFG_CONTENT_PACKAGES = "packages";

    public static final boolean DEFAULT_STRING_VALIDATION = false;

    public static final boolean DEFAULT_MERGE_CONFIGURATIONS = false;

    public static final int DEFAULT_BUNDLE_START_ORDER = 20;

    public static final boolean DEFAULT_INSTALL_CONVERTED_PACKAGE = true;

    public static final String DEFAULT_CONVERTED_CP_OUTPUT_DIRECTORY = "${project.build.directory}/cp-conversion";

    public static final String DEFAULT_FM_OUTPUT_DIRECTORY = DEFAULT_CONVERTED_CP_OUTPUT_DIRECTORY + "/fm.out";

    /**
     * If set to {@code true} the Content Package is strictly validated.
     */
    @Parameter(property = CFG_STRICT_VALIDATION, defaultValue = DEFAULT_STRING_VALIDATION + "")
    private boolean strictValidation;

    //AS TODO: Is that applicable to a single CP ?
    /**
     * If set to {@code true} the OSGi Configurations with same PID are merged.
     */
    @Parameter(property = CFG_MERGE_CONFIGURATIONS, defaultValue = DEFAULT_MERGE_CONFIGURATIONS + "")
    private boolean mergeConfigurations;

    /**
     * If set to {@code true} the Content Package is strictly validated.
     */
    @Parameter(property = CFG_BUNDLE_START_ORDER, defaultValue = DEFAULT_BUNDLE_START_ORDER + "")
    private int bundleStartOrder;

    /**
     * If set to {@code true} the Content Package is strictly validated.
     */
    @Parameter(property = CFG_ARTIFACT_ID_OVERRIDE)
    private String artifactIdOverride;

    /**
     * Target directory for the converted Content Package Feature Model file
     */
    @Parameter(property = CFG_FM_OUTPUT_DIRECTORY, defaultValue = DEFAULT_FM_OUTPUT_DIRECTORY)
    private File fmOutput;

    /**
     * Target directory for the Converted Content Package file
     */
    @Parameter(property = CFG_CONVERTED_CP_OUTPUT_DIRECTORY, defaultValue = DEFAULT_CONVERTED_CP_OUTPUT_DIRECTORY)
    private File convertedCPOutput;

    /**
     * If set to {@code true} the converted Content Package will be installed in the local Maven Repository
     */
    @Parameter(property = CFG_INSTALL_CONVERTED_PACKAGE, defaultValue = DEFAULT_INSTALL_CONVERTED_PACKAGE + "")
    private boolean installConvertedCP;

    /**
     * System Properties to hand over to the Content Package Converter
     */
    @Parameter(property = CFG_CONVERTED_CP_FM_PREFIX)
    private String fmPrefix;

    /**
     * System Properties to hand over to the Content Package Converter
     */
    @Parameter(property = CFG_SYSTEM_PROPERTIES)
    private List<String> systemProperties;

    /**
     * List of Content Packages to be converted
     */
    @Parameter(property = CFG_CONTENT_PACKAGES)
    private List<ContentPackage> contentPackages;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // Un-encode a given Artifact Override Id
        if(artifactIdOverride != null) {
            String old = artifactIdOverride;
            artifactIdOverride = artifactIdOverride.replaceAll("\\$\\{\\{", "\\$\\{");
            artifactIdOverride = artifactIdOverride.replaceAll("}}", "}");
            getLog().info("Replaced Old Artifact Id Override: '" + old + "', with new one: '" + artifactIdOverride + "'");
        }
        // Parse the System Properties if provided
        Map<String,String> properties = new HashMap<>();
        if(systemProperties != null) {
            for(String systemProperty: systemProperties) {
                if(systemProperty != null) {
                    int index = systemProperty.indexOf("=");
                    if(index > 0 && index < systemProperty.length() - 1) {
                        String key = systemProperty.substring(0, index);
                        String value = systemProperty.substring(index + 1);
                        properties.put(key, value);
                    }
                }
            }
        }
        try {
            ContentPackage2FeatureModelConverter converter = new ContentPackage2FeatureModelConverter(strictValidation)
                .setFeaturesManager(
                    new DefaultFeaturesManager(
                        mergeConfigurations,
                        bundleStartOrder,
                        fmOutput,
                        artifactIdOverride,
//                        fmPrefix,
                        properties
                    )
                )
                .setBundlesDeployer(
                    new DefaultArtifactsDeployer(
                        convertedCPOutput
                    )
                )
                .setEntryHandlersManager(
                    new DefaultEntryHandlersManager()
                )
                .setAclManager(
                    new DefaultAclManager()
                )
                .setEmitter(DefaultPackagesEventsEmitter.open(fmOutput));

            if(contentPackages == null || contentPackages.isEmpty()) {
                getLog().info("Project Artifact File: " + project.getArtifact());
                String targetPath = project.getModel().getBuild().getDirectory() + "/"
                    + project.getModel().getBuild().getFinalName()
                    + "." + ZIP_TYPE;
                File targetFile = new File(targetPath);
                if (targetFile.exists() && targetFile.isFile() && targetFile.canRead()) {
                    converter.convert(project.getArtifact().getFile());
//                    Artifact convertedPackage = new DefaultArtifact(
//                        project.getGroupId(), project.getArtifactId(), project.getVersion(),
//                        "compile", ZIP_TYPE, PACKAGE_CLASSIFIER, artifactHandlerManager.getArtifactHandler(ZIP_TYPE)
//                    );
//                    installConvertedCP(convertedPackage);
                } else {
                    getLog().error("Artifact is not found: " + targetPath);
                }
            } else {
                for(ContentPackage contentPackage: contentPackages) {
                    getLog().info("Content Package Artifact File: " + contentPackage.toString());
                    contentPackage.setExcludeTransitive(true);
                    final Collection<Artifact> artifacts = contentPackage.getMatchingArtifacts(project);
                    if (artifacts.isEmpty()) {
                        getLog().warn("No matching artifacts for " + contentPackage);
                        continue;
                    }
                    getLog().info("Target Convert CP of --- " + contentPackage + " ---");
                    for (final Artifact artifact : artifacts) {
                        final File source = artifact.getFile();
                        if (source.exists() && source.isFile() && source.canRead()) {
                            converter.convert(source);
                            Artifact convertedPackage = new DefaultArtifact(
                                artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                                "compile", ZIP_TYPE, PACKAGE_CLASSIFIER, artifactHandlerManager.getArtifactHandler(ZIP_TYPE)
                            );
//                            installConvertedCP(convertedPackage);
                        } else {
                            getLog().error("Artifact is not found: " + artifact);
                        }
                    }
                }
            }
            installGeneratedArtifacts();
        } catch (Throwable t) {
            throw new MojoExecutionException("Content Package Converter Exception", t);
        }

    }

    /**
     * For now this is a hack to update the local Maven Repo (.m2/repository) correctly
     * bypassing the Maven Installer as this is giving us grief
     */
    private void installGeneratedArtifacts() {
        if(installConvertedCP) {
            File userHome = new File(System.getProperty("user.home"));
            if(userHome.isDirectory()) {
                File destFolder = new File(userHome, ".m2/repository");
                if(destFolder.isDirectory()) {
                    copyFiles(convertedCPOutput, destFolder);
                }
            }
        }
    }

    /**
     * Copies the local generated Maven Artifacts into the local Maven Repo
     * recursively one folder at the time.
     * Folder are created on the local Maven Repo if not found, missing files
     * are copied over but only CP2FM Converted CP files are replaced on the
     * target folders
     *
     * @param source Folder with the source files / folders
     * @param destination Destination folder (inside the local Maven repo). This is the corresponding folder to the source
     */
    private void copyFiles(File source, File destination) {
        for(File file: source.listFiles()) {
            String name = file.getName();
            if(file.isDirectory()) {
                File newDest = new File(destination, name);
                if(!newDest.exists()) {
                    newDest.mkdirs();
                }
                if(newDest.isDirectory()) {
                    copyFiles(file, newDest);
                } else {
                    getLog().warn("Source File: '" + file.getAbsolutePath() + "' is a folder but its counterpart is a file: " + newDest.getAbsolutePath());
                }
            } else {
                File newDest = new File(destination, name);
                if(!newDest.exists()) {
                    // Copy File over
                    try {
                        Files.copy(file.toPath(), newDest.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
                    } catch (IOException e) {
                        getLog().warn("Failed to copy File: '" + file.getAbsolutePath() + "' to File: " + newDest.getAbsolutePath(), e);
                    }
                } else {
                    // We only overwrite converted files
                    if(name.endsWith(PACKAGE_CLASSIFIER + "." + ZIP_TYPE)) {
                        // Copy File over
                        try {
                            Files.copy(file.toPath(), newDest.toPath(), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            getLog().warn("Failed to copy generated File: '" + file.getAbsolutePath() + "' to File: " + newDest.getAbsolutePath(), e);
                        }
                    } else {
                        getLog().info("Ignore File: '" + file.getAbsolutePath());
                    }
                }
            }
        }
    }

    private void installConvertedCP(Artifact artifact) throws MojoFailureException, MojoExecutionException {
        if(installConvertedCP) {
            Collection<Artifact> artifacts = Collections.synchronizedCollection(new ArrayList<>());
            // Rebuild the converted package path
            String convertedPackagePath = artifact.getGroupId().replaceAll("\\.", "/")
                + "/" + artifact.getArtifactId()
                + "/" + artifact.getVersion()
                + "/" + artifact.getArtifactId()
                + "-" + artifact.getVersion()
                + "-" + PACKAGE_CLASSIFIER
                + "." + ZIP_TYPE;
            File convertedPackageFile = new File(convertedCPOutput, convertedPackagePath);
            if(convertedPackageFile.exists() && convertedPackageFile.canRead()) {
                artifact.setFile(convertedPackageFile);
                artifacts.add(artifact);
                installArtifact(mavenSession.getProjectBuildingRequest(), artifacts);
            } else {
                getLog().error("xCould not find Converted Package: " + convertedPackageFile);
            }
        }
    }

    private void installArtifact(ProjectBuildingRequest pbr, Collection<Artifact> artifacts )
        throws MojoFailureException, MojoExecutionException
    {
        try
        {
            installer.install(pbr, artifacts);
        }
        catch ( ArtifactInstallerException e )
        {
            throw new MojoExecutionException( "ArtifactInstallerException", e );
        }
    }

}
