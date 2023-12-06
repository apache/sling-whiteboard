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
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipException;

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
import org.apache.sling.feature.launcher.atomos.weaver.AtomosWeaver;
import org.apache.sling.feature.launcher.impl.launchers.FrameworkLauncher;
import org.apache.sling.feature.launcher.spi.LauncherPrepareContext;
import org.apache.sling.feature.launcher.spi.LauncherRunContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.util.converter.Converter;
import org.osgi.util.function.Function;
import org.slf4j.LoggerFactory;

public class AtomosConfigLauncher extends FrameworkLauncher {

    private volatile Feature m_app;
    private final AtomosWeaver m_weaver;

    public AtomosConfigLauncher() {
        m_weaver = getWeaver();

        if (m_weaver == null) {
            throw new IllegalStateException("AtomosWeaver not found via ServiceLoader");
        }
    }

    private static AtomosWeaver getWeaver() {
        Iterator<AtomosWeaver> loader = ServiceLoader.load(AtomosWeaver.class).iterator();
        if (loader.hasNext()) {
            return loader.next();
        }
        return null;
    }

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

                destJar.putNextEntry(entry);
                InputStream is = srcJar.getInputStream(entry);
                is.transferTo(destJar);
                destJar.closeEntry();
            }

            return destJar;
        }
    }

    private void addClassesToJar(String jarFileName, JarOutputStream outJar, ClassLoader cl) throws IOException {
        System.out.println("*** Jar file: " + jarFileName);
        try (JarFile jf = new JarFile(jarFileName)) {
            for (JarEntry entry : Collections.list(jf.entries())) {
                String fileName = entry.getName();

                // only add class entries.
                if (!fileName.endsWith(".class") &&
                    !fileName.endsWith(".properties")) {  // The .properties files are included here until we have weave ResourceBundle invocations
                    // !fileName.endsWith(".css") && !fileName.endsWith(".js") &&  !fileName.endsWith(".gif") && !fileName.endsWith(".png")) {
                    continue;
                }

                if (fileName.endsWith("module-info.class")) {
                    continue;
                }

                try {
                    JarEntry newEntry = new JarEntry(entry.getName());
                    outJar.putNextEntry(newEntry);
                    InputStream is = jf.getInputStream(entry);

                    // Weave the class bytes and then write them to the target jar
                    String className = fileName.substring(0, fileName.length() - 6).replace('/', '.');
                    byte[] classBytes = is.readAllBytes();
                    System.out.print("Classname: " + className + " length before " + classBytes.length);
                    try {
                        byte[] woven = m_weaver.weave(classBytes, "org.apache.sling.feature.launcher.atomos.AtomosRunner",
                            "getAtomosLoaderWrapped", "getAtomosLoaderResourceWrapped", "getAtomosLoaderStreamWrapped", cl);
                        System.out.print(" length after " + woven.length);
                        if (classBytes.length != woven.length) {
                            System.out.println(" - woven!");
                        } else {
                            System.out.println();
                        }
                        outJar.write(woven);
                    } catch (Exception ex) {
                        System.out.println("\nProblem weaving " + className + " " + ex.getMessage());
                        // Use unwoven bytes
                        outJar.write(classBytes);
                    }
                    outJar.closeEntry();
                } catch (ZipException ze) {
                    // Happens in case of a duplicate class file.
                    System.out.println("Warn: " + ze.getMessage());

                    continue;
                }
            }
        }
    }

    private void addNativeImageProperties(JarOutputStream jarToBuild, String iabt) throws IOException {
        // String iart = "--initialize-at-run-time=org.apache.http.osgi.impl.OSGiCredentialsProvider," +
        //     "org.eclipse.jetty.http.MimeTypes," +
        //     "org.eclipse.jetty.server.handler.ContextHandler," +
        //     "org.osgi.util.converter.Converters," +
        //     "org.osgi.util.converter.ConvertingImpl," +
        //     "org.owasp.esapi.reference.DefaultValidator," +
        //     "org.owasp.esapi.reference.JavaLogFactory$JavaLogger," +
        //     "org.quartz.core.QuartzScheduler";

        String iart = "--initialize-at-run-time=" +
            "org.owasp.esapi.reference.DefaultValidator," +
            "org.owasp.esapi.reference.JavaLogFactory$JavaLogger";

        iabt += ",org.apache.sling.feature.launcher.atomos.AtomosRunner";

        JarEntry je = new JarEntry("META-INF/native-image/app/native-image.properties");
        jarToBuild.putNextEntry(je);
        String args = "Args = " + iabt + " " + iart + System.lineSeparator();
        jarToBuild.write(args.getBytes());
        jarToBuild.closeEntry();
    }

    private List<String> extractBundleClassPathJars(List<String> jars, File targetDir) throws IOException {
        targetDir.mkdirs();
        List<String> result = new ArrayList<>();

        for (String jar : jars) {
            System.out.println("Extracting BCP jars from " + jar);
            File file = new File(jar);
            try (JarFile jf = new JarFile(file)) {
                Manifest mf = jf.getManifest();
                Attributes ma = mf.getMainAttributes();
                String bcp = ma.getValue("Bundle-ClassPath");

                if (bcp == null) {
                    continue;
                }
                System.out.println("Found Bundle Classpath: " + bcp);

                for (String embedded : bcp.split(",")) {
                    JarEntry entry = jf.getJarEntry(embedded);
                    if (entry == null) {
                        continue;
                    }

                    File tDir = new File(targetDir, file.getName());
                    tDir.mkdirs();
                    File tFile = new File(tDir, entry.getName());

                    try (InputStream is = jf.getInputStream(entry);
                        OutputStream os = new FileOutputStream(tFile)) {
                        is.transferTo(os);
                    }
                    result.add(tFile.getAbsolutePath());
                }
            }
        }

        return result;
    }

    @Override
    public int run(LauncherRunContext context, ClassLoader cl) throws Exception {
        int result = super.run(context, cl);
        if (result == FrameworkEvent.STOPPED) {
            File outputDir = new File(Paths.get("").toAbsolutePath().toFile(), "atomos-config");
            outputDir.mkdirs();

            try (JarOutputStream jarToBuild = getJarToBuild(new File(outputDir, "atomos.substrate.jar"), new File(outputDir, "app.substrate.jar"))) {
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

                            extractJarsAndCollect(nativeConfig, jarToBuild, outputDir);
                        }
                        script += "\"\n\nexport ATOMOS_INIT=\"";
                        if (nativeConfig.containsKey("initialize-at-build-time")) {
                            String iabt = "--initialize-at-build-time=" + nativeConfig.getJsonArray("initialize-at-build-time").getValuesAs(JsonString.class).stream().map(JsonString::getString).collect(Collectors.joining(","));
                            script += iabt;

                            // Add the initialize at build time to a native-image.properties file
                            addNativeImageProperties(jarToBuild, iabt);
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

    private void extractJarsAndCollect(JsonObject nativeConfig, JarOutputStream jarToBuild, File outputDir) throws IOException {
        List<String> jarsToBeCleaned = nativeConfig.getJsonArray("classpath").getValuesAs(JsonString.class).stream().map(JsonString::getString).collect(Collectors.toList());
        List<String> jars = jarsToBeCleaned.stream()
            .filter(n -> !n.equals("atomos.substrate.jar"))
            .filter(n -> !n.contains("org.apache.felix.framework-"))
            .collect(Collectors.toList());

        List<String> bcpJars = extractBundleClassPathJars(jars, new File(outputDir, "../bcpJars"));
        jars.addAll(bcpJars);

        List<URL> jarURLs = nativeConfig.getJsonArray("classpath").getValuesAs(JsonString.class).stream().map(JsonString::getString)
            .map(j -> {
                try {
                    return new File(j).toURI().toURL();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }}).collect(Collectors.toList());
        List<URL> bcpJarURLs = bcpJars.stream().map(j -> {
                try {
                    return new File(j).toURI().toURL();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }}).collect(Collectors.toList());
        jarURLs.addAll(bcpJarURLs);

        List<URL> clURLs = new ArrayList<>(jarURLs);
        // Temp hack for the org.owasp.esapi.reference.DefaultSecurityConfiguration which references (imports)
        // an antiquated commons-lang class but doesn't use it through the code path we are executing.
        // It also doesn't import it on the OSGi level.
        clURLs.add(new URL("https://repo.maven.apache.org/maven2/commons-lang/commons-lang/2.6/commons-lang-2.6.jar"));
        URLClassLoader jarCL = new URLClassLoader(clURLs.toArray(new URL[0]));

        for (String jar : jars) {
            try {
            addClassesToJar(jar, jarToBuild, jarCL);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
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
