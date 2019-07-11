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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.io.IOUtils;
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
        context.addAppJar(context.getArtifactFile(frameworkId));

        String filter = "";
        if (frameworkId.getArtifactId().equals("org.apache.felix.connect"))
        {
            for (List<Artifact> bundles : app.getBundles().getBundlesByStartOrder().values())
            {
                for (Artifact artifact : bundles)
                {
                    URL url = context.getArtifactFile(artifact.getId());
                    try (JarFile jar = IOUtils.getJarFileFromURL(url, true, null)) {
                        Manifest mf = jar.getManifest();
                        String bsn = mf.getMainAttributes() != null ? mf.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME) : null;
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
                                            try
                                            {
                                                String urlString = "jar:" + IOUtils.getFileFromURL(getJarFileURL(url, content), true, null).toURI().toURL() + "!/";
                                                System.out.println(urlString);
                                                context.addAppJar(new URL(urlString));
                                            }
                                            catch (Exception ex)
                                            {
                                                ex.printStackTrace();
                                            }
                                        }
                                    }
                                    else
                                    {
                                        context.addAppJar(url);
                                        filter += "(" + Constants.BUNDLE_SYMBOLICNAME + "=" +  bsn + ")";
                                    }
                                }
                            }
                            else
                            {
                                context.addAppJar(url);
                                filter += "(" + Constants.BUNDLE_SYMBOLICNAME + "=" +  bsn + ")";
                            }
                        }
                        else {
                            context.addAppJar(url);
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

    private URL getJarFileURL(URL jar, JarEntry entry) throws Exception {
        String urlString = jar.toExternalForm();
        if (jar.getProtocol().equals("jar")) {

            return new URL(urlString + (urlString.endsWith("!/") ? entry.getName() : "!/" + entry.getName()));
        }
        else {
            return new URL("jar:" + urlString +  "!/" + entry.getName());
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
                define(name);
        }

        private Class define(String name) throws ClassNotFoundException
        {
            URL resource = findResource(name.replace(".", "/" ) + ".class");
            if (resource != null) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                try (InputStream input = resource.openStream() ) {
                    byte[] b = new byte[64 * 1024];
                    for (int i = input.read(b);i != -1; i = input.read(b)) {
                        buffer.write(b, 0, i);
                    }

                    return super.defineClass(name, buffer.toByteArray(),0, buffer.size(),
                        new CodeSource(new URL(resource.toExternalForm().substring(0, resource.toExternalForm().lastIndexOf("!/") + 2)), (java.security.cert.Certificate[]) null));
                }
                catch (IOException e)
                {
                    throw new ClassNotFoundException(e.getMessage());
                }
            }
            else {
                throw new ClassNotFoundException(name);
            }
        }

        public void indexURLs()
        {
            if (init.compareAndSet(false, true))
            {
                for (URL url : super.getURLs())
                {
                    try (JarFile jar = IOUtils.getJarFileFromURL(url, true, null))
                    {
                        for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements(); )
                        {
                            JarEntry entry = entries.nextElement();
                            String[] paths;
                            if (entry.isDirectory())
                            {
                                paths = new String[]{entry.getName(), entry.getName().substring(0, entry.getName().length() - 1),
                                    "/" + entry.getName(), "/" + entry.getName().substring(0, entry.getName().length() - 1)};
                            }
                            else
                            {
                                paths = new String[]{entry.getName(), "/" + entry.getName()};
                            }

                            for (String path : paths)
                            {
                                resourcesIDX.compute(path, (key, value) ->
                                {
                                    if (value == null)
                                    {
                                        value = new ArrayList<>();
                                    }
                                    try
                                    {
                                        value.add(getJarFileURL(url, entry));
                                    }
                                    catch (Exception e)
                                    {
                                        e.printStackTrace();
                                    }
                                    return value;
                                });
                            }
                        }
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }

        private volatile AtomicBoolean init = new AtomicBoolean(false);

        private final ConcurrentHashMap<String, List<URL>> resourcesIDX = new ConcurrentHashMap<String, List<URL>>();

        @Override
        public URL findResource(String name)
        {
            indexURLs();
            List<URL> urls = resourcesIDX.getOrDefault(name, Collections.emptyList());
            return urls.isEmpty() ? null : urls.get(0);
        }

        @Override
        public Enumeration<URL> findResources(String name) throws IOException
        {
            indexURLs();
            return Collections.enumeration(resourcesIDX.getOrDefault(name, Collections.emptyList()));
        }
    }
}
