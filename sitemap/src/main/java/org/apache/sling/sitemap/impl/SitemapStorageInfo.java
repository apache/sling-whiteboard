/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.sitemap.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Calendar;

class SitemapStorageInfo {

    private final String sitemapSelector;
    private final String name;
    private final String path;
    private final int fileIndex;
    private final Calendar lastModified;
    private final int size;
    private final int entries;

    SitemapStorageInfo(@NotNull String path, @NotNull String sitemapSelector, @NotNull String name, int fileIndex,
                       @Nullable Calendar lastModified, int size,
                       int entries) {
        this.path = path;
        this.sitemapSelector = sitemapSelector;
        this.name = name;
        this.fileIndex = fileIndex;
        this.lastModified = lastModified;
        this.size = size;
        this.entries = entries;
    }

    @NotNull
    public String getPath() {
        return path;
    }

    @NotNull
    public String getSitemapSelector() {
        return sitemapSelector;
    }

    @NotNull
    public String getName() {
        return name;
    }

    public int getFileIndex() {
        return fileIndex;
    }

    @Nullable
    public Calendar getLastModified() {
        return lastModified;
    }

    public int getSize() {
        return size;
    }

    public int getEntries() {
        return entries;
    }
}
