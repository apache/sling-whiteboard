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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

/**
 * Goal which parses the specified RepoInit files. Any errors encountered will
 * be reported and fail the build.
 */
@Mojo(name = "parse", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresProject = false, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class ParseMojo extends BaseMojo {

    /**
     * The entry point to Maven Artifact Resolver, i.e. the component doing all the
     * work.
     */
    @Component
    protected RepositorySystem repoSystem;

    /**
     * The current repository/network configuration of Maven.
     */
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    protected RepositorySystemSession repoSession;

    /**
     * The project's remote repositories to use for the resolution.
     */
    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
    protected List<RemoteRepository> remoteRepos;

    /**
     * The version of Sling RepoInit parser to use to parse the repoinit files.
     * 
     * <a href=
     * "https://search.maven.org/search?q=a:org.apache.sling.repoinit.parser">Released
     * Sling RepoInit Parser versions</a>
     */
    @Parameter(property = "repoinit.parserVersion", required = true)
    protected String parserVersion;

    public void execute()
            throws MojoExecutionException {

        getLog().info("Loading scripts from: " + scriptBaseDir);
        List<File> scripts = findScripts();

        if (scripts.isEmpty()) {
            return;
        }

        File parserJar = resolveParser();
        try (URLClassLoader classLoader = new URLClassLoader(new URL[] { parserJar.toURI().toURL() })) {
            Class<?> parserClass = classLoader
                    .loadClass("org.apache.sling.repoinit.parser.impl.RepoInitParserImpl");

            for (File script : scripts) {
                getLog().info("Parsing script: " + script.getAbsolutePath());
                parseScript(parserClass, script);
            }
            getLog().info("All scripts parsed successfully!");
        } catch (IllegalArgumentException | SecurityException | ClassNotFoundException | IOException e) {
            throw new MojoExecutionException("Could not parse scripts: " + e.getMessage(), e);
        }
    }

    private void parseScript(Class<?> parserClass, File script) throws MojoExecutionException {
        try {
            try (Reader reader = new BufferedReader(new FileReader(script))) {
                Object parser = parserClass.getConstructor(Reader.class)
                        .newInstance(reader);
                Method parse = parser.getClass().getDeclaredMethod("parse");
                List<?> operations = (List<?>) parse.invoke(parser);
                getLog().info("Parsed operations: \n\n"
                        + operations.stream().map(Object::toString).collect(Collectors.joining("\n")) + "\n");
            }
        } catch (InvocationTargetException e) {
            throw new MojoExecutionException(
                    "Failed to parse script " + script.getAbsolutePath() + " Exception: "
                            + e.getTargetException().getMessage(),
                    e.getTargetException());
        } catch (InstantiationException | NoSuchMethodException | SecurityException | IllegalAccessException
                | IllegalArgumentException e) {
            throw new MojoExecutionException("Unexpected exception calling parser: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to read script from files: " + e.getMessage(), e);
        }
    }

    private File resolveParser() throws MojoExecutionException {
        try {
            Artifact artifact = new DefaultArtifact(
                    "org.apache.sling:org.apache.sling.repoinit.parser:" + parserVersion);
            ArtifactRequest request = new ArtifactRequest();
            request.setArtifact(artifact);
            request.setRepositories(remoteRepos);
            getLog().info("Getting Repoint Parser version " + artifact.getVersion() + " from " + remoteRepos);
            ArtifactResult result = repoSystem.resolveArtifact(repoSession, request);
            getLog().info("Resolved artifact " + artifact + " to " + result.getArtifact().getFile() + " from "
                    + result.getRepository());
            return result.getArtifact().getFile();
        } catch (IllegalArgumentException | ArtifactResolutionException e) {
            throw new MojoExecutionException("Couldn't download artifact: " + e.getMessage(), e);
        }

    }
}
