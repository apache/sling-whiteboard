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
package org.osgi.feature.impl;

import org.osgi.feature.ArtifactID;
import org.osgi.feature.BuilderFactory;
import org.osgi.feature.BundleBuilder;
import org.osgi.feature.Extension.Kind;
import org.osgi.feature.Extension.Type;
import org.osgi.feature.ExtensionBuilder;
import org.osgi.feature.FeatureBuilder;
import org.osgi.feature.MergeContextBuilder;

class BuilderFactoryImpl implements BuilderFactory {
    @Override
    public BundleBuilder newBundleBuilder(ArtifactID id) {
        return new BundleBuilderImpl(id);
    }

    @Override
    public FeatureBuilder newFeatureBuilder(ArtifactID id) {
        return new FeatureBuilderImpl(id);
    }

    @Override
    public ExtensionBuilder newExtensionBuilder(String name, Type type, Kind kind) {
        return new ExtensionBuilderImpl(name, type, kind);
    }

    @Override
    public MergeContextBuilder newMergeContextBuilder() {
        return new MergeContextBuilderImpl();
    }
}
