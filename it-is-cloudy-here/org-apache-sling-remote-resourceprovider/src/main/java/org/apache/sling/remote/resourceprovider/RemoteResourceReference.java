/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Licensed to the Apache Software Foundation (ASF) under one
 ~ or more contributor license agreements.  See the NOTICE file
 ~ distributed with this work for additional information
 ~ regarding copyright ownership.  The ASF licenses this file
 ~ to you under the Apache License, Version 2.0 (the
 ~ "License"); you may not use this file except in compliance
 ~ with the License.  You may obtain a copy of the License at
 ~
 ~   http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package org.apache.sling.remote.resourceprovider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

/**
 * This interface represents the base object for files and folders provided by a remote file storage provider. Implementations of this
 * interface have to make sure that two {@code RemoteResourceReference} are equal if and only if all of the following are true:
 *
 * <ol>
 *     <li>{@link #getProvider()} returns the same provider for both references</li>
 *     <li>{@link #getPath()} returns the same path for both references</li>
 * </ol>
 */
@ProviderType
public interface RemoteResourceReference {

    enum Type {
        FILE, DIRECTORY
    }

    /**
     * Returns {@code this} reference's type.
     * @return the type of this reference
     */
    Type getType();

    /**
     * Returns the {@link RemoteStorageProvider} instance responsible for creating {@code this} reference.
     *
     * @return the remote storage provider
     */
    RemoteStorageProvider getProvider();

    /**
     * Returns {@code this} resource's storage path in its remote system.
     *
     * @return the resource's path
     */
    @NotNull
    String getPath();

    /**
     * Returns {@code this} resource's name (e.g. the last part from the resource's path after the last "/").
     *
     * @return the resource's name
     * @see #getPath()
     */
    @NotNull
    default String getName() {
        String path = getPath();
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        int lastSlashIndex = path.lastIndexOf('/');
        if (lastSlashIndex > -1 && path.length() > 1) {
            return path.substring(path.lastIndexOf('/') + 1);
        }
        return "/";
    }

    /**
     * Returns the date when {@code this} resource was created.
     *
     * @return A {@code long} value representing the time the resource was created, measured in milliseconds since the epoch (00:00:00
     * GMT, January 1, 1970)
     */
    long getCreated();

    /**
     * Returns the last modified date of {@code this} resource. The value returned by this method should be equal or greater than the value
     * returned by {@link #getCreated()}.
     *
     * @return A {@code long} value representing the time the resource was last modified, measured in milliseconds since the epoch (00:00:00
     * GMT, January 1, 1970)
     * @see #getCreated()
     */
    default long getLastModified() {
        return getCreated();
    }

    /**
     * Returns {@code this} resource's size in bytes.
     *
     * @return the resource size in bytes
     */
    long getSize();

    /**
     * Returns {@code this} resource's revision, if the remote file storage provider supports this feature.
     *
     * @return the resource's revision or {@code null}
     */
    @Nullable
    String getRevision();
}
