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

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.remote.resourceprovider.Directory;
import org.apache.sling.remote.resourceprovider.RemoteResourceReference;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.DeletedMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderContinueErrorException;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

public class DropboxDirectory extends DropboxResourceReference implements Directory {

    private static final Logger LOGGER = LoggerFactory.getLogger(DropboxDirectory.class);

    private final FolderMetadata metadata;
    private final DropboxStorageProvider dropboxStorageProvider;
    private final DbxClientV2 client;

    DropboxDirectory(DropboxStorageProvider dropboxStorageProvider, FolderMetadata metadata) {
        super(dropboxStorageProvider, metadata);
        this.dropboxStorageProvider = dropboxStorageProvider;
        this.metadata = metadata;
        this.client = dropboxStorageProvider.getClient();
    }

    @Override
    public @NotNull List<RemoteResourceReference> getChildren() {
        ArrayList<RemoteResourceReference> children = new ArrayList<>();
        ListFolderResult result;
        try {
            result = client.files().listFolder(metadata.getPathLower());
            boolean hasMoreResults = true;
            while (hasMoreResults) {
                for (Metadata entry : result.getEntries()) {
                    if (!(entry instanceof DeletedMetadata)) {
                        children.add(new DropboxResourceReference(dropboxStorageProvider, entry));
                    }
                }
                result = client.files().listFolderContinue(result.getCursor());
                hasMoreResults = result.getHasMore();
            }
        } catch (ListFolderContinueErrorException e) {
            LOGGER.error(String.format("Cannot read all children of folder %s.", metadata.getPathLower()), e);
        } catch (
                DbxException ex) {
            LOGGER.error(String.format("Cannot access path %s on Dropbox.", metadata.getPathLower()), ex);
        }
        return children;
    }
}
