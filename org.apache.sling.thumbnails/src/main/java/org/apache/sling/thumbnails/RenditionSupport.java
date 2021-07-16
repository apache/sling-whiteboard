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
package org.apache.sling.thumbnails;

import java.io.InputStream;
import java.util.List;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Service for interacting with renditions
 */
@ProviderType
public interface RenditionSupport {

    /**
     * Retrieves the rendition with the specified rendition name, if one exists.
     * 
     * @param file          the file from which to retrieve the rendition
     * @param renditionName the rendition to retrieve
     * @return the rendition resource or null
     */
    @Nullable
    Resource getRendition(@NotNull Resource file, @NotNull String renditionName);

    /**
     * Retrieves the inputstream of the data of a rendition with the specified
     * rendition name, if one exists.
     * 
     * @param file          the file from which to retrieve the rendition
     * @param renditionName the rendition to retrieve
     * @return the rendition contents or null
     */
    @Nullable
    InputStream getRenditionContent(@NotNull Resource file, @NotNull String renditionName);

    /**
     * Retrieves all of the renditions for the specified file.
     * 
     * @param file the file from which to retrieve the renditions
     * @return the renditions
     */
    @NotNull
    List<Resource> listRenditions(@NotNull Resource file);

    /**
     * Returns true if the requested rendition exists for the specified file.
     * 
     * @param file          the file to check
     * @param renditionName the rendition name to check (including extension)
     * @return true if the rendition exists, false otherwise
     */
    boolean renditionExists(@NotNull Resource file, @NotNull String renditionName);

    /**
     * Checks if the file supports renditions, e.g. it's defined as a Persistable
     * Type.
     * 
     * @param file the file to check
     * @return true if the file supports renditons, false otherwise
     */
    boolean supportsRenditions(@NotNull Resource file);

    /**
     * Sets the content of the rendition, overriding any existing content
     * 
     * @param file
     * @param renditionName
     * @param baos
     */
    void setRendition(@NotNull Resource file, @NotNull String renditionName, @NotNull InputStream baos)
            throws PersistenceException;

}
