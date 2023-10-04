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

import org.apache.felix.atomos.Atomos;
import org.apache.felix.atomos.AtomosContent;
import org.apache.sling.feature.launcher.impl.launchers.FrameworkRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.startlevel.BundleStartLevel;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class AtomosRunner extends FrameworkRunner {
    private final Atomos m_atomos = Atomos.newAtomos();
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
        return new AtomosFrameworkFactory(m_atomos);
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
        final BundleContext bc = framework.getBundleContext();
        int defaultStartLevel = getProperty(bc, "felix.startlevel.bundle", 1);
        for (final Integer startLevel : sortStartLevels(bundleMap.keySet(), defaultStartLevel)) {
            logger.debug("Installing bundles with start level {}", startLevel);

            for (final URL file : bundleMap.get(startLevel)) {
                logger.debug("- {}", file);

                final URLStreamHandler dummyHandler = new URLStreamHandler() {
                    @Override
                    protected URLConnection openConnection(URL u) throws IOException {
                        return null;
                    }
                };
                AtomosContent content = m_atomos
                        .getBootLayer()
                        .getAtomosContents().stream()
                        .filter(atomosContent -> {
                            try {
                                return new URL(new URL(null, "missing:", dummyHandler), atomosContent.getAtomosLocation(), dummyHandler).getPath().endsWith(file.getPath().substring(file.getPath().lastIndexOf('/')));
                            } catch (MalformedURLException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .findFirst().orElseThrow(() -> new IllegalStateException("Unable to find " + file.getPath() + " on the classpath!"));

                final Bundle bundle = content.install();

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
}
