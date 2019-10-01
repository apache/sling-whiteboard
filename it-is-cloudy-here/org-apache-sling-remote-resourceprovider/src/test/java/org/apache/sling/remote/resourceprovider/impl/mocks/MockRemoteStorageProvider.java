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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.remote.resourceprovider.Directory;
import org.apache.sling.remote.resourceprovider.File;
import org.apache.sling.remote.resourceprovider.RemoteResourceEventHandler;
import org.apache.sling.remote.resourceprovider.RemoteResourceReference;
import org.apache.sling.remote.resourceprovider.RemoteStorageProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MockRemoteStorageProvider implements RemoteStorageProvider {

    final java.io.File root;
    final Map<String, Set<String>> whitelist;

    public MockRemoteStorageProvider() {
        this(Collections.emptyMap());
    }

    public MockRemoteStorageProvider(Map<String, Set<String>> whitelist) {
        this.root = new java.io.File("src/test/resources");
        this.whitelist = whitelist;
    }


    @Override
    public @Nullable RemoteResourceReference findResource(@NotNull String slingPath, @NotNull Map<String, Object> authenticationInfo) {
        String path = slingPath.startsWith("/") ? slingPath.substring(1) : slingPath;
        java.io.File file = new java.io.File(root, path);
        while (file.getAbsolutePath().startsWith(root.getAbsolutePath())) {
            if (file.exists()) {
                if (!whitelist.isEmpty()) {
                    String user = (String) authenticationInfo.get(ResourceResolverFactory.USER);
                    if (user == null || user.isEmpty()) {
                        return null;
                    }
                    Set<String> whitelistedUsers = whitelist.get(file.getPath());
                    if (whitelistedUsers != null) {
                        if (whitelistedUsers.contains(user)) {
                            return new MockRemoteResourceReference(this, file);
                        }
                    }
                } else {
                    return new MockRemoteResourceReference(this, file);
                }
            }
            file = file.getParentFile();
        }
        return null;
    }

    @Override
    public @Nullable File getFile(@NotNull RemoteResourceReference reference, @Nullable Map<String, Object> authenticationInfo) {
        return new MockFile(this, ((MockRemoteResourceReference) reference).file);
    }

    @Override
    public @Nullable Directory getDirectory(@NotNull RemoteResourceReference reference, @Nullable Map<String, Object> authenticationInfo) {
        java.io.File file = ((MockRemoteResourceReference) reference).file;
        if (!file.exists() || !file.isDirectory()) {
            return null;
        }
        List<RemoteResourceReference> children = new ArrayList<>();
        for (java.io.File f : file.listFiles()) {
            MockRemoteResourceReference child = null;
            if (!whitelist.isEmpty()) {
                String user = (String) authenticationInfo.get(ResourceResolverFactory.USER);
                if (user == null || user.isEmpty()) {
                    return null;
                }
                Set<String> whitelistedUsers = whitelist.get(f.getPath());
                if (whitelistedUsers != null) {
                    if (whitelistedUsers.contains(user)) {
                        child = new MockRemoteResourceReference(this, f);
                    }
                }
            } else {
                child = new MockRemoteResourceReference(this, f);
            }
            if (child != null) {
                children.add(child);
            }
        }
        return new MockDirectory(this, file, children);
    }

    @Override
    public String slingPath(@NotNull String storagePath) {
        return storagePath;
    }

    @Override
    public String storagePath(@NotNull String slingPath) {
        return slingPath;
    }

    @Override
    public void registerEventHandler(RemoteResourceEventHandler handler) {

    }

    private static class MockFile extends MockRemoteResourceReference implements File {

        private final java.io.File file;
        private final String mime;

        MockFile(MockRemoteStorageProvider remoteStorageProvider, java.io.File file) {
            super(remoteStorageProvider, file);
            this.file = file;
            mime = MimeTypes.getType(this);
        }

        @Override
        public @NotNull InputStream getInputStream() throws IOException {
            return new FileInputStream(file);
        }

        @Override
        public @Nullable String getMimeType() {
            return mime;
        }
    }

    private static class MockDirectory extends MockRemoteResourceReference implements Directory {

        private final List<RemoteResourceReference> children;

        MockDirectory(MockRemoteStorageProvider remoteStorageProvider, java.io.File file, List<RemoteResourceReference> children) {
            super(remoteStorageProvider, file);
            this.children = children;
        }

        @Override
        public @NotNull List<RemoteResourceReference> getChildren() {
            return children;
        }
    }
}
