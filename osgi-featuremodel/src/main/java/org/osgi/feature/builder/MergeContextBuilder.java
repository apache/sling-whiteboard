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
import org.osgi.feature.MergeContext;

import java.util.List;
import java.util.function.BiFunction;

public class MergeContextBuilder {
    private BiFunction<Bundle, Bundle, List<Bundle>> bundleHandler;
    private BiFunction<Configuration, Configuration, Configuration> configHandler;

    public MergeContextBuilder bundleConflictHandler(BiFunction<Bundle, Bundle, List<Bundle>> bh) {
        bundleHandler = bh;
        return this;
    }

    public MergeContextBuilder configConflictHandler(BiFunction<Configuration, Configuration, Configuration> ch) {
        configHandler = ch;
        return this;
    }

    public MergeContext build() {
        return new MergeContextImpl(bundleHandler, configHandler);
    }

    private static class MergeContextImpl implements MergeContext {
        private BiFunction<Bundle, Bundle, List<Bundle>> bundleHandler;
        private BiFunction<Configuration, Configuration, Configuration> configHandler;

        private MergeContextImpl(BiFunction<Bundle, Bundle, List<Bundle>> bundleHandler,
                BiFunction<Configuration, Configuration, Configuration> configHandler) {
            this.bundleHandler = bundleHandler;
            this.configHandler = configHandler;
        }

        @Override
        public List<Bundle> resolveBundleConflict(Bundle b1, Bundle b2) {
            return bundleHandler.apply(b1, b2);
        }

        @Override
        public Configuration resolveConfigurationConflict(Configuration c1, Configuration c2) {
            return configHandler.apply(c1, c2);
        }
    }
}
