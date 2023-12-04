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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.script.ScriptEngineManager;

import org.apache.felix.atomos.utils.api.plugin.SubstratePlugin;
import org.apache.felix.atomos.utils.core.LauncherBuilderImpl;
import org.apache.felix.atomos.utils.substrate.api.resource.ResourceConfiguration;
import org.apache.felix.atomos.utils.substrate.impl.config.DefaultResourceConfiguration;
import org.apache.felix.cm.json.ConfigurationReader;
import org.apache.felix.framework.Felix;
import org.apache.felix.scr.impl.logger.BundleLogger;
import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.builder.BuilderContext;
import org.apache.sling.feature.builder.FeatureBuilder;
import org.apache.sling.feature.builder.FeatureProvider;
import org.apache.sling.feature.io.IOUtils;
import org.apache.sling.feature.io.json.FeatureJSONReader;
import org.apache.sling.feature.launcher.impl.launchers.FrameworkLauncher;
import org.apache.sling.feature.launcher.spi.LauncherPrepareContext;
import org.apache.sling.feature.launcher.spi.LauncherRunContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.util.converter.Converter;
import org.osgi.util.function.Function;
import org.slf4j.LoggerFactory;

public class AtomosConfigLauncher extends FrameworkLauncher {

    private volatile Feature m_app;

    @Override
    public void prepare(final LauncherPrepareContext context,
                        final ArtifactId frameworkId,
                        final Feature app) throws Exception {
        context.addAppJar(AtomosConfigLauncher.class.getProtectionDomain().getCodeSource().getLocation());
        context.addAppJar(Felix.class.getProtectionDomain().getCodeSource().getLocation());
        context.addAppJar(LoggerFactory.class.getProtectionDomain().getCodeSource().getLocation());
        context.addAppJar(IOUtils.class.getProtectionDomain().getCodeSource().getLocation());
        context.addAppJar(ConfigurationReader.class.getProtectionDomain().getCodeSource().getLocation());
        context.addAppJar(Json.class.getProtectionDomain().getCodeSource().getLocation());
        context.addAppJar(Converter.class.getProtectionDomain().getCodeSource().getLocation());
        context.addAppJar(SubstratePlugin.class.getProtectionDomain().getCodeSource().getLocation());
        context.addAppJar(LauncherBuilderImpl.class.getProtectionDomain().getCodeSource().getLocation());
        context.addAppJar(Function.class.getProtectionDomain().getCodeSource().getLocation());
        context.addAppJar(ResourceConfiguration.class.getProtectionDomain().getCodeSource().getLocation());
        context.addAppJar(BundleLogger.class.getProtectionDomain().getCodeSource().getLocation());
        context.addAppJar(DefaultResourceConfiguration.class.getProtectionDomain().getCodeSource().getLocation());
        context.addAppJar(ScriptEngineManager.class.getProtectionDomain().getCodeSource().getLocation());
        super.prepare(context, ArtifactId.fromMvnId("org.apache.felix:org.apache.felix.framework:7.0.5"), app);
        app.getBundles().addAll(Stream.of(
                new Artifact(ArtifactId.fromMvnId("org.apache.sling:org.apache.sling.feature.launcher.atomos:0.0.1-SNAPSHOT")),
                new Artifact(ArtifactId.fromMvnId("org.apache.sling:org.apache.sling.feature.launcher:1.2.4")),
                new Artifact(ArtifactId.fromMvnId("org.apache.felix:org.apache.felix.framework:7.0.5")),
                new Artifact(ArtifactId.fromMvnId("org.osgi:osgi.core:8.0.0")),
                new Artifact(ArtifactId.fromMvnId("org.apache.felix:org.apache.felix.atomos:1.0.1-SNAPSHOT")),
                new Artifact(ArtifactId.fromMvnId("org.slf4j:slf4j-simple:1.7.25")),
                new Artifact(ArtifactId.fromMvnId("org.apache.sling:org.apache.sling.feature:1.3.0")),
                new Artifact(ArtifactId.fromMvnId("org.apache.felix:org.apache.felix.cm.json:1.0.6")),
                new Artifact(ArtifactId.fromMvnId("commons-cli:commons-cli:1.4"))).map(artifact -> {
                    artifact.setStartOrder(Integer.MAX_VALUE);
                    return artifact;
        }).collect(Collectors.toList()));
        app.getBundles().forEach(bundle -> {
            try {
                context.addAppJar(context.getArtifactFile(bundle.getId()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        m_app = app;
    }

    private JarOutputStream getJarToBuild(File inJar, File outJar) throws IOException {
        try(JarFile srcJar = new JarFile(inJar)) {
            JarOutputStream destJar = new JarOutputStream(new FileOutputStream(outJar), srcJar.getManifest());

            for(JarEntry entry : Collections.list(srcJar.entries())) {
                if ("META-INF/MANIFEST.MF".equals(entry.getName())) {
                    continue;
                }

                InputStream is = srcJar.getInputStream(entry);
                destJar.putNextEntry(entry);
                is.transferTo(destJar);
            }

            return destJar;
        }
    }

    @Override
    public int run(LauncherRunContext context, ClassLoader cl) throws Exception {
        int result = super.run(context, cl);
        if (result == FrameworkEvent.STOPPED) {
            File outputDir = new File(Paths.get("").toAbsolutePath().toFile(), "atomos-config");
            outputDir.mkdirs();

            try (JarOutputStream jarToBuild = getJarToBuild(new File(outputDir, "atomos.substrate.jar"), new File(outputDir, "app.substrate.jar"))) {
            //     // nothing else to add
            // } catch (Exception e) {
            //     e.printStackTrace();

            //     // Do system exit here for now because otherwise any exception here gets drowned out by others
            //     System.exit(-1);
            // }

                try (Reader reader = new FileReader(new File(outputDir, "config-feature.slingosgifeature"), StandardCharsets.UTF_8)) {
                    Feature config = FeatureJSONReader.read(reader, null);
                    Feature assembled = FeatureBuilder.assemble(ArtifactId.parse("config:assembled:1.0.0"), new BuilderContext(new FeatureProvider() {
                        @Override
                        public Feature provide(ArtifactId id) {
                            return null;
                        }
                    }), m_app, config);
                    Extension assembledEx = assembled.getExtensions().getByName("atomos-config");
                    JsonObject nativeConfig = assembledEx.getJSONStructure().asJsonObject();
                    write(outputDir, jarToBuild, "reflect-config", nativeConfig);
                    write(outputDir, jarToBuild, "resource-config", nativeConfig);
                    write(outputDir, jarToBuild, "proxy-config", nativeConfig);
                    write(outputDir, jarToBuild, "jni-config", nativeConfig);
                    write(outputDir, jarToBuild, "serialization-config", nativeConfig);

                    try (FileOutputStream output = new FileOutputStream(new File(outputDir,  "atomos_init.sh"))) {
                        String script = "#!/bin/sh\n\nexport ATOMOS_CLASSPATH=\"";
                        if (nativeConfig.containsKey("classpath")) {
                            script += nativeConfig.getJsonArray("classpath").getValuesAs(JsonString.class).stream().map(JsonString::getString).collect(Collectors.joining(":"));
                        }
                        script += "\"\n\nexport ATOMOS_INIT=\"";
                        if (nativeConfig.containsKey("initialize-at-build-time")) {
                            script += "--initialize-at-build-time=" + nativeConfig.getJsonArray("initialize-at-build-time").getValuesAs(JsonString.class).stream().map(JsonString::getString).collect(Collectors.joining(","));
                        }
                        script += "\"\n";
                        output.write(script.getBytes(StandardCharsets.UTF_8));
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                    /* */ System.out.println("*** About to throw t");
                    throw t;
                }
            }
        }

        /* */ System.out.println("*** Finished");
        System.exit(0);
        return result;
    }

    private void write(File outputDir, JarOutputStream outputJar, String name, JsonObject source) throws IOException {
        try (FileOutputStream output = new FileOutputStream(new File(outputDir, name + ".json"))) {
            if (source.containsKey(name)) {
                Json.createWriter(output).write(source.get(name));

                JarEntry je = new JarEntry("META-INF/native-image/app/" + name + ".json");

                outputJar.putNextEntry(je);
                Json.createWriter(outputJar).write(source.get(name));
            }
        }
    }

    @Override
    protected String getFrameworkRunnerClass() {
        return AtomosConfigRunner.class.getName();
    }

    @Override
    public LauncherClassLoader createClassLoader() {
        return new LauncherClassLoader(){
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                try {
                    return super.findClass(name);
                } catch (Exception e) {
                    return LauncherClassLoader.class.getClassLoader().loadClass(name);
                }
            }
        };
    }
}
