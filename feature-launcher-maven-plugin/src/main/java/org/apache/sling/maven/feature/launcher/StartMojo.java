/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.maven.feature.launcher;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

@Mojo( name = "start", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST )
public class StartMojo
    extends AbstractMojo
{
    /**
     * Location of the file.
     */
    @Parameter( defaultValue = "${project.build.directory}", property = "outputDir", required = true )
    private File outputDirectory;
    
    // TODO - support multiple dependencies and also have proper parameter names
    @Parameter(required = true)
    private Dependency toLaunch;

    @Parameter
    private Map<String, String> frameworkProperties = new HashMap<String, String>();

    @Component
    private ArtifactResolver resolver;

    @Parameter(property = "project", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(property = "session", readonly = true, required = true)
    protected MavenSession mavenSession;
    
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        
        try {
            RepositorySystemSession repositorySession = mavenSession.getRepositorySession();
            Artifact artifact = toArtifact(toLaunch);
            
            ArtifactResult result = resolver.resolveArtifact(repositorySession, new ArtifactRequest(artifact, null, null));
            File featureFile = result.getArtifact().getFile();

            
            // TODO - this should be inferred from the plugin's pom.xml dependency (not the project's pom.xml)
            Dependency featureLauncherDep = project.getDependencies().stream()
                .filter( d -> d.getGroupId().equals("org.apache.sling") && d.getArtifactId().equals("org.apache.sling.feature.launcher"))
                .findFirst()
                .orElseThrow( () -> new MojoFailureException("No feature launcher dependency found"));
            
            Artifact launcherArtifact = toArtifact(featureLauncherDep);
            File launcher = resolver
                .resolveArtifact(repositorySession, new ArtifactRequest(launcherArtifact, null, null))
                .getArtifact()
                .getFile();
            
            File workDir = new File(outputDirectory, "launchers");
            workDir.mkdirs();
            
            List<String> args = new ArrayList<>();
            args.add(System.getenv("JAVA_HOME") + File.separatorChar + "bin" + File.separatorChar + "java");
            args.add("-jar");
            args.add(launcher.getAbsolutePath());
            args.add("-f");
            args.add(featureFile.getAbsolutePath());
            
            for ( var frameworkProperty : frameworkProperties.entrySet() ) {
                args.add("-D");
                args.add(frameworkProperty.getKey()+"="+frameworkProperty.getValue());
            }
            
            // TODO - add support for all arguments supported by the feature launcher
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.inheritIO();
            pb.directory(workDir);
            Process process = pb.start();
            
            // TODO - reliably stop started processes in case 'stop' is not invoked
            
            Processes.set(process);
        } catch (ArtifactResolutionException | IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private Artifact toArtifact(Dependency dependency) {
        return new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getClassifier(), dependency.getType(), dependency.getVersion());
    }
}
