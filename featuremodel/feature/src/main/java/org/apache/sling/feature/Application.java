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
package org.apache.sling.feature;

import java.util.ArrayList;
import java.util.List;

/**
 * An application consists of
 * <ul>
 *   <li>Framework
 *   <li>Bundles
 *   <li>Configurations
 *   <li>Framework properties
 *   <li>Extensions
 *   <li>Feature ids (of the features making up this application)
 * </ul>
 */
public class Application {

    /** Container for bundles. */
    private final Bundles bundles = new Bundles();

    /** List of configurations. */
    private final Configurations configurations = new Configurations();

    /** Map of framework properties. */
    private final KeyValueMap frameworkProperties = new KeyValueMap();

    /** List of extensions. */
    private final Extensions extensions = new Extensions();

    /** List of features. */
    private final List<ArtifactId> features = new ArrayList<>();

    /** The framework id. */
    private ArtifactId framework;

    /**
     * Get the bundles
     * @return The bundles object.
     */
    public Bundles getBundles() {
        return this.bundles;
    }

    /**
     * Get the configurations
     * The list is modifiable.
     * @return The list of configurations
     */
    public Configurations getConfigurations() {
        return this.configurations;
    }

    /**
     * Get the framework properties
     * The map is modifiable
     * @return The map of properties
     */
    public KeyValueMap getFrameworkProperties() {
        return this.frameworkProperties;
    }

    /**
     * Get the list of extensions
     * The list is modifiable
     * @return The list of extension
     */
    public Extensions getExtensions() {
        return this.extensions;
    }

    /**
     * Get the list of used features to build this application
     * @return The list of features
     */
    public List<ArtifactId> getFeatureIds() {
        return this.features;
    }

    /**
     * Get the framework id
     * @return The framework id or {@code null}
     */
    public ArtifactId getFramework() {
        return framework;
    }

    /**
     * Set the framework id
     * @param framework The framework id
     */
    public void setFramework(final ArtifactId framework) {
        this.framework = framework;
    }

    @Override
    public String toString() {
        return "Application [features=" + this.features
                + "]";
    }
}
