/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.maven.repoinit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jackrabbit.util.Text;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.sling.installer.api.serializer.ConfigurationSerializer;
import org.apache.sling.installer.api.serializer.ConfigurationSerializerFactory;

/**
 * Goal which converts Sling RepoInit files to OSGi configurations.
 */
@Mojo(name = "to-osgi-config", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresProject = false)
public class ToOsgiConfigMojo extends BaseMojo {

    /**
     * The directory to which to output the OSGi configuration files
     */
    @Parameter(property = "repoinit.outputDir", required = true)
    File outputDir;

    /**
     * The format to output the OSGi configuration. Must be one of JSON or CONFIG.
     */
    @Parameter(property = "repoinit.outputFormat", defaultValue = "JSON")
    String outputFormat;

    public void execute()
            throws MojoExecutionException {

        if (!ConfigurationSerializerFactory.Format.CONFIG.toString().equals(outputFormat)
                && !ConfigurationSerializerFactory.Format.JSON.toString().equals(outputFormat)) {
            throw new MojoExecutionException("Unsupported output format: " + outputFormat);
        }
        getLog().info("Writing configurations in format: " + outputFormat);
        ConfigurationSerializerFactory.Format format = ConfigurationSerializerFactory.Format.valueOf(outputFormat);

        ConfigurationSerializer serializer = ConfigurationSerializerFactory.create(format);

        String extension = "cfg.json";
        if (format == ConfigurationSerializerFactory.Format.CONFIG) {
            extension = "config";
        }

        List<File> scripts = findScripts();
        for (File script : scripts) {
            convertScript(serializer, script, extension);
        }

        getLog().info("All scripts converted successfully!");
    }

    private void convertScript(ConfigurationSerializer serializer, File script, String extension)
            throws MojoExecutionException {
        try {
            getLog().info("Converting script: " + script.getAbsolutePath());

            StringBuilder sb = new StringBuilder();
            try (Stream<String> lines = Files.lines(script.toPath())) {
                sb.append(lines.collect(Collectors.joining(System.lineSeparator())));
            }

            // strip off the extension, trim it, ensure there's no additional periods in the
            // name and then escape any illegal JCR chars
            String configId = Text
                    .escapeIllegalJcrChars(
                            script.getName().substring(0, script.getName().lastIndexOf(".")).trim().replace(".", "-"));
            String nodeName = String.format("org.apache.sling.jcr.repoinit.RepositoryInitializer.%s.%s", configId,
                    extension);

            File destination = outputDir.toPath().resolve(nodeName).toFile();
            getLog().info("Saving to: " + destination.getAbsolutePath());
            destination.getParentFile().mkdirs();

            Dictionary<String, Object> config = new Hashtable<>();
            config.put("scripts", new String[] { sb.toString() });
            try (OutputStream os = new FileOutputStream(destination)) {
                serializer.serialize(config, os);
            }
        } catch (IOException | UncheckedIOException e) {
            throw new MojoExecutionException("Failed to convert script " + e.getMessage(), e);
        }
    }

}
