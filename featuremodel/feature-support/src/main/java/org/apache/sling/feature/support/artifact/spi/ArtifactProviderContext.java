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
package org.apache.sling.feature.support.artifact.spi;

import java.io.File;

/**
 * This is the context for the artifact providers
 */
public interface ArtifactProviderContext {

    /**
     * Get the cache directory
     * @return The cache directory.
     */
    File getCacheDirectory();

    /**
     * Inform about an artifact found in the cache.
     */
    void incCachedArtifacts();

    /**
     * Inform about an artifact being downloaded
     */
    void incDownloadedArtifacts();

    /**
     * Inform about an artifact found locally.
     */
    void incLocalArtifacts();
}
