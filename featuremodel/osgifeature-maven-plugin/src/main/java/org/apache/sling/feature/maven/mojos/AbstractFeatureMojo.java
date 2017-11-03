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
package org.apache.sling.feature.maven.mojos;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.sling.feature.maven.FeatureProjectConfig;

import java.io.File;

/**
 * Base class for all mojos.
 */
public abstract class AbstractFeatureMojo extends AbstractMojo {

    /**
     * The feature file..
     * This parameter is evaluated in the {@link DependencyLifecycleParticipant}.
     */
    @Parameter(name = FeatureProjectConfig.CFG_FEATURE_FILE,
            defaultValue="${basedir}/" + FeatureProjectConfig.DEFAULT_FEATURE_FILE)
    private File featureFile;

    /**
     * The test feature file..
     * This parameter is evaluated in the {@link DependencyLifecycleParticipant}.
     */
    @Parameter(name = FeatureProjectConfig.CFG_TEST_FEATURE_FILE,
            defaultValue="${basedir}/" + FeatureProjectConfig.DEFAULT_TEST_FEATURE_FILE)
    private File testFeatureFile;

    /**
     * Inlined model.
     * This parameter is evaluated in the {@link DependencyLifecycleParticipant}.
     */
    @Parameter(name = FeatureProjectConfig.CFG_FEATURE_INLINED)
    private String feature;

    /**
     * Inlined test model.
     * This parameter is evaluated in the {@link DependencyLifecycleParticipant}.
     */
    @Parameter(name = FeatureProjectConfig.CFG_TEST_FEATURE_INLINED)
    private String testFeature;

    /**
     * If set to {@code true} the artifacts from the feature are not as dependencies to the project.
     * This parameter is evaluated in the {@link DependencyLifecycleParticipant}.
     */
    @Parameter(name=FeatureProjectConfig.CFG_SKIP_ADD_FEATURE_DEPENDENCIES,
            defaultValue="false")
    private boolean skipAddFeatureDependencies;

    /**
     * If set to {@code true} the artifacts from the test feature are not as dependencies to the project.
     * This parameter is evaluated in the {@link DependencyLifecycleParticipant}.
     */
    @Parameter(name=FeatureProjectConfig.CFG_SKIP_ADD_TEST_FEATURE_DEPENDENCIES,
            defaultValue="true")
    private boolean skipAddTestFeatureDependencies;

    /**
     * If set to {@code true} the main jar artifact is not added to the feature.
     * This parameter is evaluated in the {@link DependencyLifecycleParticipant}.
     */
    @Parameter(name=FeatureProjectConfig.CFG_SKIP_ADD_JAR_TO_FEATURE,
            defaultValue="false")
    private boolean skipAddJarToFeature;

    /**
     * If set to {@code true} the main jar artifact is not added to the test feature.
     * This parameter is evaluated in the {@link DependencyLifecycleParticipant}.
     */
    @Parameter(name=FeatureProjectConfig.CFG_SKIP_ADD_JAR_TO_TEST_FEATURE,
            defaultValue="false")
    private boolean skipAddJarToTestFeature;

    /**
     * The start level for the attached jar/bundle.
     * This parameter is evaluated in the {@link DependencyLifecycleParticipant}.
     */
    @Parameter(name=FeatureProjectConfig.CFG_JAR_START_LEVEL,
            defaultValue=FeatureProjectConfig.DEFAULT_START_LEVEL)
    private int jarStartLevel;

    @Parameter(property = "project", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(property = "session", readonly = true, required = true)
    protected MavenSession mavenSession;

    @Component
    protected MavenProjectHelper projectHelper;

    protected File getTmpDir() {
        return new File(this.project.getBuild().getDirectory(), "osgifeature-tmp");
    }
}
