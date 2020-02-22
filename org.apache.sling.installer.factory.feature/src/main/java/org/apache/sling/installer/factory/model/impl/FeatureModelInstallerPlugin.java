/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.installer.factory.model.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.sling.feature.Feature;
import org.apache.sling.feature.io.archive.ArchiveReader;
import org.apache.sling.feature.io.artifacts.ArtifactManager;
import org.apache.sling.feature.io.artifacts.ArtifactManagerConfig;
import org.apache.sling.feature.io.json.FeatureJSONReader;
import org.apache.sling.feature.io.json.FeatureJSONWriter;
import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallTaskFactory;
import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.ResourceTransformer;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.apache.sling.installer.api.tasks.TransformationResult;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.repoinit.JcrRepoInitOpsProcessor;
import org.apache.sling.repoinit.parser.RepoInitParser;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This task factory processes model resources detected by
 * the {@link FeatureModelTransformer}.
 */
@Component(service = { InstallTaskFactory.class, ResourceTransformer.class })
@Designate(ocd = FeatureModelInstallerPlugin.Config.class)
public class FeatureModelInstallerPlugin implements InstallTaskFactory, ResourceTransformer {

    @ObjectClassDefinition(name = "Apache Sling Feature Model Installer",
            description = "This component provides support for feature models to the OSGi installer")
    public @interface Config {

        @AttributeDefinition(name = "Use Apache Maven",
                description = "If enabled, missing artifacts from a feature are tried by invoking the mvn command")
        boolean useMvn() default true;

        @AttributeDefinition(name = "Repository URLs", description = "Additional repository URLs to fetch artifacts")
        String[] repositories();

        @AttributeDefinition(name = "Classifier Patterns", description = "Patterns for selecting the features to handle based on the classifier. Without a configuration all features are handled.")
        String[] classifierPatterns();
    }

    public static final String FILE_EXTENSION = ".json";

    public static final String TYPE_FEATURE_MODEL = "featuremodel";

    public static final String ATTR_MODEL = "feature";

    public static final String ATTR_BASE_PATH = "path";

    public static final String ATTR_ID = "featureId";

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private SlingRepository repository;

    @Reference
    private JcrRepoInitOpsProcessor repoInitProcessor;

    @Reference
    private RepoInitParser repoInitParser;

    private final BundleContext bundleContext;

    private final ArtifactManager artifactManager;

    private final List<String> classifierPatterns = new ArrayList<>();

    @Activate
    public FeatureModelInstallerPlugin(final BundleContext ctx, final Config config) throws IOException {
        this.bundleContext = ctx;
        final ArtifactManagerConfig amCfg = new ArtifactManagerConfig();
        amCfg.setUseMvn(config.useMvn());
        if (config.repositories() != null && config.repositories().length > 0) {
            final List<String> repos = new ArrayList<>(Arrays.asList(amCfg.getRepositoryUrls()));
            for (final String r : config.repositories()) {
                if (!r.trim().isEmpty()) {
                    repos.add(r);
                }
            }
            amCfg.setRepositoryUrls(repos.toArray(new String[repos.size()]));
        }
        this.artifactManager = ArtifactManager.getArtifactManager(amCfg);
        if (config.classifierPatterns() != null) {
            for (final String text : config.classifierPatterns()) {
                if (text != null && !text.trim().isEmpty()) {
                    classifierPatterns.add(text.trim());
                }
            }
        }
    }

    @Override
    public TransformationResult[] transform(final RegisteredResource resource) {
        final List<Feature> features = new ArrayList<>();
        File baseDir = null;
        if (resource.getType().equals(InstallableResource.TYPE_FILE) && resource.getURL().endsWith(FILE_EXTENSION)) {
            try (final Reader reader = new InputStreamReader(resource.getInputStream(), "UTF-8")) {
                features.add(FeatureJSONReader.read(reader, resource.getURL()));
            } catch (final IOException ioe) {
                logger.info("Unable to read feature model from " + resource.getURL(), ioe);
            }
        }
        if (resource.getType().equals(InstallableResource.TYPE_FILE) && resource.getURL().endsWith(".zip")) {
            baseDir = this.bundleContext.getDataFile("");
            try (final InputStream is = resource.getInputStream()) {
                features.addAll(ArchiveReader.read(is, null));
            } catch (final IOException ioe) {
                logger.info("Unable to read feature model from " + resource.getURL(), ioe);
            }
        }
        if (!features.isEmpty()) {
            boolean error = false;
            final List<TransformationResult> result = new ArrayList<>();
            for (final Feature feature : features) {
                boolean select = this.classifierPatterns.isEmpty();
                if (!select) {
                    for (final String pattern : this.classifierPatterns) {
                        if (":".equals(pattern)) {
                            select = feature.getId().getClassifier() == null;
                        } else if (feature.getId().getClassifier() != null) {
                            select = Pattern.compile(pattern).matcher(feature.getId().getClassifier()).matches();
                        }

                        if (select) {
                            break;
                        }
                    }
                }

                if (!select) {
                    continue;
                }

                String featureJson = null;
                try (final StringWriter sw = new StringWriter()) {
                    FeatureJSONWriter.write(sw, feature);
                    featureJson = sw.toString();
                } catch (final IOException ioe) {
                    logger.info("Unable to read feature model from " + resource.getURL(), ioe);
                }

                if (featureJson != null) {
                    final TransformationResult tr = new TransformationResult();
                    tr.setResourceType(TYPE_FEATURE_MODEL);
                    tr.setId(feature.getId().toMvnId());
                    tr.setVersion(feature.getId().getOSGiVersion());

                    final Map<String, Object> attributes = new HashMap<>();
                    attributes.put(ATTR_MODEL, featureJson);
                    attributes.put(ATTR_ID, feature.getId().toMvnId());
                    if (baseDir != null) {
                        final File dir = new File(baseDir, feature.getId().toMvnName());
                        attributes.put(ATTR_BASE_PATH, dir.getAbsolutePath());
                    }
                    tr.setAttributes(attributes);

                    result.add(tr);
                } else {
                    error = true;
                    break;
                }
            }
            if (!error) {
                return result.toArray(new TransformationResult[result.size()]);
            }
        }
        return null;
    }

    @Override
    public InstallTask createTask(final TaskResourceGroup group) {
        final TaskResource rsrc = group.getActiveResource();
        if (!TYPE_FEATURE_MODEL.equals(rsrc.getType())) {
            return null;
        }
        if (rsrc.getState() == ResourceState.UNINSTALL ) {
            return new UninstallFeatureModelTask(group, bundleContext);
        }
        return new InstallFeatureModelTask(group,
                this.repository,
                this.repoInitProcessor,
                this.repoInitParser,
                this.bundleContext, this.artifactManager);
    }
}
