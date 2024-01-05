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

import org.apache.sling.testing.osgi.unit.OSGiSupport;
import org.apache.sling.testing.osgi.unit.Service;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.condition.Condition;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.sling.testing.osgi.unit.OSGiSupport.Enablement.ENABLED;
import static org.apache.sling.testing.osgi.unit.OSGiSupport.LogService.NONE;
import static org.apache.sling.testing.osgi.unit.OSGiSupport.LogService.SLING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.osgi.framework.wiring.BundleWiring.LISTRESOURCES_LOCAL;

@OSGiSupport(verbose = ENABLED)
class OSGiSupportTest {

    // This needs to be a copy, because OSGiSupport.LogService is not
    // available when running in an OSGi env. Otherwise, the list could
    // be retrieved via OSGiSupport.LogService.SLING.getRequiredBundles()
    public static final String[] SLING_LOG_SERVICE_BUNDLES = {
            "org.apache.felix.log",
            "org.apache.sling.commons.log",
            "org.apache.sling.commons.logservice"};

    public static final String[] BNDLIB_TRANSITIVE_DEPENDENCIES = {
            "biz.aQute.bndlib",
            "biz.aQute.bnd.util",
            "org.osgi.service.repository",
            "slf4j.api",
            "ch.qos.logback.classic",
            "ch.qos.logback.core",
            "org.apache.felix.log"};

    @Test
    void bundleContainsTestClass(Bundle bundle) {
        final BundleWiring wiring = bundle.adapt(BundleWiring.class);
        final String packagePath = OSGiSupportTest.class.getPackageName().replace('.', '/') + '/';
        final Collection<String> resources = wiring.listResources(packagePath, null, LISTRESOURCES_LOCAL);
        assertThat(resources).anyMatch(path -> path.contains(OSGiSupportTest.class.getSimpleName().replace('.', '/')));
    }

    @Test
    void testClassIsLoadedWithBundleClassLoader(Bundle bundle) {
        assertSame(FrameworkUtil.getBundle(getClass()), bundle);
        assertSame(getClass().getClassLoader(), bundle.adapt(BundleWiring.class).getClassLoader());
    }

    @Test
    void frameworkIsBackedByTheSystemClassLoader(Framework framework) {
        assertSame(ClassLoader.getSystemClassLoader(), framework.adapt(BundleWiring.class).getClassLoader());
    }

    @Test
    @OSGiSupport(logService = SLING)
    void slingLogServiceBundlesArePresent(BundleContext bc) {
        assertThat(getSymbolicNames(bc.getBundles())).containsOnlyOnce(SLING_LOG_SERVICE_BUNDLES);
    }

    @Test
    @OSGiSupport(logService = NONE)
    void slingLogServiceBundlesAreNotPresent(BundleContext bc) {
        assertThat(getSymbolicNames(bc.getBundles())).doesNotContain(SLING_LOG_SERVICE_BUNDLES);
    }

    @Test
    @OSGiSupport(logService = NONE, additionalBundles = {"biz.aQute.bndlib"})
    void additionalBundleIsInstalledAndPullsInTransitiveDependencies(BundleContext bc) {
        assertThat(getSymbolicNames(bc.getBundles())).contains(BNDLIB_TRANSITIVE_DEPENDENCIES);
    }

    @Test
    @OSGiSupport(logService = NONE, additionalBundles = {"slf4j.simple"})
    void additionalFragmentIsHandled(BundleContext bc) {
        final Bundle slf4jSimple = Stream.of(bc.getBundles())
                .filter(b -> Objects.equals("slf4j.simple", b.getSymbolicName()))
                .findFirst()
                .orElse(null);
        assertThat(slf4jSimple)
                .isNotNull()
                .matches(OSGiUtil::isFragment, "is fragment");
    }

    @Test
    @OSGiSupport(logService = NONE)
    void noAdditionalBundleIsInstalled(BundleContext bc) {
        assertThat(getSymbolicNames(bc.getBundles())).doesNotContain(BNDLIB_TRANSITIVE_DEPENDENCIES);
    }

    @Component(service = ServiceA.class)
    public static class ServiceA {}

    @Test
    @OSGiSupport(logService = SLING, additionalBundles = {"org.apache.felix.scr"})
    void serviceInjection(Framework framework, @Service ServiceComponentRuntime scr, @Service ServiceA serviceA) {
        assertNotNull(framework);
        assertNotNull(scr);
        assertNotNull(serviceA);
    }

    @Test
    void filteredServiceInjection(@Service(filter = "(osgi.condition.id=true)") Condition condition) {
        assertNotNull(condition);
    }

    @Test
    void serviceUnavailableInjection(@Service String string) {
        assertNull(string);
    }

    @Test
    void filteredServiceUnavailableInjection(@Service(filter = "(osgi.condition.id=unavailable)") Condition condition) {
        assertNull(condition);
    }

    @Test
    void serviceIterableInjection(@Service Iterable<Condition> conditions, @Service(filter = "(osgi.condition.id=true)") Condition control) {
        assertThat(conditions)
                .allMatch(Condition.class::isInstance)
                .contains(control);
    }

    @Test
    void serviceCollectionInjection(@Service Collection<Condition> conditions, @Service(filter = "(osgi.condition.id=true)") Condition control) {
        assertThat(conditions)
                .allMatch(Condition.class::isInstance)
                .contains(control);
    }

    @Test
    void serviceListInjection(@Service List<Condition> conditions, @Service(filter = "(osgi.condition.id=true)") Condition control) {
        assertThat(conditions)
                .allMatch(Condition.class::isInstance)
                .contains(control);
    }

    @Test
    void serviceArrayInjection(@Service Condition[] conditions, @Service(filter = "(osgi.condition.id=true)") Condition control) {
        assertThat(conditions)
                .allMatch(Condition.class::isInstance)
                .contains(control);
    }

    @Test
    void ignoreUnsupportedInjections(Framework framework, @TempDir Path path, Bundle bundle, @TempDir File file, @Service Condition condition) {
        assertNotNull(framework);
        assertNotNull(path);
        assertNotNull(bundle);
        assertNotNull(file);
        assertNotNull(condition);
    }

    @NotNull
    private static List<String> getSymbolicNames(Bundle... bundles) {
        return Stream.of(bundles)
                .map(Bundle::getSymbolicName)
                .collect(Collectors.toUnmodifiableList());
    }
}
