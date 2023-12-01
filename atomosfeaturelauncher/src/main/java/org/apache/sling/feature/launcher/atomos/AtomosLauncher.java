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
import org.apache.felix.atomos.AtomosLayer;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.launcher.atomos.weaver.AtomosWeaver;
import org.apache.sling.feature.launcher.impl.launchers.FrameworkLauncher;
import org.apache.sling.feature.launcher.spi.LauncherPrepareContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.osgi.framework.connect.ConnectContent;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;

public class AtomosLauncher extends FrameworkLauncher {
    @Override
    public void prepare(LauncherPrepareContext launcherPrepareContext, ArtifactId artifactId, Feature feature) throws Exception {
        super.prepare(new LauncherPrepareContext() {
            @Override
            public Logger getLogger() {
                return launcherPrepareContext.getLogger();
            }

            @Override
            public void addAppJar(URL url) {

            }

            @Override
            public URL getArtifactFile(ArtifactId artifactId) throws IOException {
                return null;
            }
        }, artifactId, feature);
    }

    @Override
    protected String getFrameworkRunnerClass() {
        return AtomosRunner.class.getName();
    }

    @Override
    public LauncherClassLoader createClassLoader() {
        return LOADER;
    }

    private static AtomosLauncherClassLoader LOADER = new AtomosLauncherClassLoader();

    private static final class AtomosLauncherClassLoader extends LauncherClassLoader {
        static {
            ClassLoader.registerAsParallelCapable();
        }

        private final ClassLoader parent;
        private final AtomosWeaver weaver;
        private final URLClassLoader urlClassLoader;

        private AtomosLauncherClassLoader() {
            parent = getClass().getClassLoader();
            weaver = getWeaver(parent);
            if (weaver != null) {
                urlClassLoader = new URLClassLoader(((URLClassLoader) parent).getURLs());
            } else {
                urlClassLoader = null;
            }
        }

        private AtomosWeaver getWeaver(ClassLoader parent) {
            if (parent instanceof URLClassLoader) {
                Iterator<AtomosWeaver> loader = ServiceLoader.load(AtomosWeaver.class).iterator();
                if (loader.hasNext()) {
                    return loader.next();
                }
            }
            return null;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (weaver != null && !name.startsWith("java")) {
                URL resource = urlClassLoader.getResource(name.replace('.', '/') + ".class");
                if (resource != null && resource.getProtocol().equals("jar")) {
                    try {
                        byte[] bytes = resource.openStream().readAllBytes();

                        if (!name.startsWith("org.apache.sling.feature.launcher.atomos.AtomosRunner") && !name.startsWith("org.osgi.framework")) {
                            try {
                                bytes = weaver.weave(bytes, AtomosRunner.class.getName(), "getAtomosLoaderWrapped", "getAtomosLoaderResourceWrapped",
                                        "getAtomosLoaderStreamWrapped", parent);
                            } catch (Throwable t) {
                                t.printStackTrace();
                            }
                        }

                        return defineClass(name, bytes, 0, bytes.length, new ProtectionDomain(new CodeSource(new File(
                                ((JarURLConnection) resource.openConnection()).getJarFile().getName()).toURI().toURL(), (Certificate[]) null), null));
                    } catch (IOException e) {
                    }
                }
            }
            return parent.loadClass(name);
        }

        @Override
        public URL findResource(String name) {
            return parent.getResource(name);
        }

        @Override
        public Enumeration<URL> findResources(String name) throws IOException {
            return parent.getResources(name);
        }
    }
}
