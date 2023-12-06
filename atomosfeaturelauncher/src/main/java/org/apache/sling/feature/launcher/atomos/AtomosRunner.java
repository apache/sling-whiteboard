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
package org.apache.sling.feature.launcher.atomos;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import org.apache.felix.atomos.Atomos;
import org.apache.felix.atomos.AtomosContent;
import org.apache.felix.atomos.impl.base.AtomosBase;
import org.apache.sling.feature.launcher.impl.launchers.FrameworkRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.connect.ConnectContent;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.startlevel.BundleStartLevel;

public class AtomosRunner extends FrameworkRunner {

    private static final ConcurrentHashMap<String, ClassLoader> location2Loader = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Bundle> classToBundle = new ConcurrentHashMap<>();
    private static Atomos m_atomos;
    private BiConsumer<URL, Map<String, String>> bundleReporter;

    public AtomosRunner(Map<String, String> frameworkProperties, Map<Integer, List<URL>> bundlesMap, List<Object[]> configurations, List<URL> installables) throws Exception {
        super(frameworkProperties, bundlesMap, configurations, installables);
    }

    private static Iterable<Integer> sortStartLevels(final Collection<Integer> startLevels, final int defaultStartLevel) {
        final List<Integer> result = new ArrayList<>(startLevels);
        Collections.sort(result, (o1, o2) -> {
            int i1 = o1 == 0 ? defaultStartLevel : o1;
            int i2 = o2 == 0 ? defaultStartLevel : o2;
            return Integer.compare(i1, i2);
        });
        return result;
    }

    private synchronized static Atomos getAtomos() {
        if (m_atomos == null) {
            m_atomos = Atomos.newAtomos();
        }
        return m_atomos;
    }

    private static int getProperty(BundleContext bc, String propName, int defaultValue) {
        String val = bc.getProperty(propName);
        if (val == null) {
            return defaultValue;
        } else {
            return Integer.parseInt(val);
        }
    }

    @Override
    protected FrameworkFactory getFrameworkFactory() throws Exception {
        return new AtomosFrameworkFactory(getAtomos());
    }

    @Override
    protected void setupFramework(Framework framework, Map<Integer, List<URL>> bundlesMap) throws BundleException {
        super.setupFramework(framework, Collections.emptyMap());
        this.install(framework, bundlesMap);
    }

    @Override
    public void setBundleReporter(final BiConsumer<URL, Map<String, String>> reporter) {
        this.bundleReporter = reporter;
        super.setBundleReporter(reporter);
    }

    private boolean isSystemBundleFragment(final Bundle installedBundle) {
        final String fragmentHeader = getFragmentHostHeader(installedBundle);
        return fragmentHeader != null
                && fragmentHeader.indexOf(Constants.EXTENSION_DIRECTIVE) > 0;
    }

    /**
     * Gets the bundle's Fragment-Host header.
     */
    private String getFragmentHostHeader(final Bundle b) {
        return b.getHeaders().get(Constants.FRAGMENT_HOST);
    }

    private void install(final Framework framework, final Map<Integer, List<URL>> bundleMap) throws BundleException {
        System.out.println(System.getProperty("java.specification.version"));
        final BundleContext bc = framework.getBundleContext();
        System.out.println(bc.getBundle(0).getHeaders());
        int defaultStartLevel = getProperty(bc, "felix.startlevel.bundle", 1);

        System.out.println(new File(".").getAbsolutePath());
        System.out.println(new File(new File("."), "content").getAbsolutePath());

        /*for (File child : new File(new File("."), "content").listFiles()) {
            System.out.println(child.getAbsolutePath());
        }*/
        /*
        if (1 == (2-1)) {
            throw new RuntimeException("Yaaaa!!");
        }
         */
        System.out.println("+++ About to process");

        for (final Integer startLevel : sortStartLevels(bundleMap.keySet(), defaultStartLevel)) {
            System.out.println("*** SL:" + startLevel);
            logger.debug("Installing bundles with start level {}", startLevel);

            for (final URL file : bundleMap.get(startLevel)) {
                logger.debug("- {}", file);

                final URLStreamHandler dummyHandler = new URLStreamHandler() {
                    @Override
                    protected URLConnection openConnection(URL u) throws IOException {
                        return null;
                    }
                };

                System.out.println("%%% URL:" + file);
                AtomosContent content = getAtomos()
                        .getBootLayer()
                        .getAtomosContents().stream()
                        .filter(atomosContent -> {
                            System.out.println("$$$" + atomosContent);
                            try {
                                if (atomosContent instanceof AtomosBase.AtomosLayerBase.AtomosContentIndexed) {
                                    System.out.println("??? instance of AtomosBase.AtomosLayerBase.AtomosContentIndexed: " + atomosContent);
                                } else {
                                    System.out.println("!!! not instance of AtomosBase.AtomosLayerBase.AtomosContentIndexed: " + atomosContent);
                                }

                                if (atomosContent instanceof  AtomosBase.AtomosLayerBase.AtomosContentIndexed) {
                                    ConnectContent.ConnectEntry fileLocation = atomosContent.getConnectContent().getEntry("META-INF/atomos/file.location").orElse(null);
                                    if (fileLocation != null) {
                                        String fileName = file.getPath().substring(file.getPath().lastIndexOf('/') + 1);
                                        System.out.println("***" + fileName);
                                        System.out.println("###" + new String(fileLocation.getBytes()));
                                        return new String(fileLocation.getBytes()).endsWith(fileName);
                                    } else {
                                        return false;
                                    }
                                } else {
                                    return new URL(new URL(null, "missing:", dummyHandler), atomosContent.getAtomosLocation(), dummyHandler).getPath().endsWith(file.getPath().substring(file.getPath().lastIndexOf('/')));
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .findFirst().orElseThrow(() -> new IllegalStateException("Unable to find " + file.getPath() + " on the classpath!"));

                final Bundle bundle = content.install();
                System.out.println(bundle.getBundleId() + " " + bundle.getLocation());

                // fragment?
                if (!isSystemBundleFragment(bundle) && getFragmentHostHeader(bundle) == null) {
                    if (startLevel > 0) {
                        bundle.adapt(BundleStartLevel.class).setStartLevel(startLevel);
                    }
                    bundle.start();
                }

                if (this.bundleReporter != null) {
                    final Map<String, String> params = new HashMap<>();
                    params.put(Constants.BUNDLE_SYMBOLICNAME, bundle.getSymbolicName());
                    params.put(Constants.BUNDLE_VERSION, bundle.getVersion().toString());
                    params.put("Bundle-Id", String.valueOf(bundle.getBundleId()));

                    this.bundleReporter.accept(file, params);
                }
            }
        }
    }

    private static Class<?> elementType(Class origin) {
        if (!origin.isArray()) return null;

        Class<?> c = origin;
        while (c.isArray()) {
            c = c.getComponentType();
        }
        return c;
    }
    private static String resolveName(Class origin, String name) {
        if (!name.startsWith("/")) {
            Class<?> c = origin.isArray() ? elementType(origin) : origin;
            String baseName = c.getPackageName();
            if (baseName != null && !baseName.isEmpty()) {
                name = baseName.replace('.', '/') + "/" + name;
            }
        } else {
            name = name.substring(1);
        }
        return name;
    }
    public static URL getAtomosLoaderResourceWrapped(Class origin, String resource) {
        return getAtomosLoaderWrapped(origin).getResource(resolveName(origin, resource));
    }

    public static InputStream getAtomosLoaderStreamWrapped(Class origin, String resource) {
        // return getAtomosLoaderWrapped(origin).getResourceAsStream(resolveName(origin, resource));
        URL u = getAtomosLoaderWrapped(origin).getResource(resolveName(origin, resource));
        if (u == null) {
            System.out.println("!!! getResourceAsStream does not find it: " + resource);
            return null;
        }

        try {
            System.out.println("!!! getResourceAsStream: " + u);
            return u.openStream();
        } catch (IOException e) {
            System.out.println("!!! getResourceAsStream exception " + e.getMessage());
            return null;
        }
    }

    public static ClassLoader getAtomosLoaderWrapped(Class origin) {
        System.out.println("TRAP: " + origin.getName());
        if (origin.isInterface()) {
            return origin.getClassLoader();
        }
        Bundle bundle = classToBundle.computeIfAbsent(origin.getName(), (name) -> {
            try {
                return FrameworkUtil.getBundle(origin);
            } catch (Throwable ex) {
                return null;
            }});

        ClassLoader cl = Optional.ofNullable(bundle).map(b ->
                    location2Loader.computeIfAbsent(b.getLocation(), location -> new BundleClassLoader(origin, bundle) )
                ).orElseGet(origin::getClassLoader);
        System.out.println(cl + "-" + bundle);

        if (bundle != null) {
            if ("org.apache.felix.webconsole.plugins.event".equals(bundle.getSymbolicName())) {
                System.out.println("!!! entry: " + bundle.getEntry("/res/events.html"));
                System.out.println("!!! cl.getResource: " + cl.getResource("/res/events.html"));
                URL url = cl.getResource("/res/events.html");
                if (url != null) {
                    try (InputStream is = url.openStream()) {
                        System.out.println("Was able to open stream");
                        byte[] bytes = is.readAllBytes();
                        String s = new String(bytes);
                        System.out.println("Bytes: " + s.substring(0, 256));
                    } catch (Exception ex) {
                        System.out.println("!!! unable to open stream");
                        ex.printStackTrace();
                    }
                }

                // Enumeration<URL> e = bundle.findEntries("/", "*", true);
                // System.out.println("!!! Found entries: " + Collections.list(e));
            }
        }

        return cl;
    }

    private static class BundleClassLoader extends ClassLoader implements BundleReference {
        private final Class origin;
        private final Bundle bundle;
        BundleClassLoader(Class origin, Bundle bundle) {
            this.origin = origin;
            this.bundle = bundle;
        }
        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            return origin.getClassLoader().loadClass(name);
        }

        @Override
        public URL getResource(String name) {
            return bundle.getEntry(name);
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            int idx = name.lastIndexOf("/");
            String path = "/";
            if (idx > 0) {
                path = name.substring(0, idx);
                name = name.substring(idx + 1);
            }
            Enumeration<URL> result = bundle.findEntries(path, name, false);
            return result != null ? result : Collections.emptyEnumeration();
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            System.out.println("Bundle:" + bundle + " origin:" + origin + " ResolveName: " + resolveName(origin, name));
            URL u = getResource(name);
            if (u == null) {
                System.out.println("~~~ getResourceAsStream does not find it: " + name);
                return null;
            }

            try {
                System.out.println("~~~ getResourceAsStream: " + u);
                return u.openStream();
            } catch (IOException e) {
                System.out.println("~~~ getResourceAsStream exception " + e.getMessage());
                return null;
            }
        }

        @Override
        public Bundle getBundle() {
            return bundle;
        }
    }
}
