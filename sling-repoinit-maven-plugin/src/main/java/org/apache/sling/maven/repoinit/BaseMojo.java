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
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

public abstract class BaseMojo extends AbstractMojo {

    /**
     * The base directory under which to search for repoinit files.
     */
    @Parameter(property = "repoinit.scriptBaseDir", defaultValue = "${project.basedir}/src/main/repoinit", required = false)
    File scriptBaseDir;

    /**
     * The files included when processing the plugin. Supports glob style patterns.
     * All globs are evaluated relative to the <code>scriptBaseDir</code>
     * 
     * <a href="https://docs.oracle.com/javase/tutorial/essential/io/find.html">More
     * on Globs in Java</a>
     */
    @Parameter(property = "repoinit.includedFiles", required = false, defaultValue = "*.txt")
    List<String> includedFiles;

    List<File> findScripts() throws MojoExecutionException {

        List<PathMatcher> matchers = includedFiles.stream()
                .map(pattern -> FileSystems.getDefault().getPathMatcher("glob:" + pattern))
                .collect(Collectors.toList());

        getLog().info("Loading scripts from: " + scriptBaseDir + " using patterns: " + includedFiles);
        try {
            try (Stream<Path> paths = Files.walk(scriptBaseDir.toPath())) {
                List<File> scripts = paths.map(path -> scriptBaseDir.toPath().relativize(path))
                        .filter(file -> matchers.stream().anyMatch(matcher -> matcher.matches(file)))
                        .map(path -> scriptBaseDir.toPath().resolve(path).toAbsolutePath())
                        .map(Path::toFile)
                        .collect(Collectors.toList());
                if (scripts.isEmpty()) {
                    getLog().warn(
                            "No scripts found in directory: " + scriptBaseDir + " with patterns: " + includedFiles);
                }
                return scripts;
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Could not find scripts in directory: " + scriptBaseDir
                    + " using patterns: " + includedFiles + " Exception: " + e.getMessage(), e);
        }
    }
}
