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
package org.apache.sling.feature.launcher.atomos.config;

import org.apache.felix.atomos.utils.api.Config;
import org.apache.felix.atomos.utils.api.Context;
import org.apache.felix.atomos.utils.api.FileType;
import org.apache.felix.atomos.utils.api.Launcher;
import org.apache.felix.atomos.utils.api.LauncherBuilder;
import org.apache.felix.atomos.utils.api.RegisterServiceCall;
import org.apache.felix.atomos.utils.api.plugin.ClassPlugin;
import org.apache.felix.atomos.utils.api.plugin.ComponentDescription;
import org.apache.felix.atomos.utils.api.plugin.ComponentMetaDataPlugin;
import org.apache.felix.atomos.utils.api.plugin.FileCollectorPlugin;
import org.apache.felix.atomos.utils.api.plugin.FinalPlugin;
import org.apache.felix.atomos.utils.core.plugins.ComponentDescriptionPlugin;
import org.apache.felix.atomos.utils.core.plugins.OsgiDTOPlugin;
import org.apache.felix.atomos.utils.core.plugins.ResourcePlugin;
import org.apache.felix.atomos.utils.core.plugins.activator.InvocatingBundleActivatorPlugin;
import org.apache.felix.atomos.utils.core.plugins.activator.ReflectionBundleActivatorPlugin;
import org.apache.felix.atomos.utils.core.plugins.finaliser.ni.NativeImageBuilderConfig;
import org.apache.felix.atomos.utils.core.plugins.finaliser.ni.NativeImagePlugin;
import org.apache.felix.atomos.utils.core.plugins.index.IndexOutputType;
import org.apache.felix.atomos.utils.core.plugins.index.IndexPluginConfig;
import org.apache.felix.atomos.utils.substrate.api.NativeImageConfigJsonProvider;
import org.apache.felix.atomos.utils.substrate.api.dynproxy.DynamicProxyConfiguration;
import org.apache.felix.atomos.utils.substrate.api.reflect.ReflectionConfiguration;
import org.apache.felix.atomos.utils.substrate.api.resource.ResourceConfiguration;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionState;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.builder.BuilderContext;
import org.apache.sling.feature.builder.FeatureBuilder;
import org.apache.sling.feature.builder.FeatureProvider;
import org.apache.sling.feature.io.IOUtils;
import org.apache.sling.feature.io.json.FeatureJSONReader;
import org.apache.sling.feature.launcher.impl.launchers.AbstractRunner;
import org.osgi.framework.FrameworkEvent;

import javax.json.Json;
import javax.json.stream.JsonParser;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AtomosConfigRunner extends AbstractRunner {
    private final Map<String, String> frameworkProperties;
    private final Map<Integer, List<URL>> bundleMap;

    public AtomosConfigRunner(final Map<String, String> frameworkProperties,
                              final Map<Integer, List<URL>> bundlesMap,
                              final List<Object[]> configurations,
                              final List<URL> installables) {
        super(configurations, installables);
        this.frameworkProperties = frameworkProperties;
        this.bundleMap = bundlesMap;
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

    private int getFrameworkProperty(String propName, int defaultValue) {
        String val = this.frameworkProperties.get(propName);
        if (val == null) {
            return defaultValue;
        } else {
            return Integer.parseInt(val);
        }
    }

    @Override
    public Integer call() throws Exception {
        int defaultStartLevel = getFrameworkProperty("felix.startlevel.bundle", 1);

        List<String> classPath = new ArrayList<>();
        Path currentDir = Paths.get("").toAbsolutePath();

        List<URL> synthetics = this.bundleMap.remove(Integer.MAX_VALUE);
        if (synthetics != null) {
            for (final URL file : synthetics) {
                File localFile = IOUtils.getFileFromURL(file, false, null);
                classPath.add(currentDir.relativize(localFile.toPath().toAbsolutePath()).toString());
            }
        }

        List<String> bundles = new ArrayList<>();

        for(final Integer startLevel : sortStartLevels(this.bundleMap.keySet(), defaultStartLevel)) {
            logger.debug("Installing bundles with start level {}", startLevel);

            for (final URL file : bundleMap.get(startLevel)) {
                File localFile = IOUtils.getFileFromURL(file, false, null);
                classPath.add(currentDir.relativize(localFile.toPath().toAbsolutePath()).toString());
                bundles.add(currentDir.relativize(localFile.toPath().toAbsolutePath()).toString());
            }
        }
        System.out.println(String.join(File.pathSeparator, classPath));
        bundles.add(0, "artifacts/org/apache/felix/org.apache.felix.framework/7.0.5/org.apache.felix.framework-7.0.5.jar");
        LauncherBuilder builder = Launcher.builder();
        Config cfg = new Config() {};
        builder.addPlugin(new FileCollectorPlugin<Config>() {
            @Override
            public void init(Config unused) {

            }

            @Override
            public void collectFiles(Context context) {
                bundles.stream().map(File::new).map(File::toPath).forEach(entry -> context.addFile(entry, FileType.ARTIFACT));
            }
        }, cfg);

        File outputDir = new File(currentDir.toFile(), "atomos-config");
        outputDir.mkdirs();


        IndexPluginConfig ic = new IndexPluginConfig()
        {
            @Override
            public Path indexOutputDirectory()
            {
                return outputDir.toPath();
            }

            @Override
            public IndexOutputType indexOutputType()
            {
                return IndexOutputType.JAR;
            }
        };


        builder.addPlugin(IndexPlugin.class, ic);

        ComponentDescriptionPlugin test = new ComponentDescriptionPlugin();

        builder//
                .addPlugin(ReflectionBundleActivatorPlugin.class, cfg)//
                .addPlugin(new ComponentMetaDataPlugin<Config>() {
                    @Override
                    public void doComponentMetaData(ComponentDescription componentDescription, Context context, ClassLoader classLoader) {
                        try {
                        test.doComponentMetaData(componentDescription, new Context() {
                                    @Override
                                    public void addReflectionClass(String s) {
                                        context.addReflectionClass(s);
                                    }

                                    @Override
                                    public void addReflectionConstructor(String s, String[] strings) {
                                        context.addReflectionConstructor(s, strings);

                                        if (strings != null) {
                                            Arrays.stream(strings).forEach(name -> {
                                                try {
                                                    if (classLoader.loadClass(name).isAnnotation()) {
                                                        addDynamicProxyConfigs(name);
                                                    }
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            });
                                        }
                                    }

                                    @Override
                                    public void addReflectionConstructorAllPublic(String s) {
                                        context.addReflectionConstructorAllPublic(s);
                                    }

                                    @Override
                                    public void addReflectionField(String s, Class<?> aClass) {
                                        context.addReflectionField(s, aClass);
                                        if (aClass != null && aClass.isAnnotation()) addDynamicProxyConfigs(aClass.getName());
                                        try {
                                            if (aClass.getField(s).getType().isAnnotation()) addDynamicProxyConfigs(aClass.getField(s).getType().getName());
                                        } catch (Throwable e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    @Override
                                    public void addReflectionFieldsAllPublic(String s) {
                                        context.addReflectionFieldsAllPublic(s);
                                    }

                                    @Override
                                    public void addFile(Path path, FileType fileType) {
                                        context.addFile(path, fileType);
                                    }

                                    @Override
                                    public void addReflectionMethod(String name, Class<?> aClass) {
                                        Arrays.stream(aClass.getDeclaredMethods()).filter(m -> m.getName().equals(name)).findFirst()
                                                .ifPresent(m -> addReflectionMethod(m));
                                    }

                                    @Override
                                    public void addReflecionMethod(String s, Class<?>[] classes, Class<?> aClass) {
                                        context.addReflecionMethod(s, classes, aClass);
                                        try {
                                            Arrays.stream(aClass.getDeclaredMethods()).filter(m -> m.getName().equals(s)).map(Method::getParameterTypes).flatMap(Arrays::stream).filter(Class::isAnnotation).map(Class::getName).forEach(this::addDynamicProxyConfigs);
                                        } catch (Throwable e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    @Override
                                    public void addReflectionMethodsAllPublic(String s) {
                                        context.addReflectionMethodsAllPublic(s);
                                    }

                                    @Override
                                    public void addRegisterServiceCalls(RegisterServiceCall registerServiceCall) {
                                        context.addRegisterServiceCalls(registerServiceCall);
                                    }

                                    @Override
                                    public void addResourceConfig(ResourceConfiguration resourceConfiguration) {
                                        context.addResourceConfig(resourceConfiguration);
                                    }

                                    @Override
                                    public Stream<Path> getFiles(FileType... fileTypes) {
                                        return context.getFiles(fileTypes);
                                    }

                                    @Override
                                    public List<RegisterServiceCall> getRegisterServiceCalls() {
                                        return context.getRegisterServiceCalls();
                                    }

                                    @Override
                                    public ResourceConfiguration getResourceConfig() {
                                        return context.getResourceConfig();
                                    }

                                    @Override
                                    public DynamicProxyConfiguration getDynamicProxyConfig() {
                                        return context.getDynamicProxyConfig();
                                    }

                                    @Override
                                    public void addDynamicProxyConfigs(String... strings) {
                                        context.addDynamicProxyConfigs(strings);
                                    }

                                    @Override
                                    public ReflectionConfiguration getReflectConfig() {
                                        return context.getReflectConfig();
                                    }
                                }
                                , new SecureClassLoader() {
                            @Override
                            public Class<?> loadClass(String name) throws ClassNotFoundException {
                                try {
                                    return getClass().getClassLoader().loadClass(name);
                                } catch (Exception e) {
                                    return classLoader.loadClass(name);
                                }
                            }
                        }); } catch (NoClassDefFoundError err) {
                            err.printStackTrace();
                        }
                    }

                    @Override
                    public void init(Config config) {
                        test.init(config);
                    }
                }, cfg)//
                .addPlugin(InvocatingBundleActivatorPlugin.class, cfg)//
                .addPlugin(OsgiDTOPlugin.class, cfg)//
                .addPlugin(ResourcePlugin.class, cfg);//

        builder.addPlugin(new FinalPlugin<Void>() {
            @Override
            public void init(Void unused) {

            }

            @Override
            public void doFinal(Context context) {
                try {
                    // prepare configuration files
                    DynamicProxyConfiguration dynPrC = context.getDynamicProxyConfig();
                    String sDynPrC = NativeImageConfigJsonProvider.newInstance().json(dynPrC);
                    ReflectionConfiguration refCs = context.getReflectConfig();
                    String sRefCs = NativeImageConfigJsonProvider.newInstance().json(refCs);
                    ResourceConfiguration resC = context.getResourceConfig();
                    String sResC = NativeImageConfigJsonProvider.newInstance().json(resC);

                    bundles.add("atomos.substrate.jar");
                    String configFeature =
                            "{\"id\":\"org.apache.sling:org.apache.sling.feature.launcher.atomos.config:0.0.1-SNAPSHOT:calculated\"," +
                            "\"atomos-config:JSON|false\": {" +
                                "\"proxy-config\":" + sDynPrC +
                                ",\"reflect-config\":" + sRefCs +
                                ",\"resource-config\":" + sResC +
                                ",\"classpath\": [" + bundles.stream().map(entry -> "\"" + entry + "\"").collect(Collectors.joining(",")) + "]" +
                            "}}";
                    try (OutputStream output = new FileOutputStream(new File(outputDir, "config-feature.slingosgifeature"))) {
                        output.write(configFeature.getBytes("UTF-8"));
                    }
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }, null);

        builder.build().execute();

        return FrameworkEvent.STOPPED;
    }
}
