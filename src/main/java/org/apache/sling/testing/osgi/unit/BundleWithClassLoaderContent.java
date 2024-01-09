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
package org.apache.sling.testing.osgi.unit;

import aQute.bnd.osgi.resource.ResourceUtils;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.connect.ConnectContent;
import org.osgi.resource.Resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class BundleWithClassLoaderContent implements ConnectContent {

    private final Resource resource;

    private final ClassLoader classLoader;

    private final Path tempDir;

    private volatile List<String> entries;

    public BundleWithClassLoaderContent(Resource resource, ClassLoader classLoader, Path tempDir) {
        this.resource = resource;
        this.classLoader = classLoader;
        this.tempDir = tempDir;
    }

    @Override
    public Optional<Map<String, String>> getHeaders() {
        return Optional.empty();
    }

    @Override
    public Iterable<String> getEntries() {
        return entries;
    }

    @Override
    public Optional<ConnectEntry> getEntry(String path) {
        return Optional.of(path)
                .filter(entries::contains)
                .map(Entry::new);
    }

    @Override
    public Optional<ClassLoader> getClassLoader() {
        return Optional.of(classLoader);
    }

    @Override
    public void open() throws IOException {
        final Optional<URI> uri = ResourceUtils.getURI(resource);
        try (ZipFile zipFile = new ZipFile(Path.of(uri.get()).toFile())) {
            this.entries = Collections.list(zipFile.entries()).stream()
                    .map(ZipEntry::getName)
                    .collect(Collectors.toUnmodifiableList());
        }
    }

    @Override
    public void close() throws IOException {
        Files.walkFileTree(getBundleDirectory(), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
        this.entries = List.of();
    }

    @NotNull
    private Path getBundleDirectory() {
        return this.tempDir;
    }

    private class Entry implements ConnectEntry {
        private final String path;

        public Entry(String path) {
            this.path = path;
        }

        @Override
        public String getName() {
            return path;
        }

        @Override
        public long getContentLength() {
            return withZipEntry(((zipFile, zipEntry) -> zipEntry.getSize()));
        }

        @Override
        public long getLastModified() {
            return withZipEntry(((zipFile, zipEntry) -> zipEntry.getLastModifiedTime().toMillis()));
        }

        @Override
        public InputStream getInputStream() {
            return withZipEntry(this::copyAndRead);
        }

        private InputStream copyAndRead(ZipFile zipFile, ZipEntry zipEntry) throws IOException {
            final long size = zipEntry.getSize();
            if (0 <= size && size < 512 * 1024) {
                final ByteArrayOutputStream bytes = new ByteArrayOutputStream((int) size);
                zipFile.getInputStream(zipEntry).transferTo(bytes);
                return new ByteArrayInputStream(bytes.toByteArray());
            } else {
                final Path entryPath = getBundleDirectory().resolve(zipEntry.getName());
                if (Files.notExists(entryPath)) {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zipFile.getInputStream(zipEntry), entryPath);
                }
                return Files.newInputStream(entryPath);
            }
        }

        private <T> T withZipEntry(ThrowingBiFunction<ZipFile, ZipEntry, T> zipEntryFn) {
            final URI uri = ResourceUtils.getURI(resource).get();
            try (ZipFile zipFile = new ZipFile(Path.of(uri).toFile())) {
                final ZipEntry entry = zipFile.getEntry(path);
                return zipEntryFn.apply(zipFile, entry);
            } catch (Throwable e) {
                throw new UncheckedIOException(new IOException(e));
            }
        }
    }

    private interface ThrowingBiFunction<S, T, R> {
        R apply(S s, T t) throws Throwable;
    }
}
