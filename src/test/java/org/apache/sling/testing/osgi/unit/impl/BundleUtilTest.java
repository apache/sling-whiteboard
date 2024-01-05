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

import aQute.bnd.osgi.Descriptors;
import org.apache.felix.utils.manifest.Clause;
import org.apache.sling.testing.osgi.unit.OSGiSupport;
import org.apache.sling.testing.osgi.unit.impl.samples.AbstractServiceAImpl;
import org.apache.sling.testing.osgi.unit.impl.samples.DSComponentNoService;
import org.apache.sling.testing.osgi.unit.impl.samples.OuterDSImpl;
import org.apache.sling.testing.osgi.unit.impl.samples.OuterImpl;
import org.apache.sling.testing.osgi.unit.impl.samples.OuterInterface;
import org.apache.sling.testing.osgi.unit.impl.samples.ServiceAImplReferencingOuterInterface;
import org.apache.sling.testing.osgi.unit.impl.samples.ServiceA;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.ObjectArrayAssert;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.osgi.service.component.annotations.Component;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

class BundleUtilTest {

    interface NestedInterface {}

    static class NestedImpl implements NestedInterface {}

    @Component(service = NestedInterface.class)
    static class NestedDSImpl implements NestedInterface {}

    @TempDir Path outputDir;

    @Test
    void buildTestBundleForNestedInterface() throws Exception {
        final Path bundleJar = createTestBundle(NestedInterface.class);

        final Attributes mainAttributes = getManifestAttributes(bundleJar);
        assertCommonHeaders(mainAttributes);
        assertPackageHeader(mainAttributes, "Import-Package")
                .noneMatch(clause -> Objects.equals(clause.getName(), "org.junit.jupiter.api"))
                .noneMatch(clause -> Objects.equals(clause.getName(), "org.assertj.core.api"));

        assertThat(getContainedFiles(bundleJar)).containsOnly(
                relativeClassFilename(NestedInterface.class)
        );
    }

    @Test
    void buildTestBundleForNestedClass() throws Exception {
        final Path bundleJar = createTestBundle(NestedImpl.class);

        final Attributes mainAttributes = getManifestAttributes(bundleJar);
        assertCommonHeaders(mainAttributes);
        assertPackageHeader(mainAttributes, "Import-Package")
                .noneMatch(clause -> Objects.equals(clause.getName(), "org.junit.jupiter.api"))
                .noneMatch(clause -> Objects.equals(clause.getName(), "org.assertj.core.api"));

        assertThat(getContainedFiles(bundleJar)).containsOnly(
                relativeClassFilename(NestedInterface.class),
                relativeClassFilename(NestedImpl.class)
        );
    }

    @Test
    void buildTestBundleForNestedDSService() throws Exception {
        final Path bundleJar = createTestBundle(NestedDSImpl.class);

        final Attributes mainAttributes = getManifestAttributes(bundleJar);
        assertCommonHeaders(mainAttributes);
        assertPackageHeader(mainAttributes, "Import-Package")
                .noneMatch(clause -> Objects.equals(clause.getName(), "org.junit.jupiter.api"))
                .noneMatch(clause -> Objects.equals(clause.getName(), "org.assertj.core.api"));

        assertThat(getContainedFiles(bundleJar)).containsOnly(
                relativeClassFilename(NestedInterface.class),
                relativeClassFilename(NestedDSImpl.class),
                dsComponentXmlFilename(NestedDSImpl.class)
        );
    }

    @Test
    void buildTestBundleForOuterInterface() throws Exception {
        final Path bundleJar = createTestBundle(OuterInterface.class);

        final Attributes mainAttributes = getManifestAttributes(bundleJar);
        assertCommonHeaders(mainAttributes);
        assertPackageHeader(mainAttributes, "Import-Package")
                .noneMatch(clause -> Objects.equals(clause.getName(), "org.junit.jupiter.api"))
                .noneMatch(clause -> Objects.equals(clause.getName(), "org.assertj.core.api"));

        assertThat(getContainedFiles(bundleJar)).containsOnly(
                relativeClassFilename(OuterInterface.class)
        );
    }

    @Test
    void buildTestBundleForOuterClass() throws Exception {
        final Path bundleJar = createTestBundle(OuterImpl.class);

        final Attributes mainAttributes = getManifestAttributes(bundleJar);
        assertCommonHeaders(mainAttributes);
        assertPackageHeader(mainAttributes, "Import-Package")
                .noneMatch(clause -> Objects.equals(clause.getName(), "org.junit.jupiter.api"))
                .noneMatch(clause -> Objects.equals(clause.getName(), "org.assertj.core.api"));

        assertThat(getContainedFiles(bundleJar)).containsOnly(
                relativeClassFilename(OuterInterface.class),
                relativeClassFilename(OuterImpl.class)
        );
    }

    @Test
    void buildTestBundleForOuterDSClass() throws Exception {
        final Path bundleJar = createTestBundle(OuterDSImpl.class);

        final Attributes mainAttributes = getManifestAttributes(bundleJar);
        assertCommonHeaders(mainAttributes);
        assertPackageHeader(mainAttributes, "Import-Package")
                .noneMatch(clause -> Objects.equals(clause.getName(), "org.junit.jupiter.api"))
                .noneMatch(clause -> Objects.equals(clause.getName(), "org.assertj.core.api"));

        assertThat(getContainedFiles(bundleJar)).containsOnly(
                relativeClassFilename(OuterInterface.class),
                relativeClassFilename(OuterDSImpl.class),
                dsComponentXmlFilename(OuterDSImpl.class)
        );
    }

    @Test
    void buildTestBundleForOuterDSClassAndNonServiceComponent() throws Exception {
        final Path bundleJar = createTestBundle(OuterDSImpl.class, DSComponentNoService.class);

        final Attributes mainAttributes = getManifestAttributes(bundleJar);
        assertCommonHeaders(mainAttributes);
        assertPackageHeader(mainAttributes, "Import-Package")
                .noneMatch(clause -> Objects.equals(clause.getName(), "org.junit.jupiter.api"))
                .noneMatch(clause -> Objects.equals(clause.getName(), "org.assertj.core.api"));

        assertThat(getContainedFiles(bundleJar)).containsOnly(
                relativeClassFilename(OuterInterface.class),
                relativeClassFilename(OuterDSImpl.class),
                dsComponentXmlFilename(OuterDSImpl.class),
                relativeClassFilename(DSComponentNoService.class),
                dsComponentXmlFilename(DSComponentNoService.class)
        );
    }

    @Test
    void buildTestBundleForServiceAImplReferencingOuterInterface() throws Exception {
        final Path bundleJar = createTestBundle(ServiceAImplReferencingOuterInterface.class);

        final Attributes mainAttributes = getManifestAttributes(bundleJar);
        assertCommonHeaders(mainAttributes);
        assertPackageHeader(mainAttributes, "Import-Package")
                .noneMatch(clause -> Objects.equals(clause.getName(), "org.junit.jupiter.api"))
                .noneMatch(clause -> Objects.equals(clause.getName(), "org.assertj.core.api"));

        assertThat(getContainedFiles(bundleJar)).containsOnly(
                relativeClassFilename(ServiceA.class),
                relativeClassFilename(AbstractServiceAImpl.class),
                relativeClassFilename(ServiceAImplReferencingOuterInterface.class),
                dsComponentXmlFilename(ServiceAImplReferencingOuterInterface.class),
                relativeClassFilename(OuterInterface.class)
                );
    }

    @Test
    void buildTestBundleForNonServiceDSComponent() throws Exception {
        final Path bundleJar = createTestBundle(DSComponentNoService.class);

        final Attributes mainAttributes = getManifestAttributes(bundleJar);
        assertCommonHeaders(mainAttributes);
        assertPackageHeader(mainAttributes, "Import-Package")
                .noneMatch(clause -> Objects.equals(clause.getName(), "org.junit.jupiter.api"))
                .noneMatch(clause -> Objects.equals(clause.getName(), "org.assertj.core.api"));

        assertThat(getContainedFiles(bundleJar)).containsOnly(
                relativeClassFilename(DSComponentNoService.class),
                dsComponentXmlFilename(DSComponentNoService.class)
        );
    }

    @Test
    void buildTestBundleForTestClass() throws Exception {
        final Path bundleJar = createTestBundle(BundleUtilTest.class);

        final Attributes mainAttributes = getManifestAttributes(bundleJar);
        assertCommonHeaders(mainAttributes);
        assertPackageHeader(mainAttributes, "Import-Package")
                .anyMatch(clause -> Objects.equals(clause.getName(), "org.junit.jupiter.api"))
                .anyMatch(clause -> Objects.equals(clause.getName(), "org.assertj.core.api"));


        assertThat(getContainedFiles(bundleJar)).containsOnly(
                relativeClassFilename(BundleUtil.class),
                relativeClassFilename(BundleUtilTest.class),
                relativeClassFilename(NestedInterface.class),
                relativeClassFilename(NestedImpl.class),
                relativeClassFilename(NestedDSImpl.class),
                dsComponentXmlFilename(NestedDSImpl.class),
                relativeClassFilename(OuterInterface.class),
                relativeClassFilename(OuterImpl.class),
                relativeClassFilename(OuterDSImpl.class),
                dsComponentXmlFilename(OuterDSImpl.class),
                relativeClassFilename(DSComponentNoService.class),
                dsComponentXmlFilename(DSComponentNoService.class),
                relativeClassFilename(ServiceAImplReferencingOuterInterface.class),
                dsComponentXmlFilename(ServiceAImplReferencingOuterInterface.class),
                relativeClassFilename(AbstractServiceAImpl.class),
                relativeClassFilename(ServiceA.class),
                relativeClassFilename(OSGiUnitConfig.class)
        );
    }

    @NotNull
    private Path createTestBundle(Class<?>... classes) throws Exception {
        return BundleUtil.buildTestBundle(
                new OSGiUnitConfig(true, OSGiSupport.LogService.NONE, List.of(), List.of(classes)),
                outputDir, "test-bundle",
                asList(getSourceDirectory(BundleUtil.class), getSourceDirectory(getClass())),
                BundleUtil.collectJarFilesFromClassPath(getClass().getClassLoader()));
    }

    private static void assertCommonHeaders(Attributes mainAttributes) {
        assertHeader(mainAttributes, "Manifest-Version").isEqualTo("1.0");
        assertHeader(mainAttributes, "Bundle-ManifestVersion").isEqualTo("2");
        assertHeader(mainAttributes, "Bundle-Name").isEqualTo("test-bundle");
        assertHeader(mainAttributes, "Bundle-SymbolicName").isEqualTo("test-bundle");
        assertHeader(mainAttributes, "Bundle-Version").isEqualTo("0");
        assertHeader(mainAttributes, "Export-Package").isNull();

        // no imports for the testing annotation etc
        assertPackageHeader(mainAttributes, "Import-Package")
                .noneMatch(clause -> Objects.equals(clause.getName(), "org.apache.sling.testing.osgi.unit"))
                .noneMatch(clause -> clause.getName().startsWith("org.apache.sling.testing.osgi.unit."));
        assertPackageHeader(mainAttributes, "Private-Package")
                .allMatch(clause ->
                        Objects.equals(clause.getName(), OSGiSupport.class.getPackageName())
                                || clause.getName().startsWith(OSGiSupport.class.getPackageName() + '.'));
        assertPackageHeader(mainAttributes, "Require-Capability")
                .anyMatch(clause ->
                        Objects.equals(clause.getName(), "osgi.ee")
                                && Objects.equals(
                                clause.getDirective("filter"),
                                String.format("(&(osgi.ee=JavaSE)(version=%s))", Runtime.version().feature())));
    }

    private static AbstractStringAssert<?> assertHeader(Attributes mainAttributes, String headerName) {
        return assertThat(mainAttributes.getValue(headerName)).as("Manifest header \"%s\"", headerName);
    }

    private static ObjectArrayAssert<Clause> assertPackageHeader(Attributes mainAttributes, String headerName) {
        String value = mainAttributes.getValue(headerName);
        return assertThat(org.apache.felix.utils.manifest.Parser.parseHeader(value))
                .as("Manifest header \"%s\"", headerName);
    }

    private static Attributes getManifestAttributes(Path bundleJar) throws IOException {
        try (JarInputStream jar = new JarInputStream(Files.newInputStream(bundleJar))) {
            return jar.getManifest().getMainAttributes();
        }
    }

    @NotNull
    private static List<String> getContainedFiles(Path bundleJar) throws IOException {
        try (JarInputStream jar = new JarInputStream(Files.newInputStream(bundleJar))) {
            final List<String> containedEntries = new ArrayList<>();
            JarEntry jarEntry;
            while ((jarEntry = jar.getNextJarEntry()) != null) {
                if (!jarEntry.getName().endsWith("/")) { // skip directories
                    containedEntries.add(jarEntry.getName());
                }
            }
            return containedEntries;
        }
    }

    private static String relativeClassFilename(Class<?> clazz) {
        return Descriptors.fqnToPath(clazz.getName());
    }

    private static String dsComponentXmlFilename(Class<?> clazz) {
        return String.format("OSGI-INF/%s.xml", clazz.getName());
    }

    @NotNull
    private static Path getSourceDirectory(Class<?> clazz) throws URISyntaxException {
        return Path.of(clazz.getProtectionDomain().getCodeSource().getLocation().toURI());
    }
}