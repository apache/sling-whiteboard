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
package org.apache.sling.feature.support.spi;

import java.io.File;
import java.io.IOException;

/**
 * The artifact provider is an extension point for providing artifacts
 * from different sources, like for example s3.
 */
public interface ArtifactProvider {

    /**
     * The protocol name of the provider, e.g. "s3"
     * @return The protocol name.
     */
    String getProtocol();

    /**
     * Initialize the provider.
     * @param context The context
     * @throws IOException If the provider can't be initialized.
     */
    void init(ArtifactProviderContext context) throws IOException;

    /**
     * Shutdown the provider.
     */
    void shutdown();

    /**
     * Get a local file for the artifact URL.
     *
     * @param url Artifact url
     * @param relativeCachePath A relative path that can be used as a cache path
     *                          by the provider. The path does not start with a slash.
     * @return A file if the artifact exists or {@code null}
     */
    File getArtifact(String url, String relativeCachePath);
}
