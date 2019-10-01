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

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

/**
 * A {@code RemoteStorageProvider} is responsible for retrieving the {@link RemoteResourceReference}s corresponding to a certain Sling path.
 */
@ProviderType
public interface RemoteStorageProvider {

    /**
     * OSGi registration property that will be used to set the value of the
     * {@link org.apache.sling.spi.resource.provider.ResourceProvider#PROPERTY_ROOT} registration property of the
     * associated
     * {@link org.apache.sling.spi.resource.provider.ResourceProvider}. If this property is missing, empty or invalid, no
     * {@link org.apache.sling.spi.resource.provider.ResourceProvider} instance will be associated to this {@code RemoteStorageProvider}.
     */
    String PROP_RESOURCE_PROVIDER_ROOT = "resource.provider.root";

    /**
     * OSGi registration property that will be used to set the value of the
     * {@link org.apache.sling.spi.resource.provider.ResourceProvider#PROPERTY_AUTHENTICATE} registration property of the associated
     * {@link org.apache.sling.spi.resource.provider.ResourceProvider}.
     */
    String PROP_RESOURCE_PROVIDER_AUTHENTICATE = "resource.provider.authenticate";

    /**
     * OSGi registration property indicating which folder provided by this {@code RemoteStorageProvider} will be used to generate the
     * {@link org.apache.sling.api.resource.Resource} tree mapped at the location provided by {@link #PROP_RESOURCE_PROVIDER_ROOT}.
     */
    String PROP_REMOTE_STORAGE_PROVIDER_ROOT = "remote.storage.provider.root";

    /**
     * The name of a JSON file providing the representation of a Sling resource tree. First level object properties provide the current
     * resource's properties, whereas subsequent objects provide full descriptions of children resources.
     * <br><br>
     * <p>Example:
     * <pre>
     *  {
     *     "sling:resourceType": "org.example.page/1.0.0",
     *     "par-1": {
     *         "sling:resourceType": "org.example.paragraph/1.0.0",
     *         "text": "&lt;h2&gt;Hello World!&lt;/h2&gt;"
     *     },
     *     "image-1": {
     *         "sling:resourceType": "org.example.image/1.0.0",
     *         "url": "https://sling.apache.org/res/logos/sling.svg"
     *     }
     * }
     * </pre>
     * <p>
     * The above JSON description would identify the following resource tree:
     * <pre>
     *     &lt;resource&gt;
     *          sling:resourceType=org.example.page/1.0.0
     *          /par-1
     *              sling:resourceType=org.example.paragraph/1.0.0
     *              text=&lt;h2&gt;Hello World!&lt;/h2&gt;
     *          /image-1
     *              sling:resourceType=org.example.image/1.0.0
     *              url=https://sling.apache.org/res/logos/sling.svg
     * </pre>
     * where {@code resource} would be replaced by the name of the {@link Directory} where this {@link #SLING_META_FILE} is stored.
     */
    String SLING_META_FILE = ".sling.json";

    /**
     * Finds the {@link RemoteResourceReference} corresponding to a {@code slingPath}.
     * <br><br>
     * <p>The value of the {@link #PROP_RESOURCE_PROVIDER_ROOT} service registration property provides the value of the
     * {@link org.apache.sling.spi.resource.provider.ResourceProvider#PROPERTY_ROOT} property of the calling
     * {@link org.apache.sling.spi.resource.provider.ResourceProvider}. In this way, the {@code
     * RemoteStorageProvider} is free to provide its own {@code slingPath} -> {@code RemoteResourceReference} mapping algorithm, as long as
     * the following rules are obeyed:
     * <ol>
     * <li>
     * a {@link RemoteResourceReference} can map directly to a {@code slingPath} if the {@link RemoteResourceReference} is either a {@link
     * File}
     * that is not a {@link #SLING_META_FILE} or a {@link Directory}
     * </li>
     * <li>
     * a {@link RemoteResourceReference} can be mapped to multiple {@code slingPath}s only when the {@link RemoteResourceReference} is a
     * {@link #SLING_META_FILE}
     * </li>
     * </ol>
     *
     */
    @Nullable
    RemoteResourceReference findResource(@NotNull String slingPath, @NotNull Map<String, Object> authenticationInfo);

    @Nullable
    File getFile(@NotNull RemoteResourceReference reference, @NotNull Map<String, Object> authenticationInfo);

    @Nullable
    Directory getDirectory(@NotNull RemoteResourceReference reference, @NotNull Map<String, Object> authenticationInfo);

    /**
     * Indicates if {@code this RemoteStorageProvider} is read only or not. If a
     * {@link org.apache.sling.spi.resource.provider.ResourceProvider} should be associated to {@code this} {@code RemoteStorageProvider},
     * this information will be reflected in the {@link org.apache.sling.spi.resource.provider.ResourceProvider}'s
     * {@link org.apache.sling.spi.resource.provider.ResourceProvider#PROPERTY_MODIFIABLE} property.
     */
    default boolean isReadOnly() {
        return true;
    }

    default boolean supportsVersions() {
        return false;
    }

    String slingPath(@NotNull String storagePath);

    String storagePath(@NotNull String slingPath);

    /**
     * If a {@code handler} is registered for {@code this RemoteStorageProvider}, the handler should be notified every time there is a
     * change in the files and folders {@code this} provider makes available.
     * @param handler
     */
    void registerEventHandler(RemoteResourceEventHandler handler);

}
