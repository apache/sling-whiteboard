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
import java.io.FileReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import javax.jcr.Session;

import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.sling.jcr.repoinit.JcrRepoInitOpsProcessor;
import org.apache.sling.jcr.repoinit.impl.JcrRepoInitOpsProcessorImpl;
import org.apache.sling.maven.repoinit.ContentFolder.Type;
import org.apache.sling.repoinit.parser.operations.Operation;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.context.SlingContextImpl;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * Goal which validates the specified RepoInit files by installing them into a
 * mock Sling repository. Any errors encountered will be reported and fail the
 * build.
 */
@Mojo(name = "validate", defaultPhase = LifecyclePhase.TEST, requiresProject = false)
public class ValidateMojo extends ParseMojo {

    /**
     * CND (compact node definitions) to load before running the validation.
     * Supports glob style patterns. All globs are evaluated relative to the
     * <code>${project.basedir}</code>
     * 
     * <a href="https://docs.oracle.com/javase/tutorial/essential/io/find.html">More
     * on Globs in Java</a>
     */
    @Parameter(property = "repoinit.nodeDefinitions", required = false)
    List<String> nodeDefinitions;

    /**
     * Initial content folders to load before running the RepoInit scripts.
     */
    @Parameter(property = "repoinit.contentFolders", required = false)
    List<ContentFolder> contentFolders;

    /**
     * The Maven Project
     */
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project = null;

    @Override
    public void execute()
            throws MojoExecutionException {
        getLog().info("Loading scripts from: " + scriptBaseDir);
        List<File> scripts = findScripts();

        if (scripts.isEmpty()) {
            return;
        }

        LinkedHashMap<String, List<Operation>> parsedScripts = new LinkedHashMap<>();
        for (File script : scripts) {
            getLog().debug("Parsing script: " + script.getAbsolutePath());
            parsedScripts.put(script.getAbsolutePath(), parseScript(script));
        }
        getLog().info("All scripts parsed successfully!");

        getLog().info("Setting up Sling context...");
        try (ValidatorContext context = new ValidatorContext()) {
            Session session = Optional.ofNullable(context.resourceResolver().adaptTo(Session.class))
                    .orElseThrow(() -> new MojoExecutionException("Failed to get Session from Sling Context"));

            if (nodeDefinitions != null && !nodeDefinitions.isEmpty()) {
                getLog().info("Loading nodetypes");
                loadNodeDefinitions(nodeDefinitions, session);
            }

            if (contentFolders != null && !contentFolders.isEmpty()) {
                getLog().info("Loading initial content");
                loadInitialContent(contentFolders, context);
            }

            JcrRepoInitOpsProcessor processor = new JcrRepoInitOpsProcessorImpl();
            for (Entry<String, List<Operation>> parsed : parsedScripts.entrySet()) {
                getLog().info("Executing script: " + parsed.getKey());
                try {
                    processor.apply(session, parsed.getValue());
                } catch (Exception e) {
                    throw new MojoExecutionException("Failed to execute script: " + parsed.getKey()
                            + ", Exception: " + e.toString(), e);
                }
            }
        } 
        getLog().info("All scripts executed successfully!");
    }

    void loadNodeDefinitions(@NotNull List<String> globs, @NotNull Session session) throws MojoExecutionException {
        File root = new File(".");
        List<File> cndFiles = findByGlob(root, globs);
        for (File cndFile : cndFiles) {
            getLog().info("Loading node types from: " + cndFile.getAbsolutePath());
            try {
                CndImporter.registerNodeTypes(new FileReader(cndFile), session);
            } catch (Exception e) {
                throw new MojoExecutionException(
                        "Failed to load CND file: " + cndFile.getAbsolutePath() + ", Exception: " + e.toString(), e);
            }
        }
    }

    void loadInitialContent(@NotNull List<ContentFolder> contentFolders, @NotNull ValidatorContext context)
            throws MojoExecutionException {
        for (ContentFolder contentFolder : contentFolders) {
            if (contentFolder.getFolder() == null || !contentFolder.getFolder().isDirectory()
                    || contentFolder.getPath() == null || contentFolder.getType() == null) {
                throw new MojoExecutionException("Invalid initial content folder: " + contentFolder);
            }
            if (contentFolder.getType() == Type.VLT_XML) {
                context.load(true).folderFileVaultXml(contentFolder.getFolder(), contentFolder.getPath());
            } else {
                context.load(true).folderJson(contentFolder.getFolder(), contentFolder.getPath());
            }
        }
    }

    class ValidatorContext extends SlingContextImpl implements AutoCloseable {
        ValidatorContext() {
            setResourceResolverType(ResourceResolverType.JCR_OAK);
            super.setUp();
        }

        @Override
        public void close() {
            super.tearDown();
        }

    }
}