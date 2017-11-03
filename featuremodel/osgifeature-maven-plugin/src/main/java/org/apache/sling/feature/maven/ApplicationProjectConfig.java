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

public class ApplicationProjectConfig {

    public static final String CFG_FEATURES = "features";

    public static final String CFG_TEST_FEATURES = "testFeatures";

    public static final String CFG_FEATURE_REFS = "featureRefs";

    public static final String CFG_TEST_FEATURE_REFS = "testFeatureRefs";

    public static final String DEFAULT_FEATURE_DIR = "src/main/osgi/features";

    public static final String DEFAULT_TEST_FEATURE_DIR = "src/test/osgi/features";

    public static final String DEFAULT_REF_DIR = "src/main/osgi/feature-refs";

    public static final String DEFAULT_TEST_REF_DIR = "src/test/osgi/feature-refs";

    private final String featuresDirName;

    private final String featureRefsDirName;

    private final boolean skipAddDep;

    private final String name;

    private final String scope;

    private final boolean isTest;

    public static ApplicationProjectConfig getMainConfig(final ApplicationProjectInfo info) {
        return new ApplicationProjectConfig(info, false);
    }

    public static ApplicationProjectConfig getTestConfig(final ApplicationProjectInfo info) {
        return new ApplicationProjectConfig(info, true);
    }

    private ApplicationProjectConfig(final ApplicationProjectInfo info, final boolean test) {
        this.isTest = test;
        final String featuresDirCfgName;
        final String featureRefsDirCfgName;
        final String defaultDir;
        final String defaultRefDir;
        final String skipAddDepCfgName;
        final String defaultSkipValue;
        if ( test ) {
            featuresDirCfgName = CFG_TEST_FEATURES;
            featureRefsDirCfgName = CFG_TEST_FEATURE_REFS;
            defaultDir = DEFAULT_TEST_FEATURE_DIR;
            defaultRefDir = DEFAULT_TEST_REF_DIR;
            skipAddDepCfgName = FeatureProjectConfig.CFG_SKIP_ADD_TEST_FEATURE_DEPENDENCIES;
            defaultSkipValue = "true";
            this.scope = Artifact.SCOPE_TEST;
            this.name = "test features";
        } else {
            featuresDirCfgName = CFG_FEATURES;
            featureRefsDirCfgName = CFG_FEATURE_REFS;
            defaultDir = DEFAULT_FEATURE_DIR;
            defaultRefDir = DEFAULT_REF_DIR;
            skipAddDepCfgName = FeatureProjectConfig.CFG_SKIP_ADD_FEATURE_DEPENDENCIES;
            defaultSkipValue = "false";
            this.scope = Artifact.SCOPE_PROVIDED;
            this.name = "features";
        }
        this.featuresDirName = ProjectHelper.getConfigValue(info.plugin, featuresDirCfgName, defaultRefDir);
        this.featureRefsDirName = ProjectHelper.getConfigValue(info.plugin, featureRefsDirCfgName, defaultDir);
        final String skipCfg = ProjectHelper.getConfigValue(info.plugin, skipAddDepCfgName, defaultSkipValue);
        this.skipAddDep = "true".equals(skipCfg.toLowerCase());
    }

    public String getName() {
        return this.name;
    }

    public String getFeatureDir() {
        return this.featuresDirName;
    }

    public String getFeatureRefDir() {
        return this.featuresDirName;
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
}

