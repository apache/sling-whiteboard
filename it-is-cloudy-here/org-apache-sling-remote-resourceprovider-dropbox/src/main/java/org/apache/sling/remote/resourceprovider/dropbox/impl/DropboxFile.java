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

import java.io.IOException;
import java.io.InputStream;

import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.remote.resourceprovider.File;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;

public class DropboxFile extends DropboxResourceReference implements File {

    private final FileMetadata metadata;
    private final DbxClientV2 client;
    private final MimeTypeService mimeTypeService;
    private String mimeType;

    DropboxFile(DropboxStorageProvider remoteStorageProvider, FileMetadata metadata, MimeTypeService mimeTypeService) {
        super(remoteStorageProvider, metadata);
        this.metadata = metadata;
        this.mimeTypeService = mimeTypeService;
        this.client = remoteStorageProvider.getClient();
    }

    @Override
    public @NotNull InputStream getInputStream() throws IOException {
        try {
            return client.files().download(metadata.getPathLower()).getInputStream();
        } catch (DbxException e) {
            throw new IOException(String.format("Unable to download file %s.", metadata.getPathLower()), e);
        }
    }

    @Override
    public @Nullable String getMimeType() {
        if (mimeType == null) {
            mimeType = mimeTypeService.getMimeType(metadata.getName());
        }
        return mimeType;
    }
}
