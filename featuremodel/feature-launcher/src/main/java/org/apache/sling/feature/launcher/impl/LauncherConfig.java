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
package org.apache.sling.feature.launcher.impl;

import org.apache.sling.feature.support.artifact.ArtifactManagerConfig;
import org.apache.sling.feature.support.artifact.spi.ArtifactProviderContext;

import java.io.File;
import java.io.IOException;

/**
 * This class holds the configuration of the launcher.
 */
public class LauncherConfig
    extends ArtifactManagerConfig
    implements ArtifactProviderContext {

    public enum StartupMode {
        INSTALLER,
        PURE
    };

    private static final String HOME = "launcher";

    private static final String CACHE_DIR = "cache";

    /** The feature files or directories. */
    private volatile String[] featureFiles;

    /** The application file. */
    private volatile String appFile;

    private volatile StartupMode startupMode = StartupMode.PURE;

    private final Installation installation = new Installation();

    /**
     * Create a new configuration object.
     * Set the default values
     */
    public LauncherConfig() {
        this.setCacheDirectory(new File(getHomeDirectory(), CACHE_DIR));
    }

    public void setApplicationFile(final String value) {
        appFile = value;
    }

    public String getApplicationFile() {
        return this.appFile;
    }

    /**
     * Set the list of feature files or directories.
     * @param value The array with the feature file names.
     */
    public void setFeatureFiles(final String[] value) {
        this.featureFiles = value;
        if ( value != null && value.length == 0 ) {
            this.featureFiles = null;
        }
    }

    /**
     * Get the list of feature files.
     * @return The array of names.
     * @throws IOException
     */
    public String[] getFeatureFiles() {
        return this.featureFiles;
    }


    /**
     * Get the home directory.
     * @return The home directory.
     */
    public File getHomeDirectory() {
        return new File(HOME);
    }

    /**
     * Get the startup mode.
     *
     * @return The current startup mode.
     */
    public StartupMode getStartupMode() {
        return this.startupMode;
    }

    /**
     * Sets the startup mode to {@link StartupMode#INSTALLER}.
     */
    public void setUseInstaller() {
        this.startupMode = StartupMode.INSTALLER;
    }

    public Installation getInstallation() {
        return this.installation;
    }

    /**
     * Clear all in-memory objects
     */
    public void clear() {
        this.installation.clear();
    }
}
