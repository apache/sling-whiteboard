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

package org.apache.sling.remote.resourceprovider.dropbox.impl;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.path.Path;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.remote.resourceprovider.Directory;
import org.apache.sling.remote.resourceprovider.File;
import org.apache.sling.remote.resourceprovider.RemoteResourceEvent;
import org.apache.sling.remote.resourceprovider.RemoteResourceEventHandler;
import org.apache.sling.remote.resourceprovider.RemoteResourceEventType;
import org.apache.sling.remote.resourceprovider.RemoteResourceReference;
import org.apache.sling.remote.resourceprovider.RemoteStorageProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.http.StandardHttpRequestor;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.DeletedMetadata;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.GetMetadataErrorException;
import com.dropbox.core.v2.files.ListFolderLongpollResult;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.LookupError;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.users.FullAccount;

@Component(
        service = RemoteStorageProvider.class,
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(
        ocd = DropboxStorageProviderConfiguration.class,
        factory = true
)
public class DropboxStorageProvider implements RemoteStorageProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DropboxStorageProvider.class);

    private DbxClientV2 client;
    private DbxClientV2 longPollClient;
    private Path slingMountPoint;
    private String dropboxRootPath;

    private String cursor;
    private RemoteResourceEventHandler eventHandler;
    private volatile boolean pollingActive;
    private final Object pollingLock = new Object();

    @Reference
    private MimeTypeService mimeTypeService;

    @Activate
    private void activate(DropboxStorageProviderConfiguration configuration) {
        if (StringUtils.isNotEmpty(configuration.accessToken())) {
            DbxRequestConfig requestConfig = DbxRequestConfig.newBuilder(this.getClass().getName()).build();
            client = new DbxClientV2(requestConfig, configuration.accessToken());
            StandardHttpRequestor.Config longpollConfig = StandardHttpRequestor.Config.DEFAULT_INSTANCE.copy().withReadTimeout(5,
                    TimeUnit.MINUTES).build();
            DbxRequestConfig pollingRequestConfig = DbxRequestConfig.newBuilder(this.getClass().getName() + "-longpoll")
                    .withHttpRequestor(new StandardHttpRequestor(longpollConfig))
                    .build();
            longPollClient = new DbxClientV2(pollingRequestConfig, configuration.accessToken());
            try {
                FullAccount account = client.users().getCurrentAccount();
                LOGGER.info("Initialised Dropbox provider for {}.", account.getName().getDisplayName());
                dropboxRootPath = configuration.remote_storage_provider_root();
                if (dropboxRootPath.isEmpty()) {
                    dropboxRootPath = "/";
                }
                slingMountPoint = new Path(configuration.resource_provider_root());
                cursor = client.files()
                        .listFolderGetLatestCursorBuilder("/".equals(dropboxRootPath) ? "" : dropboxRootPath)
                        .withIncludeDeleted(true)
                        .withIncludeMediaInfo(false)
                        .withRecursive(true)
                        .start().getCursor();
            } catch (Exception e) {
                LOGGER.error("Unable to initialise a Dropbox Storage Provider for configuration {}.", configuration);
                throw new IllegalStateException(e);
            }
        } else {
            throw new IllegalStateException("The access token cannot be empty.");
        }
    }

    @Deactivate
    private void deactivate() {
        if (pollingActive) {
            pollingActive = false;
        }
    }

    @Override
    public @Nullable RemoteResourceReference findResource(@NotNull String slingPath, @Nullable Map<String, Object> authenticationInfo) {
        String storagePath = storagePath(slingPath);
        if ("/".equals(storagePath)) {
            return null;
        }
        while (storagePath != null && !"/".equals(storagePath)) {
            try {
                Metadata metadata = client.files().getMetadata(storagePath);
                return new DropboxResourceReference(this, metadata);
            } catch (GetMetadataErrorException e) {
                LookupError lookupError = e.errorValue.getPathValue();
                if (!lookupError.isNotFound()) {
                    LOGGER.error(String.format("Unexpected error retrieving Dropbox resource from %s.", storagePath), e);
                } else {
                    storagePath = ResourceUtil.getParent(storagePath);
                }
            } catch (DbxException e) {
                LOGGER.error(String.format("Unexpected error retrieving Dropbox resource from %s.", storagePath), e);
            } catch (IllegalArgumentException e) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format("Sling path %s corresponding to Dropbox storage path %s is invalid for this provider.",
                            slingPath, storagePath), e);
                }
            }
        }
        return null;
    }

    @Override
    public @Nullable File getFile(@NotNull RemoteResourceReference reference, @Nullable Map<String, Object> authenticationInfo) {
        if (reference instanceof DropboxResourceReference) {
            DropboxResourceReference dropboxResourceReference = (DropboxResourceReference) reference;
            if (dropboxResourceReference.getType() == RemoteResourceReference.Type.FILE) {
                return new DropboxFile(this, (FileMetadata) dropboxResourceReference.getMetadata(), mimeTypeService);
            }
        }
        return null;
    }

    @Override
    public @Nullable Directory getDirectory(@NotNull RemoteResourceReference reference, @Nullable Map<String, Object> authenticationInfo) {
        if (reference instanceof DropboxResourceReference) {
            DropboxResourceReference dropboxResourceReference = (DropboxResourceReference) reference;
            if (dropboxResourceReference.getType() == RemoteResourceReference.Type.DIRECTORY) {
                return new DropboxDirectory(this, (FolderMetadata) dropboxResourceReference.getMetadata());
            }
        }
        return null;
    }

    @Override
    public String slingPath(@NotNull String storagePath) {
        if (dropboxRootPath.equals(storagePath) ||
                storagePath.startsWith(dropboxRootPath.endsWith("/") ? dropboxRootPath : dropboxRootPath + "/")) {
            return ResourceUtil.normalize(slingMountPoint.getPath() + "/" + storagePath.substring(dropboxRootPath.length()));
        }
        return null;
    }

    @Override
    public String storagePath(@NotNull String slingPath) {
        if (slingMountPoint.matches(slingPath)) {
            return ResourceUtil.normalize(dropboxRootPath.concat(slingPath.substring(slingMountPoint.getPath().length())));
        }
        return null;
    }

    @Override
    public void registerEventHandler(RemoteResourceEventHandler handler) {
        synchronized (this) {
            if (eventHandler == null) {
                eventHandler = handler;
                pollingActive = true;
                final long longpollTimeoutSecs = TimeUnit.MINUTES.toSeconds(2);
                new Thread(() -> {
                    synchronized (pollingLock) {
                        while (pollingActive) {
                            try {
                                ListFolderLongpollResult pollResult =
                                        longPollClient.files().listFolderLongpoll(cursor, longpollTimeoutSecs);
                                if (pollResult.getChanges()) {
                                    boolean hasChanges = true;
                                    TreeSet<String> changedPaths = new TreeSet<>();
                                    TreeSet<String> deletedPaths = new TreeSet<>();
                                    while (hasChanges) {
                                        ListFolderResult result = client.files().listFolderContinue(cursor);
                                        for (Metadata metadata : result.getEntries()) {
                                            if (metadata instanceof DeletedMetadata) {
                                                deletedPaths.add(metadata.getPathLower());
                                            } else {
                                                changedPaths.add(metadata.getPathLower());
                                            }
                                        }
                                        hasChanges = result.getHasMore();
                                        cursor = result.getCursor();
                                    }
                                    if (!changedPaths.isEmpty()) {
                                        handler.handleEvent(new DropboxChangeEvent(RemoteResourceEventType.CHANGED, changedPaths));
                                    }
                                    if (!deletedPaths.isEmpty()) {
                                        handler.handleEvent(new DropboxChangeEvent(RemoteResourceEventType.DELETED, deletedPaths));
                                    }
                                }
                                Long backoff = pollResult.getBackoff();
                                if (backoff != null) {
                                    try {
                                        if (LOGGER.isDebugEnabled()) {
                                            LOGGER.debug("Next poll will be executed in {} seconds.", backoff);
                                        }
                                        pollingLock.wait(TimeUnit.SECONDS.toMillis(backoff));
                                    } catch (InterruptedException ex) {
                                        LOGGER.error("Stopping long-poll thread.", ex);
                                        pollingActive = false;
                                        Thread.currentThread().interrupt();
                                    }
                                }
                            } catch (DbxException ex) {
                                LOGGER.error("Unable to register long-poll hook.", ex);
                                pollingActive = false;
                            }
                        }
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Stopping long-poll thread.");
                        }
                    }
                }, this.getClass().getName() + "-longpoll").start();
            }
        }
    }


    static final class DropboxChangeEvent implements RemoteResourceEvent {

        private final RemoteResourceEventType type;
        private final TreeSet<String> paths;

        DropboxChangeEvent(RemoteResourceEventType type, TreeSet<String> paths) {
            this.type = type;
            this.paths = new TreeSet<>(paths);
        }

        @Override
        public RemoteResourceEventType getType() {
            return type;
        }

        @Override
        public Set<String> getPaths() {
            return paths;
        }

        @Override
        public String toString() {
            return DropboxChangeEvent.class.getName() + "{type=" + type.toString() + "; paths=" + paths + "}";
        }
    }

    DbxClientV2 getClient() {
        return client;
    }
}
