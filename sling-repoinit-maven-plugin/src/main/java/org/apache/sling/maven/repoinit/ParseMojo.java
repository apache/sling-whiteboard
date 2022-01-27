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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.sling.repoinit.parser.impl.ParseException;
import org.apache.sling.repoinit.parser.impl.RepoInitParserImpl;
import org.apache.sling.repoinit.parser.impl.TokenMgrError;
import org.apache.sling.repoinit.parser.operations.Operation;
import org.jetbrains.annotations.NotNull;

/**
 * Goal which parses the specified RepoInit files. Any errors encountered will
 * be reported and fail the build.
 */
@Mojo(name = "parse", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresProject = false)
public class ParseMojo extends BaseMojo {

    public void execute()
            throws MojoExecutionException {

        getLog().info("Loading scripts from: " + scriptBaseDir);
        List<File> scripts = findScripts();

        if (scripts.isEmpty()) {
            return;
        }

        for (File script : scripts) {
            getLog().info("Parsing script: " + script.getAbsolutePath());
            parseScript(script);
        }
        getLog().info("All scripts parsed successfully!");
    }

    @NotNull
    List<Operation> parseScript(@NotNull File script) throws MojoExecutionException {
        try {
            try (Reader reader = new BufferedReader(new FileReader(script))) {
                List<Operation> operations = new RepoInitParserImpl(reader).parse();
                getLog().info("Parsed operations: \n\n"
                        + operations.stream().map(Object::toString).collect(Collectors.joining("\n")) + "\n");
                return operations;
            }
        } catch (ParseException | TokenMgrError e) {
            throw new MojoExecutionException(
                    "Failed to parse script " + script.getAbsolutePath() + " Exception: "
                            + e.getMessage(),
                    e);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to read script from files: " + e.getMessage(), e);
        }
    }

}
