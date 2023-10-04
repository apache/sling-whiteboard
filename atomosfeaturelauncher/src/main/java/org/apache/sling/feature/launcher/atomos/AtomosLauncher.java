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

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.launcher.impl.launchers.FrameworkLauncher;
import org.apache.sling.feature.launcher.spi.LauncherPrepareContext;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

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
        return new AtomosLauncherClassLoader();
    }

    private static final class AtomosLauncherClassLoader extends LauncherClassLoader {
        static {
            ClassLoader.registerAsParallelCapable();
        }

        private final ClassLoader parent;

        private AtomosLauncherClassLoader() {
            parent = getClass().getClassLoader();
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
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
