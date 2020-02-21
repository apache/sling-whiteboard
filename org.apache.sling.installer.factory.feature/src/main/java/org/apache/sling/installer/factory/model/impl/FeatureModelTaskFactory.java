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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.sling.feature.io.artifacts.ArtifactManager;
import org.apache.sling.feature.io.artifacts.ArtifactManagerConfig;
import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallTaskFactory;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
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

/**
 * This task factory processes model resources detected by
 * the {@link FeatureModelTransformer}.
 */
@Component(service = InstallTaskFactory.class)
@Designate(ocd = FeatureModelTaskFactory.Config.class)
public class FeatureModelTaskFactory implements InstallTaskFactory {

    @ObjectClassDefinition(name = "Apache Sling Feature Model Installer",
            description = "This component provides support for feature models to the OSGi installer")
    public @interface Config {

        @AttributeDefinition(name = "Use Apache Maven",
                description = "If enabled, missing artifacts from a feature are tried by invoking the mvn command")
        boolean useMvn() default true;

        @AttributeDefinition(name = "Repository URLs", description = "Additional repository URLs to fetch artifacts")
        String[] repositories();
    }

    @Reference
    private SlingRepository repository;

    @Reference
    private JcrRepoInitOpsProcessor repoInitProcessor;

    @Reference
    private RepoInitParser repoInitParser;

    private final BundleContext bundleContext;

    private final ArtifactManager artifactManager;

    @Activate
    public FeatureModelTaskFactory(final BundleContext ctx, final Config config) throws IOException {
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
    }

    @Override
    public InstallTask createTask(final TaskResourceGroup group) {
        final TaskResource rsrc = group.getActiveResource();
        if (!FeatureModelTransformer.TYPE_FEATURE_MODEL.equals(rsrc.getType())) {
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
