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
package org.apache.sling.feature.maven;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.builder.BuilderContext;
import org.apache.sling.feature.builder.FeatureBuilder;
import org.apache.sling.feature.builder.FeatureProvider;
import org.apache.sling.feature.io.json.FeatureJSONReader;
import org.apache.sling.feature.io.json.FeatureJSONReader.SubstituteVariables;
import org.codehaus.plexus.logging.Logger;

/**
 * The processor processes all feature projects.
 */
public class Preprocessor {

    /**
     * Process the provided projects.
     * @param env The environment with all maven settings and projects
     */
    public void process(final Environment env) {
        for(final ProjectInfo info : env.modelProjects.values()) {
            if ( info instanceof FeatureProjectInfo ) {
                final FeatureProjectInfo finfo = (FeatureProjectInfo)info;
                process(env, finfo, FeatureProjectConfig.getMainConfig(finfo));
                process(env, finfo, FeatureProjectConfig.getTestConfig(finfo));
                if ( FeatureConstants.PACKAGING_FEATURE.equals(info.project.getPackaging()) && finfo.feature == null ) {
                    throw new RuntimeException("Feature project has no feature defined: " + info.project.getId());
                }

                ProjectHelper.storeProjectInfo(finfo);
            } else {
                final ApplicationProjectInfo ainfo = (ApplicationProjectInfo)info;
                process(env, ainfo, ApplicationProjectConfig.getMainConfig(ainfo));
                process(env, ainfo, ApplicationProjectConfig.getTestConfig(ainfo));

                ProjectHelper.storeProjectInfo(ainfo);
            }
        }
    }

    /**
     * Process a single feature project.
     *
     * @param env The environment with all maven settings and projects
     * @param info The project to process.
     * @param config The configuration for the project.
     */
    private void process(final Environment env,
            final FeatureProjectInfo info,
            final FeatureProjectConfig config) {
        if ( (config.isTestConfig() && info.testFeatureDone == true )
             || (!config.isTestConfig() && info.featureDone == true) ) {
            env.logger.debug("Return assembled " + config.getName() + " for " + info.project.getId());
            return;
        }
        // prevent recursion and multiple processing
        if ( config.isTestConfig() ) {
            info.testFeatureDone = true;
        } else {
            info.featureDone = true;
        }
        env.logger.debug("Processing " + config.getName() + " in project " + info.project.getId());

        // read project feature, either inlined or from file
        final Feature feature = readProjectFeature(env.logger, info.project, config);
        if ( feature == null ) {
            env.logger.debug("No " + config.getName() + " found in project " + info.project.getId());
            return;
        }
        if ( config.isTestConfig() ) {
            info.testFeature = feature;
        } else {
            info.feature = feature;
        }

        // process attachments (only for jar or bundle)
        if ( "jar".equals(info.project.getPackaging())
             || "bundle".equals(info.project.getPackaging())) {
            if ( config.isSkipAddJarToFeature() ) {
                env.logger.debug("Skip adding jar to " + config.getName());
            } else {
                final Artifact jar = new Artifact(new ArtifactId(info.project.getGroupId(),
                        info.project.getArtifactId(),
                        info.project.getVersion(),
                        null,
                        "jar"));
                jar.getMetadata().put(Artifact.KEY_START_ORDER, String.valueOf(config.getJarStartLevel()));
                feature.getBundles().add(jar);
            }
        }

        final Feature assembledFeature = FeatureBuilder.assemble(feature, new BuilderContext(this.createFeatureProvider(env,
                info,
                config.isTestConfig(),
                config.isSkipAddDependencies(),
                config.getScope(), null)));
        if ( config.isTestConfig() ) {
            info.assembledTestFeature = assembledFeature;
        } else {
            info.assembledFeature = assembledFeature;
        }

        if ( config.isSkipAddDependencies() ) {
            env.logger.debug("Not adding artifacts from feature as dependencies");
        } else {
            addDependenciesFromFeature(env, info, assembledFeature, config.getScope());
        }
    }

    private void scan(final List<File> files, final File dir, final String ext) {
        for(final File f : dir.listFiles()) {
            if ( !f.getName().startsWith(".") ) {
                if ( f.isDirectory() ) {
                    scan(files, f, ext);
                } else if ( f.getName().endsWith("." + ext) ) {
                    files.add(f);
                }
            }
        }
    }

    /**
     * Process a single application project.
     *
     * @param env The environment with all maven settings and projects
     * @param info The project to process.
     * @param config The configuration for the project.
     */
    private void process(final Environment env,
            final ApplicationProjectInfo info,
            final ApplicationProjectConfig config) {
        final List<Feature> featureList = new ArrayList<>();
        env.logger.debug("Processing " + config.getName() + " in project " + info.project.getId());

        // an application supports two sets of files:
        // features and references to features

        // feature files first:
        final File dir = new File(info.project.getBasedir(), config.getFeatureDir());
        if ( dir.exists() ) {
            final List<File> files = new ArrayList<>();
            scan(files, dir, "json");

            for(final File file : files) {
                // create id in case the file does not contain one
                // classifier is the hard part, we use the file path/name
                String fileName = file.getAbsolutePath().substring(dir.getAbsolutePath().length() + 1);
                fileName = fileName.substring(0, fileName.length() - 5); // remove .json
                fileName = fileName.replace(File.separatorChar, '_');
                fileName = fileName.replace('-', '_');
                final String classifier;
                if ( config.isTestConfig() ) {
                    classifier = "test_" + fileName;
                } else {
                    classifier = fileName;
                }
                final ArtifactId id = new ArtifactId(info.project.getGroupId(),
                        info.project.getArtifactId(),
                        info.project.getVersion(),
                        classifier,
                        FeatureConstants.PACKAGING_FEATURE);

                // We should pass in an "id" to FeatureJSONReader.read and later on check the id (again, need to handle ref files)
                try (final FileReader reader = new FileReader(file)) {
                    final Feature feature = FeatureJSONReader.read(reader, id, file.getAbsolutePath(), SubstituteVariables.RESOLVE);

                    this.checkFeatureId(id, feature);

                    this.setProjectInfo(info.project, feature);
                    this.postProcessReadFeature(feature);
                    featureList.add(feature);

                } catch ( final IOException io) {
                    throw new RuntimeException("Unable to read feature " + file.getAbsolutePath(), io);
                }
            }
        } else {
            env.logger.debug("Feature directory " + config.getFeatureDir() + " does not exist in project " + info.project.getId());
        }
        final List<Feature> assembledFeatureList = new ArrayList<>();
        for(final Feature feature : featureList) {
            final Feature assembledFeature = FeatureBuilder.assemble(feature, new BuilderContext(this.createFeatureProvider(env,
                    info,
                    config.isTestConfig(),
                    config.isSkipAddDependencies(),
                    config.getScope(),
                    featureList)));
            assembledFeatureList.add(assembledFeature);
        }
        if ( config.isTestConfig() ) {
            info.testFeatures = featureList;
            info.assembledTestFeatures = assembledFeatureList;
        } else {
            info.features = featureList;
            info.assembledFeatures = assembledFeatureList;
        }

        // and now references
        final List<Feature> featureRefList = new ArrayList<>();
        final File refDir = new File(info.project.getBasedir(), config.getFeatureRefDir());
        if ( refDir.exists() ) {
            final List<File> files = new ArrayList<>();
            scan(files, refDir, "ref");

            for(final File file : files) {
                try {
                    final List<String> features = org.apache.sling.feature.io.IOUtils.parseFeatureRefFile(file);
                    if ( features.isEmpty() ) {
                        env.logger.debug("Empty feature ref file at " + file);
                    } else {
                        for(final String ref : features) {
                            if ( !ref.startsWith("mvn:") ) {
                                throw new RuntimeException("Unsupported feature ref in feature ref file at " + file + " : " + ref);
                            }
                            final ArtifactId id = ArtifactId.fromMvnUrl(ref);
                            final Feature feature = this.createFeatureProvider(env, info, config.isTestConfig(), config.isSkipAddDependencies(), config.getScope(), null).provide(id);
                            if ( feature == null ) {
                                throw new RuntimeException("Unable to resolve feature " + id);
                            }
                            featureRefList.add(feature);
                        }
                    }
                } catch ( final IOException io) {
                    throw new RuntimeException("Unable to read feature " + file.getAbsolutePath(), io);
                }
            }
        }
        final List<Feature> assembledFeatureRefList = new ArrayList<>();
        for(final Feature feature : featureRefList) {
            final Feature assembledFeature = FeatureBuilder.assemble(feature, new BuilderContext(this.createFeatureProvider(env,
                    info,
                    config.isTestConfig(),
                    config.isSkipAddDependencies(),
                    config.getScope(),
                    featureList)));
            assembledFeatureRefList.add(assembledFeature);
        }
        if ( config.isTestConfig() ) {
            info.testFeatures.addAll(featureRefList);
            info.assembledTestFeatures.addAll(assembledFeatureRefList);
        } else {
            info.features.addAll(featureRefList);
            info.assembledFeatures.addAll(assembledFeatureRefList);
        }

        if ( config.isSkipAddDependencies() ) {
            env.logger.debug("Not adding artifacts from features as dependencies");
        } else {
            for(final Feature feature : assembledFeatureList) {
                addDependenciesFromFeature(env, info, feature, config.getScope());
            }
        }
    }

    /**
     * Add all dependencies from the feature
     * @param env The environment
     * @param info The project info
     * @param assembledFeature The assembled feature for finding the artifacts.
     * @param scope The scope which the new dependencies should have
     */
    private void addDependenciesFromFeature(
            final Environment env,
            final ProjectInfo info,
            final Feature assembledFeature,
            final String scope) {
        for(final org.apache.sling.feature.Artifact entry : assembledFeature.getBundles()) {
            final ArtifactId a = entry.getId();
            if ( a.getGroupId().equals(info.project.getGroupId())
                 && a.getArtifactId().equals(info.project.getArtifactId())
                 && a.getVersion().equals(info.project.getVersion()) ) {
                // skip artifact from the same project
                env.logger.debug("- skipping dependency " + a.toMvnId());
                continue;
            }

            env.logger.debug("- adding dependency " + a.toMvnId());
            final Dependency dep = ProjectHelper.toDependency(a, scope);
            info.project.getDependencies().add(dep);
        }
        for(final Extension ext : assembledFeature.getExtensions()) {
            if ( ext.getType() != ExtensionType.ARTIFACTS ) {
                continue;
            }
            for(final org.apache.sling.feature.Artifact art : ext.getArtifacts()) {
                final ArtifactId a = art.getId();
                if ( a.getGroupId().equals(info.project.getGroupId())
                     && a.getArtifactId().equals(info.project.getArtifactId())
                     && a.getVersion().equals(info.project.getVersion()) ) {
                    // skip artifact from the same project
                    env.logger.debug("- skipping dependency " + a.toMvnId());
                    continue;
                }
                env.logger.debug("- adding dependency " + a.toMvnId());
                final Dependency dep = ProjectHelper.toDependency(a, scope);
                info.project.getDependencies().add(dep);
            }
        }
    }

    /**
     * Read the feature for a feature project.
     * The feature is either inlined in the pom or stored in a file in the project.
     *
     * @param logger The logger
     * @param project The current maven project
     * @param config The configuration
     * @return The feature or {@code null}
     */
    protected Feature readProjectFeature(
            final Logger logger,
            final MavenProject project,
            final FeatureProjectConfig config) {
        final File featureFile = new File(project.getBasedir(), config.getFeatureFileName());
        logger.debug("Checking feature file " + config.getFeatureFileName() + " : " + featureFile.exists());
        logger.debug("Inlined feature : " + (config.getInlinedFeature() != null));

        if ( config.getInlinedFeature() != null && featureFile.exists() ) {
            throw new RuntimeException("Only one (feature file or inlined feature) can be specified - but not both");
        }

        final String classifier;
        if ( config.isTestConfig() ) {
            classifier = FeatureConstants.CLASSIFIER_TEST_FEATURE;
        } else if ( FeatureConstants.PACKAGING_FEATURE.equals(project.getPackaging()) ) {
            classifier = null;
        } else {
            classifier = FeatureConstants.CLASSIFIER_FEATURE;
        }
        final ArtifactId id = new ArtifactId(project.getGroupId(),
                project.getArtifactId(),
                project.getVersion(),
                classifier,
                FeatureConstants.PACKAGING_FEATURE);

        final Feature feature;
        if ( config.getInlinedFeature() != null ) {
            logger.debug("Reading inlined model from project " + project.getId());
            try (final Reader reader = new StringReader(config.getInlinedFeature())) {
                feature = FeatureJSONReader.read(reader, id, null, SubstituteVariables.RESOLVE);
            } catch ( final IOException io) {
                throw new RuntimeException("Unable to read inlined feature", io);
            }
        } else {
            if ( !featureFile.exists() ) {
                logger.debug("Feature file " + featureFile + " in project " + project.getId() + " does not exist.");
                return null;
            }
            logger.debug("Reading feature " + featureFile + " in project " + project.getId());
            try (final FileReader reader = new FileReader(featureFile)) {
                feature = FeatureJSONReader.read(reader, id, featureFile.getAbsolutePath(), SubstituteVariables.RESOLVE);
            } catch ( final IOException io) {
                throw new RuntimeException("Unable to read feature " + featureFile, io);
            }
        }
        this.checkFeatureId(id, feature);

        this.setProjectInfo(project, feature);

        // post process and return
        return postProcessReadFeature(feature);
    }

    private void checkFeatureId(final ArtifactId id, final Feature feature) {
        // check feature id
        if ( !id.getGroupId().equals(feature.getId().getGroupId()) ) {
            throw new RuntimeException("Wrong group id for feature. It should be " + id.getGroupId() + " but is " + feature.getId().getGroupId());
        }
        if ( !id.getArtifactId().equals(feature.getId().getArtifactId()) ) {
            throw new RuntimeException("Wrong artifact id for feature. It should be " + id.getArtifactId() + " but is " + feature.getId().getArtifactId());
        }
        if ( !id.getVersion().equals(feature.getId().getVersion()) ) {
            throw new RuntimeException("Wrong version for feature. It should be " + id.getVersion() + " but is " + feature.getId().getVersion());
        }
    }

    /**
     * Hook to post process the local feature
     * @param result The read feature
     * @return The post processed feature
     */
    protected Feature postProcessReadFeature(final Feature result)  {
        return result;
    }

    protected void setProjectInfo(final MavenProject project, final Feature feature) {
        // set title, description, vendor, license
        if ( feature.getTitle() == null ) {
            feature.setTitle(project.getName());
        }
        if ( feature.getDescription() == null ) {
            feature.setDescription(project.getDescription());
        }
        if ( feature.getVendor() == null && project.getOrganization() != null ) {
            feature.setVendor(project.getOrganization().getName());
        }
        if ( feature.getLicense() == null
             && project.getLicenses() != null
             && !project.getLicenses().isEmpty()) {
            final String license = project.getLicenses().stream()
                    .filter(l -> l.getName() != null )
                    .map(l -> l.getName())
                    .collect(Collectors.joining(", "));

            feature.setLicense(license);
        }
    }

    protected FeatureProvider createFeatureProvider(final Environment env,
            final ProjectInfo info,
            final boolean isTest,
            final boolean skipAddDependencies,
            final String dependencyScope,
            final List<Feature> projectFeatures) {
        return new FeatureProvider() {

            @Override
            public Feature provide(final ArtifactId id) {

                final Dependency dep = ProjectHelper.toDependency(id, dependencyScope);
                if ( !skipAddDependencies ) {

                    env.logger.debug("- adding feature dependency " + id.toMvnId());
                    info.project.getDependencies().add(dep);
                }

                // if it's a project from the current reactor build, we can't resolve it right now
                final String key = id.getGroupId() + ":" + id.getArtifactId();
                final ProjectInfo depProjectInfo = env.modelProjects.get(key);
                if ( depProjectInfo != null ) {
                    env.logger.debug("Found reactor " + id.getType() + " dependency to project: " + id);
                    // check if it is a feature project
                    if ( depProjectInfo instanceof FeatureProjectInfo ) {
                        final FeatureProjectInfo depInfo = (FeatureProjectInfo)depProjectInfo;
                        if ( isTest ) {
                            process(env, depInfo, FeatureProjectConfig.getTestConfig(depInfo));
                        } else {
                            process(env, depInfo, FeatureProjectConfig.getMainConfig(depInfo));
                        }
                        if ( isTest && depInfo.assembledTestFeature == null ) {
                            env.logger.error("Unable to get feature " + id.toMvnId() + " : Recursive test feature dependency list including project " + info.project);
                        } else if ( !isTest && depInfo.assembledFeature == null ) {
                            env.logger.error("Unable to get feature " + id.toMvnId() + " : Recursive feature dependency list including project " + info.project);
                        } else {

                            if ( isTest ) {
                                return depInfo.testFeature;
                            } else {
                                return depInfo.feature;
                            }
                        }
                    } else {
                        // we only support a dependency to *this* application project
                        final ApplicationProjectInfo depInfo = (ApplicationProjectInfo)depProjectInfo;
                        if ( depInfo != info) {
                            env.logger.error("Unable to get feature " + id.toMvnId() + " : Feature dependency is to a different application project from " + info.project);
                            return null;
                        }
                        if ( projectFeatures != null ) {
                            for(final Feature f : projectFeatures) {
                                if ( f.getId().equals(id)) {
                                    return f;
                                }
                            }
                        }
                        return null;
                    }
                } else {
                    env.logger.debug("Found external " + id.getType() + " dependency: " + id);

                    // "external" dependency, we can already resolve it
                    final File featureFile = ProjectHelper.getOrResolveArtifact(info.project, env.session, env.artifactHandlerManager, env.resolver, id).getFile();
                    try (final FileReader r = new FileReader(featureFile)) {
                        return FeatureJSONReader.read(r, featureFile.getAbsolutePath(), SubstituteVariables.RESOLVE);
                    } catch ( final IOException ioe) {
                        env.logger.error("Unable to read feature file from " + featureFile, ioe);
                    }
                }

                return null;
            }
        };
    }
}
