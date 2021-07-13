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

import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Supports configuring which resource types should be supported as files for
 * thumbnail generation. Each type supported must be adaptable to a
 * java.io.InputStream.
 */
@ProviderType
public interface ThumbnailSupport {

    /**
     * Gets the property path for the resources's file meta type
     * 
     * @param resourceType the resource typefor which to look up the meta type
     * @return the meta type path for the node type
     * @throws java.lang.IllegalArgumentException the specified resource typeis not
     *                                            a supported
     */
    @NotNull
    String getMetaTypePropertyPath(@NotNull String resourceType);

    /**
     * Gets all of the resource types which support persisting renditions
     * 
     * @return the set of persistable resource types
     */
    @NotNull
    Set<String> getPersistableTypes();

    /**
     * Gets the path under which to persist the renditions for the specified
     * resource type
     * 
     * @param resourceType the resource type to get the rendition path
     * @return the path under which to persist the renditions for the resource type
     * @throws java.lang.IllegalArgumentException the specified resource typeis not
     *                                            a supported or does not support
     *                                            persistence
     */
    @NotNull
    String getRenditionPath(@NotNull String resourceType);

    /**
     * Gets the error Suffix for configuring the transformation servlet
     * 
     * @return the error suffix for the servlet
     */
    @NotNull
    String getServletErrorSuffix();

    /**
     * Gets the error resource path for configuring the transformation servlet
     * 
     * @return the error resource path for the servlet
     */
    @NotNull
    String getServletErrorResourcePath();

    /**
     * Gets all of the resource types which support generating and transforming
     * thumbnails. These resource types must be adaptable to a java.io.InputStream
     * 
     * @return the set of supported resource types
     */
    @NotNull
    Set<String> getSupportedTypes();

}
