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
package org.apache.sling.remote.resourceprovider.impl.mocks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.sling.remote.resourceprovider.AbstractRemoteResourceReference;
import org.apache.sling.remote.resourceprovider.RemoteStorageProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockRemoteResourceReference extends AbstractRemoteResourceReference {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockRemoteResourceReference.class);

    final MockRemoteStorageProvider remoteStorageProvider;
    final File file;
    private final String path;
    private final long createdDate;
    private final long lastModifiedDate;

    public MockRemoteResourceReference(MockRemoteStorageProvider remoteStorageProvider, File file) {
        this.remoteStorageProvider = remoteStorageProvider;
        this.file = file;
        path = file.getAbsolutePath().substring(remoteStorageProvider.root.getAbsolutePath().length());
        BasicFileAttributes basicFileAttributes = null;
        try {
            basicFileAttributes = Files.readAttributes(Paths.get(file.getAbsolutePath()), BasicFileAttributes.class);
        } catch (IOException e) {
            LOGGER.warn("Unable to read file attributes.", e);
        }
        if (basicFileAttributes != null) {
            createdDate = basicFileAttributes.creationTime().toMillis();
            lastModifiedDate = basicFileAttributes.lastModifiedTime().toMillis();
        } else {
            createdDate = 0;
            lastModifiedDate = file.lastModified();
        }
    }

    @Override
    public Type getType() {
        if (file.isDirectory()) {
            return Type.DIRECTORY;
        }
        return Type.FILE;
    }

    @Override
    public RemoteStorageProvider getProvider() {
        return remoteStorageProvider;
    }

    @Override
    public @NotNull String getPath() {
        return path;
    }

    @Override
    public long getCreated() {
        return createdDate;
    }

    @Override
    public long getLastModified() {
        return lastModifiedDate;
    }

    @Override
    public long getSize() {
        return 0;
    }

    @Override
    public @Nullable String getRevision() {
        return null;
    }
}
