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
package org.apache.sling.feature.process;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder context holds services used by {@link ApplicationBuilder}
 * and {@link FeatureBuilder}.
 */
public class BuilderContext {

    private final FeatureProvider provider;

    private final List<FeatureExtensionHandler> featureExtensionHandlers = new ArrayList<>();

    /**
     * Assemble the full feature by processing all includes.
     *
     * @param feature The feature to start
     * @param provider A provider providing the included features
     * @param extensionMergers Optional feature mergers
     * @return The assembled feature.
     * @throws IllegalArgumentException If feature or provider is {@code null}
     * @throws IllegalStateException If an included feature can't be provided or merged.
     */
    public BuilderContext(final FeatureProvider provider) {
        if ( provider == null ) {
            throw new IllegalArgumentException("Provider must not be null");
        }
        this.provider = provider;
    }

    FeatureProvider getFeatureProvider() {
        return this.provider;
    }

    List<FeatureExtensionHandler> getFeatureExtensionHandlers() {
        return this.featureExtensionHandlers;
    }

    public BuilderContext add(final FeatureExtensionHandler handler) {
        featureExtensionHandlers.add(handler);
        return this;
    }

    BuilderContext clone(final FeatureProvider featureProvider) {
        final BuilderContext ctx = new BuilderContext(featureProvider);
        ctx.featureExtensionHandlers.addAll(featureExtensionHandlers);
        return ctx;
    }
}
