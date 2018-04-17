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
package org.apache.sling.feature.support.artifact;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.sling.feature.support.artifact.spi.ArtifactProviderContext;

/**
 * This class holds the configuration of artifact manager.
 */
public class ArtifactManagerConfig implements ArtifactProviderContext {

    /** The repository urls. */
    private volatile String[] repositoryUrls;

    /** The cache directory. */
    private volatile File cacheDirectory;

    private volatile long cachedArtifacts;

    private volatile long downloadedArtifacts;

    private volatile long localArtifacts;

    /**
     * Create a new configuration object.
     * Set the default values
     */
    public ArtifactManagerConfig() {
        // set defaults
        this.repositoryUrls = new String[] {
                "file://" + System.getProperty("user.home") + "/.m2/repository",
                "https://repo.maven.apache.org/maven2",
                "https://repository.apache.org/content/groups/snapshots"
                };
        try {
            this.cacheDirectory = Files.createTempDirectory("slingfeature").toFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Set the repository urls
     * @param urls The repository urls
     */
    public void setRepositoryUrls(final String[] urls) {
        if ( urls == null || urls.length == 0 ) {
            this.repositoryUrls = null;
        } else {
            this.repositoryUrls = new String[urls.length];
            System.arraycopy(urls, 0, this.repositoryUrls, 0, urls.length);
            for(int i=0; i<this.repositoryUrls.length; i++) {
                if ( this.repositoryUrls[i].endsWith("/") ) {
                    this.repositoryUrls[i] = this.repositoryUrls[i].substring(0, this.repositoryUrls[i].length() - 1);
                }
            }
        }
    }

    /**
     * Get the repository urls.
     * A repository url does not end with a slash.
     * @return The repository urls.
     */
    public String[] getRepositoryUrls() {
        return repositoryUrls;
    }

    /**
     * Get the cache directory
     * @return The cache directory.
     */
    @Override
    public File getCacheDirectory() {
        return cacheDirectory;
    }

    /**
     * Set the cache directory
     * @param dir The cache directory
     */
    public void setCacheDirectory(final File dir) {
        this.cacheDirectory = dir;
    }

    @Override
    public void incCachedArtifacts() {
        this.cachedArtifacts++;
    }

    @Override
    public void incDownloadedArtifacts() {
        this.downloadedArtifacts++;
    }

    @Override
    public void incLocalArtifacts() {
        this.localArtifacts++;
    }

    /**
     * Get the number of cached artifacts
     * @return The number of cached artifacts
     */
    public long getCachedArtifacts() {
        return this.cachedArtifacts;
    }

    /**
     * Get the number of downloaded artifacts
     * @return The number of downloaded artifacts
     */
    public long getDownloadedArtifacts() {
        return this.downloadedArtifacts;
    }

    /**
     * Get the number of local artifacts
     * @return The number of local artifacts
     */
    public long getLocalArtifacts() {
        return this.localArtifacts;
    }
}
