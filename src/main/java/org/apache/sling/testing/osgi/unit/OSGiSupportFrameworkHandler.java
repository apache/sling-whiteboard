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

import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.repository.fileset.FileSetRepository;
import biz.aQute.resolve.BndResolver;
import biz.aQute.resolve.ResolveProcess;
import biz.aQute.resolve.ResolverLogger;
import org.apache.sling.testing.osgi.unit.impl.BundleUtil;
import org.apache.sling.testing.osgi.unit.impl.OSGiUnitConfig;
import org.apache.sling.testing.osgi.unit.impl.OSGiUtil;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.Resolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.function.Predicate.not;
import static org.apache.sling.testing.osgi.unit.impl.BundleUtil.collectJarFilesFromClassPath;
import static org.osgi.framework.Constants.FRAMEWORK_STORAGE;

class OSGiSupportFrameworkHandler implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

    private static final Logger LOG = LoggerFactory.getLogger(OSGiSupportFrameworkHandler.class);

    private static final ExtensionContext.Namespace namespace = ExtensionContext.Namespace.create(OSGiSupportInvocationInterceptor.class);

    static ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(namespace);
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        final Class<?> testClass = context.getRequiredTestClass();
        final Path storageDir = Files.createTempDirectory("osgi-test-framework");
        final List<Path> classPath = collectJarFilesFromClassPath(getClass().getClassLoader());
        final List<Path> sourcePath = Stream.of(System.getProperty("java.class.path").split(":"))
                .filter(p -> !p.endsWith(".jar"))
                .map(Path::of)
                .collect(Collectors.toUnmodifiableList());

        final Method testMethod = context.getRequiredTestMethod();
        final String bundleSymbolicName = "org.apache.sling.testing.osgi-unit.generated-" + testClass.getSimpleName() + '#' + testMethod.getName();

        final OSGiUnitConfig config = Stream.of(context.getTestClass(), context.getTestMethod())
                .map(annotatedElement -> AnnotationSupport.findAnnotation(annotatedElement, OSGiSupport.class))
                .flatMap(Optional::stream)
                .reduce(OSGiUnitConfig.withDefaults(testClass), OSGiUnitConfig::merge, (a, b) -> a);

        final Path testBundleJar = BundleUtil.buildTestBundle(
                config, storageDir, bundleSymbolicName, sourcePath, classPath);

        // TODO - resolutions could be cached
        final Map<Resource, List<Wire>> resolutions = resolveBundlesOnClasspath(
                testClass.getClassLoader(),
                osgiIdentities(bundleSymbolicName, config.getLogServiceBundles(), config.getAdditionalBundles()),
                testBundleJar);

        final Map<String, String> frameworkProperties = Map.of(
                FRAMEWORK_STORAGE, Files.createDirectories(storageDir).toAbsolutePath().toString());

        ServiceLoader<FrameworkFactory> frameworkFactories = ServiceLoader.load(FrameworkFactory.class);
        final FrameworkFactory factory = frameworkFactories.iterator().next();
        final Framework framework = factory.newFramework(frameworkProperties);
        framework.init();
        framework.start();

        for (Resource resource : resolutions.keySet()) {
            // fragments are not added to the resolutions map by the resolver,
            // they need to be retrieved from the wire of a host bundle
            final List<Resource> fragments = resolutions.get(resource).stream()
                    .filter(w -> Objects.equals(w.getRequirement().getNamespace(), "osgi.wiring.host"))
                    .map(Wire::getRequirer)
                    .filter(not(resolutions.keySet()::contains)) // eliminate duplicate fragments
                    .collect(Collectors.toUnmodifiableList());

            for (Resource fragment : fragments) {
                installBundle(framework, fragment);
            }

            // If a bundle is added to the resolution because it was added
            // as an additionalBundle (i.e. via an initial "osgi.identity"
            // requirement), we must not start it.
            // Implicitly added fragments are handled above.
            installBundle(framework, resource)
                    .filter(not(OSGiUtil::isFragment))
                    .ifPresent(bundle -> {
                        try {
                            bundle.start();
                        } catch (BundleException e) {
                            throw new IllegalStateException(e);
                        }
                    });
        }

        final Bundle testBundle = Stream.of(framework.getBundleContext().getBundles())
                .filter(b -> Objects.equals(b.getSymbolicName(), bundleSymbolicName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Bundle " + bundleSymbolicName + " was not installed."));

        final FrameworkStartLevel frameworkStartLevel = framework.adapt(FrameworkStartLevel.class);
        final CountDownLatch waitForStartup = new CountDownLatch(1);
        frameworkStartLevel.setStartLevel(5, event -> {
            if (frameworkStartLevel.getStartLevel() == 5) {
                waitForStartup.countDown();
            }
        });
        waitForStartup.await();

        getStore(context).put(Framework.class, framework);
        getStore(context).put(Bundle.class, testBundle);
        getStore(context).put(BundleContext.class, testBundle.getBundleContext());
    }

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        final ExtensionContext.Store store = getStore(context);
        Optional.ofNullable(store.remove(Closeables.class, Closeables.class))
                .ifPresent(Closeables::closeAll);
        store.remove(BundleContext.class);
        store.remove(Bundle.class);
        final Framework framework = store.remove(Framework.class, Framework.class);
        if (framework != null) {
            framework.stop();
            framework.waitForStop(1000);
        }
    }

    private static Map<Resource, List<Wire>> resolveBundlesOnClasspath(ClassLoader classLoader, String runRequires, Path... testBundles) throws Exception {
        try (final ResolverLogger logger = new ResolverLogger(); final Processor processor = new Processor()) {
            final List<Path> jarFiles = collectJarFilesFromClassPath(classLoader);

            final Runtime.Version version = Runtime.version();
            processor.setRunee("JavaSE-" + version.feature()); // TODO - is this the correct way to determine the version?
            processor.setRunfw("org.apache.felix.framework"); // TODO - dynamically support felix, eclipse and ???
            processor.setRunRequires(runRequires);

            processor.addBasicPlugin(new FileSetRepository("testBundles", pathsToFiles(asList(testBundles))));
            processor.addBasicPlugin(new FileSetRepository("jarsFromClassPath", pathsToFiles(jarFiles)));

            ResolveProcess resolve = new ResolveProcess();
            Resolver resolver = new BndResolver(logger);
            final Map<Resource, List<Wire>> resolution = resolve.resolveRequired(processor, null, processor, resolver, Collections.emptySet(), logger);
            return resolution;
        } catch (ResolutionException e) {
            // TODO - revisit error reporting
//            if (e.getCause() instanceof ReasonException) {
//                ReasonException reason = (ReasonException) e.getCause();
//                while (reason.getCause() instanceof ReasonException) {
//                    reason = (ReasonException) reason.getCause();
//                }
//                // reason.addSuppressed(e);
//                throw reason;
//            }
            throw e;
        }
    }

    private static List<File> pathsToFiles(List<Path> jarFiles) {
        return jarFiles.stream()
                .map(Path::toFile)
                .collect(Collectors.toUnmodifiableList());
    }

    private static String osgiIdentities(String bundleSymbolicName, Collection<String> loggerBundles, Collection<String> additionalBundles) {
        return Stream.of(Stream.of(bundleSymbolicName), loggerBundles.stream(), additionalBundles.stream())
                .flatMap(Function.identity())
                .map(bsn -> String.format("osgi.identity;filter:='(osgi.identity=%s)'", bsn))
                .collect(Collectors.joining(","));
    }

    private static Optional<Bundle> installBundle(Framework framework, Resource resource) throws IOException, BundleException {
        return ResourceUtils.getURI(resource)
                .map(uri -> {
                    try (InputStream bundleStream = Files.newInputStream(Path.of(uri))) {
                        final Bundle bundle = framework.getBundleContext().installBundle(uri.toString(), bundleStream);
                        bundle.adapt(BundleStartLevel.class).setStartLevel(5);
                        LOG.info("Installed bundle '{}-{}'", bundle.getSymbolicName(), bundle.getVersion());
                        return bundle;
                    } catch (IOException | BundleException e) {
                        throw new IllegalStateException(e);
                    }
                });
    }
}
