/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.feature.launcher.extensions.connect.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.launcher.impl.launchers.FrameworkLauncher;
import org.apache.sling.feature.launcher.spi.LauncherPrepareContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;

public class PojoSRLauncher extends FrameworkLauncher
{
    @Override
    public void prepare(LauncherPrepareContext context, ArtifactId frameworkId, Feature app) throws Exception
    {
        super.prepare(context, frameworkId, app);

        String filter = "";
        if (frameworkId.getArtifactId().equals("org.apache.felix.connect"))
        {
            for (List<Artifact> bundles : app.getBundles().getBundlesByStartOrder().values())
            {
                for (Artifact artifact : bundles)
                {
                    File file = context.getArtifactFile(artifact.getId());

                    try (JarFile jar = new JarFile(file, false)) {
                        Manifest mf = jar.getManifest();
                        String bsn = mf.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
                        if (bsn != null)
                        {
                            String cp = mf.getMainAttributes().getValue(Constants.BUNDLE_CLASSPATH);
                            if (cp != null)
                            {
                                for (String entry : cp.split(","))
                                {
                                    int idx = entry.indexOf(';');
                                    if (idx != -1)
                                    {
                                        entry = entry.substring(0, idx);
                                    }
                                    entry = entry.trim();
                                    if (!entry.isEmpty() && !entry.equals("."))
                                    {
                                        JarEntry content = jar.getJarEntry(entry);
                                        if (content != null && !content.isDirectory())
                                        {
                                            File target = File.createTempFile(entry, ".jar");
                                            try
                                            {
                                                try (InputStream input = jar.getInputStream(content))
                                                {
                                                    try (FileOutputStream output = new FileOutputStream(target))
                                                    {
                                                        byte[] buffer = new byte[64 * 1024];
                                                        for (int i = input.read(buffer); i != -1; i = input.read(buffer))
                                                        {
                                                            output.write(buffer, 0, i);
                                                        }
                                                    }
                                                    context.addAppJar(target);
                                                }
                                            }
                                            catch (Exception ex)
                                            {
                                                ex.printStackTrace();
                                            }
                                        }
                                    }
                                    else
                                    {
                                        context.addAppJar(file);
                                        filter += "(" + Constants.BUNDLE_SYMBOLICNAME + "=" +  bsn + ")";
                                    }
                                }
                            }
                            else
                            {
                                context.addAppJar(file);
                                filter += "(" + Constants.BUNDLE_SYMBOLICNAME + "=" +  bsn + ")";
                            }
                        }

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }

            if (!filter.isEmpty()) {
                filter = "(|" + filter + ")";
                app.getFrameworkProperties().put("pojosr.filter", filter);
            }
        }
    }

    @Override
    protected String getFrameworkRunnerClass()
    {
        return PojoSRRunner.class.getName();
    }

    @Override
    public LauncherClassLoader createClassLoader()
    {
        LauncherClassLoader cl = new PojoSRLauncherClassLoader();
        cl.addURL(PojoSRLauncher.class.getProtectionDomain().getCodeSource().getLocation());
        return cl;
    }

    private class PojoSRLauncherClassLoader extends LauncherClassLoader implements BundleReference {

        @Override
        public Bundle getBundle()
        {
            try
            {
                return (Bundle) loadClass(FrameworkUtil.class.getName()).getField("BUNDLE").get(null);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException
        {
            return (name.startsWith("org.osgi.framework.") && !name.startsWith(FrameworkUtil.class.getName())) ||
                name.startsWith("org.osgi.resource.") ?
                PojoSRLauncher.class.getClassLoader().loadClass(name) :
                super.findClass(name);
        }
    }
}
