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
package org.apache.sling.feature.maven.mojos;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.apache.sling.feature.maven.ApplicationProjectInfo;
import org.apache.sling.feature.maven.Environment;
import org.apache.sling.feature.maven.FeatureConstants;
import org.apache.sling.feature.maven.FeatureProjectInfo;
import org.apache.sling.feature.maven.Preprocessor;
import org.apache.sling.feature.maven.ProjectInfo;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

/**
 * Maven lifecycle participant which adds the artifacts of the model to the dependencies.
 */
@Component(role = AbstractMavenLifecycleParticipant.class)
public class DependencyLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    /**
     * The plugin ID consists of <code>groupId:artifactId</code>, see {@link Plugin#constructKey(String, String)}
     */
    private static final String PLUGIN_ID = "org.apache.sling:osgifeature-maven-plugin";

    @Requirement
    private Logger logger;

    @Requirement
    private ArtifactHandlerManager artifactHandlerManager;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     */
    @Requirement
    private ArtifactResolver resolver;

    @Override
    public void afterProjectsRead(final MavenSession session) throws MavenExecutionException {
        final Environment env = new Environment();
        env.artifactHandlerManager = artifactHandlerManager;
        env.resolver = resolver;
        env.logger = logger;
        env.session = session;

        logger.debug("Searching for project using plugin '" + PLUGIN_ID + "'...");

        for (final MavenProject project : session.getProjects()) {
            // consider all projects where this plugin is configured
            Plugin plugin = project.getPlugin(PLUGIN_ID);
            if (plugin != null) {
                logger.debug("Found project " + project.getId() + " using " + PLUGIN_ID);
                final ProjectInfo info;
                if ( FeatureConstants.PACKAGING_APPLICATION.equals(project.getPackaging()) ) {
                    info = new ApplicationProjectInfo();
                } else {
                    info = new FeatureProjectInfo();
                }
                info.plugin = plugin;
                info.project = project;
                env.modelProjects.put(project.getGroupId() + ":" + project.getArtifactId(), info);
            }
        }

        new Preprocessor().process(env);
    }
}
