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

import org.apache.maven.artifact.Artifact;

public class FeatureProjectConfig {

    public static final String CFG_SKIP_ADD_FEATURE_DEPENDENCIES = "skipAddFeatureDependencies";

    public static final String CFG_SKIP_ADD_TEST_FEATURE_DEPENDENCIES = "skipAddTestFeatureDependencies";

    public static final String CFG_FEATURE_FILE = "featureFile";

    public static final String CFG_TEST_FEATURE_FILE = "testFeatureFile";

    public static final String CFG_FEATURE_INLINED = "feature";

    public static final String CFG_TEST_FEATURE_INLINED = "testFeature";

    public static final String CFG_SKIP_ADD_JAR_TO_FEATURE = "skipAddJarToFeature";

    public static final String CFG_SKIP_ADD_JAR_TO_TEST_FEATURE = "skipAddJarToTestFeature";

    public static final String CFG_JAR_START_LEVEL = "jarStartLevel";

    public static final String DEFAULT_FEATURE_FILE = "src/main/osgi/feature.json";

    public static final String DEFAULT_TEST_FEATURE_FILE = "src/test/osgi/feature.json";

    public static final String DEFAULT_START_LEVEL = "20";

    private final String inlinedFeature;

    private final String featureFileName;

    private final boolean skipAddDep;

    private final String name;

    private final String scope;

    private final boolean isTest;

    private final String jarStartLevel;

    private final boolean skipAddJar;

    public static FeatureProjectConfig getMainConfig(final FeatureProjectInfo info) {
        return new FeatureProjectConfig(info, false);
    }

    public static FeatureProjectConfig getTestConfig(final FeatureProjectInfo info) {
        return new FeatureProjectConfig(info, true);
    }

    private FeatureProjectConfig(final FeatureProjectInfo info, final boolean test) {
        this.isTest = test;
        final String inlineCfgName;
        final String fileCfgName;
        final String defaultFile;
        final String skipAddDepCfgName;
        final String defaultSkipValue;
        if ( test ) {
            inlineCfgName = CFG_TEST_FEATURE_INLINED;
            fileCfgName = CFG_TEST_FEATURE_FILE;
            defaultFile = DEFAULT_TEST_FEATURE_FILE;
            this.scope = Artifact.SCOPE_TEST;
            skipAddDepCfgName = CFG_SKIP_ADD_TEST_FEATURE_DEPENDENCIES;
            defaultSkipValue = "true";
            this.name = "test feature";
            this.skipAddJar = "true".equals(ProjectHelper.getConfigValue(info.plugin, CFG_SKIP_ADD_JAR_TO_TEST_FEATURE, "true"));
        } else {
            inlineCfgName = CFG_FEATURE_INLINED;
            fileCfgName = CFG_TEST_FEATURE_FILE;
            defaultFile = DEFAULT_FEATURE_FILE;
            this.scope = Artifact.SCOPE_PROVIDED;
            skipAddDepCfgName = CFG_SKIP_ADD_FEATURE_DEPENDENCIES;
            defaultSkipValue = "false";
            this.name = "feature";
            this.skipAddJar = "true".equals(ProjectHelper.getConfigValue(info.plugin, CFG_SKIP_ADD_JAR_TO_FEATURE, "true"));
        }
        this.inlinedFeature = ProjectHelper.getConfigValue(info.plugin, inlineCfgName, null);
        this.featureFileName = ProjectHelper.getConfigValue(info.plugin, fileCfgName, defaultFile);
        final String skipCfg = ProjectHelper.getConfigValue(info.plugin, skipAddDepCfgName, defaultSkipValue);
        this.skipAddDep = "true".equals(skipCfg.toLowerCase());
        this.jarStartLevel = ProjectHelper.getConfigValue(info.plugin, CFG_JAR_START_LEVEL, DEFAULT_START_LEVEL);
    }

    public String getName() {
        return this.name;
    }

    public String getInlinedFeature() {
        return this.inlinedFeature;
    }

    public String getFeatureFileName() {
        return this.featureFileName;
    }

    public boolean isSkipAddDependencies() {
        return this.skipAddDep;
    }

    public String getScope() {
        return this.scope;
    }

    public boolean isTestConfig() {
        return this.isTest;
    }

    public String getJarStartLevel() {
        return this.jarStartLevel;
    }

    public boolean isSkipAddJarToFeature() {
        return this.skipAddJar;
    }


}

