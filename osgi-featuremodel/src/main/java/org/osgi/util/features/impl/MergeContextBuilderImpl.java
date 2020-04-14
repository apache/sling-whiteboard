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
package org.osgi.util.features.impl;

import org.osgi.util.features.FeatureBundle;
import org.osgi.util.features.FeatureConfiguration;
import org.osgi.util.features.ConflictResolver;
import org.osgi.util.features.FeatureExtension;
import org.osgi.util.features.Feature;
import org.osgi.util.features.MergeContext;
import org.osgi.util.features.MergeContextBuilder;

import java.util.List;

class MergeContextBuilderImpl implements MergeContextBuilder {
    private ConflictResolver<FeatureBundle, List<FeatureBundle>> bundleHandler;
    private ConflictResolver<FeatureConfiguration, FeatureConfiguration> configHandler;
    private ConflictResolver<FeatureExtension, FeatureExtension> extensionHandler;

    @Override
    public MergeContextBuilder bundleConflictHandler(ConflictResolver<FeatureBundle, List<FeatureBundle>> bh) {
        bundleHandler = bh;
        return this;
    }

    @Override
    public MergeContextBuilder configConflictHandler(ConflictResolver<FeatureConfiguration, FeatureConfiguration> ch) {
        configHandler = ch;
        return this;
    }

    @Override
    public MergeContextBuilder extensionConflictHandler(ConflictResolver<FeatureExtension, FeatureExtension> eh) {
        extensionHandler = eh;
        return this;
    }

    @Override
    public MergeContext build() {
        return new MergeContextImpl(bundleHandler, configHandler, extensionHandler);
    }

    private static class MergeContextImpl implements MergeContext {
        private final ConflictResolver<FeatureBundle, List<FeatureBundle>> bundleHandler;
        private final ConflictResolver<FeatureConfiguration, FeatureConfiguration> configHandler;
        private final ConflictResolver<FeatureExtension, FeatureExtension> extensionHandler;


        MergeContextImpl(ConflictResolver<FeatureBundle, List<FeatureBundle>> bundleHandler,
                ConflictResolver<FeatureConfiguration, FeatureConfiguration> configHandler,
                ConflictResolver<FeatureExtension, FeatureExtension> extensionHandler) {
            this.bundleHandler = bundleHandler;
            this.configHandler = configHandler;
            this.extensionHandler = extensionHandler;
        }

        @Override
        public List<FeatureBundle> handleBundleConflict(Feature f1, FeatureBundle b1, Feature f2, FeatureBundle b2) {
            return bundleHandler.resolve(f1, b1, f2, b2);
        }

        @Override
        public FeatureConfiguration handleConfigurationConflict(Feature f1, FeatureConfiguration c1, Feature f2, FeatureConfiguration c2) {
            return configHandler.resolve(f1, c1, f2, c2);
        }

        @Override
        public FeatureExtension handleExtensionConflict(Feature f1, FeatureExtension e1, Feature f2, FeatureExtension e2) {
            return extensionHandler.resolve(f1, e1, f2, e2);
        }
    }
}
