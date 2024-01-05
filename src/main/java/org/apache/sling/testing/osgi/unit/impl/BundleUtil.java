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
package org.apache.sling.testing.osgi.unit.impl;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Descriptors;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.bnd.stream.MapStream;
import org.apache.sling.testing.osgi.unit.OSGiSupport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class BundleUtil {

    private static final Logger LOG = LoggerFactory.getLogger(BundleUtil.class);

    public static final String COMPONENT_ANNOTATION = Descriptors.fqnToBinary("org.osgi.service.component.annotations.Component");

    public static Path buildTestBundle(OSGiUnitConfig osgiUnitConfig, Path outputDir, String bundleSymbolicName, List<Path> sourcePaths, List<Path> classPath) throws Exception {

        final Map<String, String> instructions = Map.of(
                Constants.BUNDLE_SYMBOLICNAME, bundleSymbolicName,
                Constants.EXPORT_PACKAGE, "!*",
                Constants.IMPORT_PACKAGE, "!org.apache.sling.testing.osgi.unit,*"
        );
        final Properties props = new Properties();
        props.putAll(instructions);

        try (Jar jar = populateMinimalJar(osgiUnitConfig, sourcePaths); Builder builder = new Builder()) {

            builder.setJar(jar);
            builder.setProperties(props);

            builder.setBase(outputDir.toFile());
            builder.addClasspath(classPath.stream()
                    .map(Path::toFile)
                    .collect(Collectors.toUnmodifiableSet()));

            builder.build();

            final Path bundlePath = outputDir.resolve(bundleSymbolicName.replaceAll("[^\\w._]", "-") + ".jar");
            try (OutputStream outputStream = Files.newOutputStream(bundlePath)) {
                jar.write(outputStream);
            }
            if (osgiUnitConfig.isVerbose()) {
                LOG.info("Bundle {} contents:", bundleSymbolicName);
                try (ZipInputStream zipStream = new ZipInputStream(Files.newInputStream(bundlePath))) {
                    ZipEntry entry;
                    while ((entry = zipStream.getNextEntry()) != null) {
                        if (!entry.getName().endsWith("/")) {
                            LOG.info("\u251C {}", entry.getName());
                        }
                    }
                }
            }
            return bundlePath;
        }
    }

    private static Jar populateMinimalJar(OSGiUnitConfig osgiUnitConfig, List<Path> sourcePaths) throws Exception {

        final Jar jar = new Jar("minimal");
        final Analyzer analyzer = new Analyzer(jar);
        analyzer.addClasspath(sourcePaths.stream().map(Path::toFile).collect(Collectors.toUnmodifiableList()));

        Map<String, Clazz> clazzes = parseClazzes(analyzer);

        for (Class<?> clazz : osgiUnitConfig.getClasses()) {
            putClassAndReferences(analyzer, clazzes, clazz.getName(), osgiUnitConfig.isVerbose());
        }
        // TODO - add logging to recommend classes to add
        // addDSComponents(analyzer, clazzes);
        return jar;
    }

//    private static void addDSComponents(Analyzer analyzer, Map<String, Clazz> clazzes, boolean verboseLogging) {
//        try (MapStream<String, Clazz> clazzesStream = MapStream.of(clazzes)) {
//            final Map<String, Resource> resources = analyzer.getJar().getResources();
//            clazzesStream
//                    .filterValue(clazz -> clazz.annotations(COMPONENT_ANNOTATION).findAny().isPresent())
//                    .mapValue(clazz -> Stream.concat(
//                                    streamOfNullableArray(clazz.getInterfaces()),
//                                    clazz.annotations(COMPONENT_ANNOTATION)
//                                            .flatMap(a -> streamOfNullableArray(a.get("service"))
//                                                    .filter(Descriptors.TypeRef.class::isInstance)
//                                                    .map(Descriptors.TypeRef.class::cast)))
//                            .map(Descriptors.TypeRef::getFQN)
//                            .collect(Collectors.toUnmodifiableSet())
//                    )
//                    .forEach((fqn, services) -> {
//                        if (services.isEmpty() || services.stream().map(Descriptors::fqnToPath).anyMatch(resources::containsKey)) {
//                            try {
//                                putClassAndReferences(analyzer, clazzes, fqn, verboseLogging);
//                            } catch (Exception e) {
//                                throw new IllegalStateException(e);
//                            }
//                        }
//                    }
//            );
//        }
//    }

//    @NotNull
//    private static <T> Stream<@NotNull T> streamOfNullableArray(T @Nullable [] object) {
//        return Optional.ofNullable(object).stream()
//                .flatMap(Stream::of)
//                .filter(Objects::nonNull);
//    }

    @Nullable
    private static Resource findResource(List<Jar> classpath, String path) {
        return classpath.stream()
                .map(jar -> jar.getResource(path))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private static Map<String, Clazz> parseClazzes(Analyzer analyzer) {
        try (MapStream<String, Resource> pathToResource = analyzer.getClasspath().stream()
                .map(Jar::getResources)
                .map(MapStream::of)
                .reduce(MapStream::concat)
                .orElseGet(MapStream::empty)) {
            return pathToResource
                    .filterKey(path -> path.endsWith(".class"))
                    .map((path, resource) -> {
                        try {
                            final Clazz clazz = new Clazz(analyzer, path, resource);
                            clazz.parseClassFile();
                            return Map.entry(Descriptors.pathToFqn(path), clazz);
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .collect(MapStream.toMap());
        }
    }

    private static void putClassAndReferences(Analyzer analyzer, Map<String, Clazz> clazzes, String fqn, boolean verboseLogging, String... via) throws Exception {
        final Jar jar = analyzer.getJar();
        String relativePath = Descriptors.fqnToPath(fqn);
        if (jar.getResource(relativePath) == null && !isOsgiUnitAnnotation(fqn)) {
            final Resource resource = findResource(analyzer.getClasspath(), relativePath);
            if (resource == null) {
                return;
            }
            jar.putResource(relativePath, copyResource(resource));
            if (verboseLogging) {
                if (via.length == 0) {
                    LOG.info("Added class {}", fqn);
                } else {
                    LOG.info("Added class {} via {}", fqn, via[0]);
                }
            }
            final Clazz clazz = clazzes.get(fqn);
            final Set<Descriptors.TypeRef> typeRefs = clazz.parseClassFile();
            for (Descriptors.TypeRef typeRef : typeRefs) {
                final Descriptors.TypeRef ref = typeRef.isArray() ? typeRef.getComponentTypeRef() : typeRef;
                if (Objects.equals(fqn, ref.getFQN())) {
                    continue;
                }

                putClassAndReferences(analyzer, clazzes, ref.getFQN(), verboseLogging, fqn);
            }
        }
    }

    private static boolean isOsgiUnitAnnotation(String fqn) {
        if (Descriptors.getPackage(fqn).startsWith(OSGiSupport.class.getPackageName())) {
            try {
                Class<?> clazz = OSGiSupport.class.getClassLoader().loadClass(fqn);
                while (clazz.getEnclosingClass() != null) {
                    clazz = clazz.getEnclosingClass();
                }
                return clazz.isAnnotation();
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
        return false;
    }

    private static Resource copyResource(Resource resource) throws Exception {
        return new FileResource(resource);
    }

    @NotNull
    public static List<Path> collectJarFilesFromClassPath(ClassLoader classLoader) throws IOException {
        final Enumeration<URL> jars = classLoader.getResources("META-INF/MANIFEST.MF");
        final Iterable<URL> iterable = jars::asIterator;
        return StreamSupport.stream(iterable.spliterator(), false)
                .map(url -> {
                    try {
                        final URLConnection urlConnection = url.openConnection();
                        if (urlConnection instanceof JarURLConnection) {
                            final String filePath = ((JarURLConnection) urlConnection).getJarFile().getName();
                            return new File(filePath).toPath();
                        }
                        return null;
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableList());
    }

    private BundleUtil() {}
}
