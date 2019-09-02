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

import org.apache.sling.remote.resourceprovider.AbstractRemoteResourceReference;
import org.apache.sling.remote.resourceprovider.RemoteStorageProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.Metadata;

public class DropboxResourceReference extends AbstractRemoteResourceReference {

    private final DropboxStorageProvider remoteStorageProvider;
    private final Metadata metadata;
    private final Type type;

    DropboxResourceReference(DropboxStorageProvider remoteStorageProvider, Metadata metadata) {
        this.remoteStorageProvider = remoteStorageProvider;
        this.metadata = metadata;
        if (metadata instanceof FileMetadata) {
            type = Type.FILE;
        } else {
            type = Type.DIRECTORY;
        }
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public RemoteStorageProvider getProvider() {
        return remoteStorageProvider;
    }

    @Override
    public @NotNull String getPath() {
        return metadata.getPathLower();
    }

    @Override
    public @NotNull String getName() {
        return metadata.getName();
    }

    @Override
    public long getCreated() {
        return 0;
    }

    @Override
    public long getLastModified() {
        if (type == Type.FILE) {
            FileMetadata fileMetadata = (FileMetadata) metadata;
            return fileMetadata.getServerModified().getTime();
        }
        return 0;
    }

    @Override
    public long getSize() {
        if (type == Type.FILE) {
            FileMetadata fileMetadata = (FileMetadata) metadata;
            return fileMetadata.getSize();
        }
        return 0;
    }

    @Override
    public @Nullable String getRevision() {
        if (type == Type.FILE) {
            FileMetadata fileMetadata = (FileMetadata) metadata;
            return fileMetadata.getRev();
        }
        return null;
    }

    Metadata getMetadata() {
        return metadata;
    }
}
