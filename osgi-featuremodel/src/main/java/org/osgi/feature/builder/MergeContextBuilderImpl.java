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
package org.osgi.feature.builder;

import org.osgi.feature.Bundle;
import org.osgi.feature.Configuration;
import org.osgi.feature.ConflictResolver;
import org.osgi.feature.Extension;
import org.osgi.feature.Feature;
import org.osgi.feature.MergeContext;

import java.util.List;

public class MergeContextBuilderImpl {
    private ConflictResolver<Bundle, List<Bundle>> bundleHandler;
    private ConflictResolver<Configuration, Configuration> configHandler;
    private ConflictResolver<Extension, Extension> extensionHandler;

    public MergeContextBuilderImpl bundleConflictHandler(ConflictResolver<Bundle, List<Bundle>> bh) {
        bundleHandler = bh;
        return this;
    }

    public MergeContextBuilderImpl configConflictHandler(ConflictResolver<Configuration, Configuration> ch) {
        configHandler = ch;
        return this;
    }

    public MergeContextBuilderImpl extensionConflictHandler(ConflictResolver<Extension, Extension> eh) {
        extensionHandler = eh;
        return this;
    }

    public MergeContext build() {
        return new MergeContextImpl(bundleHandler, configHandler, extensionHandler);
    }

    private static class MergeContextImpl implements MergeContext {
        private final ConflictResolver<Bundle, List<Bundle>> bundleHandler;
        private final ConflictResolver<Configuration, Configuration> configHandler;
        private final ConflictResolver<Extension, Extension> extensionHandler;


        public MergeContextImpl(ConflictResolver<Bundle, List<Bundle>> bundleHandler,
                ConflictResolver<Configuration, Configuration> configHandler,
                ConflictResolver<Extension, Extension> extensionHandler) {
            this.bundleHandler = bundleHandler;
            this.configHandler = configHandler;
            this.extensionHandler = extensionHandler;
        }

        @Override
        public List<Bundle> handleBundleConflict(Feature f1, Bundle b1, Feature f2, Bundle b2) {
            return bundleHandler.resolve(f1, b1, f2, b2);
        }

        @Override
        public Configuration handleConfigurationConflict(Feature f1, Configuration c1, Feature f2, Configuration c2) {
            return configHandler.resolve(f1, c1, f2, c2);
        }

        @Override
        public Extension handleExtensionConflict(Feature f1, Extension e1, Feature f2, Extension e2) {
            return extensionHandler.resolve(f1, e1, f2, e2);
        }
    }
}
