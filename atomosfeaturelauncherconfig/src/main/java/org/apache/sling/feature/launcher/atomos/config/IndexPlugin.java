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
package org.apache.sling.feature.launcher.atomos.config;

import org.apache.felix.atomos.utils.api.Context;
import org.apache.felix.atomos.utils.api.FileType;
import org.apache.felix.atomos.utils.api.IndexInfo;
import org.apache.felix.atomos.utils.api.plugin.JarPlugin;
import org.apache.felix.atomos.utils.core.IndexInfoImpl;
import org.apache.felix.atomos.utils.core.plugins.index.IndexOutputType;
import org.apache.felix.atomos.utils.core.plugins.index.IndexPluginConfig;
import org.apache.felix.atomos.utils.substrate.impl.config.DefaultResourceConfiguration;
import org.apache.felix.atomos.utils.substrate.impl.json.ResourceJsonUtil;
import org.osgi.framework.Constants;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public class IndexPlugin implements JarPlugin<IndexPluginConfig> {
    private static final String ATOMOS_BUNDLE_SEPARATOR = "ATOMOS_BUNDLE";

    public static final String ATOMOS_BUNDLES_BASE_PATH = "atomos/";
    public static final String ATOMOS_INDEX_FILE_NAME = "bundles.index";
    public static final String ATOMOS_BUNDLES_INDEX = ATOMOS_BUNDLES_BASE_PATH
            + ATOMOS_INDEX_FILE_NAME;

    public static final String ATOMOS_CATH_ALL = "atomos/.*$";

    private static final String ATOMOS_SUBSTRATE_JAR = "atomos.substrate.jar";

    private boolean include(JarEntry entry) {
        final String path = entry.getName();
        if (entry.isDirectory() || isClass(path)) {
            return false;
        }
        return true;
    }

    private boolean isClass(String path) {
        return path.endsWith(".class");
    }

    private AtomicLong counter = new AtomicLong();

    JarOutputStream jos;

    private ArrayList<IndexInfo> indexInfos;
    private Map<String, Boolean> uniquePaths;

    private Path substrateJar;

    private IndexPluginConfig config;

    @Override
    public void initJar(JarFile jar, Context context, URLClassLoader classLoader) {
        // Detect if there are duplicates using the uniquePaths map
        jar.stream().filter(this::include).forEach(e -> uniquePaths.compute(
                e.getName(),

                (p, b) -> //
                        // Always treat the bundle manifest as a duplicate
                        "META-INF/MANIFEST.MF".equals(p) //
                                // Always treat root resources as duplicate
                                || p.indexOf('/') < 0
                                // Check if this path was found already
                                || b != null //
                                ? Boolean.FALSE
                                : Boolean.TRUE));
    }

    @Override
    public void doJar(JarFile jar, Context context, URLClassLoader classLoader) {
        long id = counter.getAndIncrement();

        IndexInfoImpl info = new IndexInfoImpl();

        try {
            Manifest mf = jar.getManifest();
            Attributes attributes = mf == null ? null : mf.getMainAttributes();
            if (attributes != null) {
                info.setBsn(attributes.getValue(Constants.BUNDLE_SYMBOLICNAME));
                info.setVersion(attributes.getValue(Constants.BUNDLE_VERSION));
            }
        } catch (IOException e1) {

            e1.printStackTrace();
            return;
        }

        info.setId(Long.toString(id));

        if (info.getBundleSymbolicName() == null) {
            return;
        }
        if (info.getVersion() == null) {
            info.setVersion("0.0");
        }
        List<String> files = jar.stream().peek(j -> {
            try {
                if (Boolean.FALSE == uniquePaths.get(j.getName())) {
                    String fileName = ATOMOS_BUNDLES_BASE_PATH + id + "/" + j.getName();
                    if (isJarType()) {
                        final JarEntry entry = new JarEntry(fileName);
                        if (j.getCreationTime() != null) {
                            entry.setCreationTime(j.getCreationTime());
                        }
                        if (j.getComment() != null) {
                            entry.setComment(j.getComment());
                        }
                        jos.putNextEntry(entry);
                        jos.write(jar.getInputStream(j).readAllBytes());
                    } else {
                        Path path = config.indexOutputDirectory().resolve(fileName);
                        Files.createDirectories(path.getParent());
                        Files.write(path, jar.getInputStream(j).readAllBytes());
                        BasicFileAttributeView attrs = Files.getFileAttributeView(path,
                                BasicFileAttributeView.class);
                        FileTime time = j.getCreationTime();
                        attrs.setTimes(time, time, time);
                    }
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }

        }).map(JarEntry::getName).collect(Collectors.toList());
        try {
            String fileName = ATOMOS_BUNDLES_BASE_PATH + id + "/META-INF/atomos/file.location";
            if (isJarType()) {
                final JarEntry entry = new JarEntry(fileName);
                jos.putNextEntry(entry);
                jos.write(jar.getName().getBytes("UTF-8"));
            } else {
                Path path = config.indexOutputDirectory().resolve(fileName);
                Files.createDirectories(path.getParent());
                Files.write(path, jar.getName().getBytes("UTF-8"));
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        files.add("META-INF/atomos/file.location");
        info.setFiles(files);
        indexInfos.add(info);
    }

    @Override
    public void init(IndexPluginConfig config) {
        this.config = config;
    }

    /**
     * @return
     */
    private boolean isJarType() {
        return IndexOutputType.JAR.equals(config.indexOutputType());
    }

    @Override
    public void postJars(Context context) {
        try {

            final List<String> bundleIndexLines = new ArrayList<>();
            final Collection<String> resources = new LinkedHashSet<>();
            indexInfos.stream() //
                    .filter((i) -> i.getBundleSymbolicName() != null) //
                    .sorted(Comparator.comparing(IndexInfo::getBundleSymbolicName)) //
                    .forEach((i) -> {
                        bundleIndexLines.add(ATOMOS_BUNDLE_SEPARATOR);
                        bundleIndexLines.add(i.getId());
                        bundleIndexLines.add(i.getBundleSymbolicName());
                        bundleIndexLines.add(i.getVersion());
                        i.getFiles().forEach(f -> {
                            bundleIndexLines.add(f);
                            if (!isClass(f)) {
                                if (Boolean.FALSE == uniquePaths.get(f)) {
                                    resources.add(
                                            ATOMOS_BUNDLES_BASE_PATH + i.getId() + "/" + f);
                                }
                                if (!f.endsWith("/") && !"META-INF/MANIFEST.MF".equals(f)) {
                                    resources.add(f);
                                }
                            }
                        });
                    });

            ByteArrayOutputStream indexBytes;
            try (final ByteArrayOutputStream out = new ByteArrayOutputStream();
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out))) {
                indexBytes = out;
                bundleIndexLines.forEach((l) -> {
                    try {
                        writer.append(l).append('\n');
                    } catch (final IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                });
            }
            writeIndexFile(indexBytes.toByteArray(), context);

            if (isJarType()) {
                writeGraalResourceConfig(resources, context);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jos != null) {
                try {
                    jos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        //after closing
        if (isJarType()) {
            context.addFile(substrateJar, FileType.INDEX_JAR);
        } else {
            context.addFile(config.indexOutputDirectory(), FileType.INDEX_DIR);

        }
    }

    @Override
    public void preJars(Context context) {

        Path indexOutputDirectory = config.indexOutputDirectory();
        if (!indexOutputDirectory.toFile().isDirectory()) {
            throw new IllegalArgumentException(
                    "Output file must be a directory." + indexOutputDirectory);
        }
        if (!indexOutputDirectory.toFile().exists()) {
            try {
                Files.createDirectories(indexOutputDirectory);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (isJarType()) {
            substrateJar = indexOutputDirectory.resolve(ATOMOS_SUBSTRATE_JAR);
            final Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            try {
                jos = new JarOutputStream(new FileOutputStream(substrateJar.toFile()),
                        manifest);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        counter = new AtomicLong(0);
        indexInfos = new ArrayList<>();
        uniquePaths = new HashMap<>();
    }

    private void writeGraalResourceConfig(Collection<String> resources, Context context)
            throws IOException {
        resources.add(ATOMOS_CATH_ALL); // This alone could be enough,

        DefaultResourceConfiguration rci = new DefaultResourceConfiguration();
        rci.addResourcePattern(resources);
        context.addResourceConfig(rci);

        final String graalResConfJson = ResourceJsonUtil.json(rci);

        final JarEntry graalResConfEntry = new JarEntry(
                "META-INF/native-image/resource-config.json");
        jos.putNextEntry(graalResConfEntry);
        jos.write(graalResConfJson.getBytes());
        jos.flush();

    }

    private void writeIndexFile(final byte[] bytes, Context context) throws IOException {
        if (isJarType()) {
            final JarEntry atomosIndexEntry = new JarEntry(ATOMOS_BUNDLES_INDEX);
            jos.putNextEntry(atomosIndexEntry);
            jos.write(bytes);
        } else {
            Files.write(config.indexOutputDirectory().resolve(ATOMOS_BUNDLES_INDEX),
                    bytes);
        }
        DefaultResourceConfiguration resourceConfig = new DefaultResourceConfiguration();
        resourceConfig.addResourcePattern(ATOMOS_BUNDLES_INDEX);
        context.addResourceConfig(resourceConfig);
    }
}
