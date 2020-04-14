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
package org.osgi.util.features;

/**
 * The Builder Factory can be used to obtain builders for the various entities.
 */
public interface BuilderFactory {
    /**
     * Obtain a new builder for Bundle objects.
     * @param id The artifact ID for the bundle object being built.
     * @return The builder.
     */
    BundleBuilder newBundleBuilder(ID id);

    /**
     * Obtain a new builder for Configuration objects.
     * @param pid The persistent ID for the Configuration being built.
     * @return The builder.
     */
    ConfigurationBuilder newConfigurationBuilder(String pid);

    /**
     * Obtain a new builder for Factory Configuration objects.
     * @param factoryPid The factory persistent ID for the Configuration being built.
     * @param name The name of the configuration being built. The PID for the configuration
     * will be the factoryPid + '~' + name
     * @return The builder.
     */
    ConfigurationBuilder newConfigurationBuilder(String factoryPid, String name);

    /**
     * Obtain a new builder for Feature objects.
     * @param id The artifact ID for the feature object being built.
     * @return The builder.
     */
    FeatureBuilder newFeatureBuilder(ID id);

    /**
     * Obtain a new builder for Feature objects.
     * @param name The extension name.
     * @param type The type of extension: JSON, Text or Artifacts.
     * @param kind The kind of extension: Mandatory, Optional or Transient.
     * @return The builder.
     */
    ExtensionBuilder newExtensionBuilder(String name, Extension.Type type, Extension.Kind kind);

    /**
     * Obtain a new builder for MergeContext objects.
     * @return The builder.
     */
    MergeContextBuilder newMergeContextBuilder();
}
