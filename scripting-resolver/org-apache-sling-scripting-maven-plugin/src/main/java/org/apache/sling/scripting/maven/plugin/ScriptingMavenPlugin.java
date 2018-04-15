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
package org.apache.sling.scripting.maven.plugin;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.io.DirectoryScanner;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mojo(name = "metadata", defaultPhase = LifecyclePhase.PACKAGE)
public class ScriptingMavenPlugin extends AbstractMojo
{

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession session;

    private final Set<String> METHODS = new HashSet<>(Arrays.asList(new String[]{"TRACE", "OPTIONS", "GET", "HEAD", "POST", "PUT", "DELETE"}));

    public void execute() throws MojoExecutionException
    {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(project.getBuild().getOutputDirectory());
        scanner.setIncludes("javax.script/**");
        scanner.setExcludes("**/*.class");
        scanner.addDefaultExcludes();
        scanner.scan();

        List<String> scripts = Stream.of(scanner.getIncludedFiles()).map(path -> new File(project.getBuild().getOutputDirectory(), path))
            .map(file -> file.getPath().substring((new File(project.getBuild().getOutputDirectory(), "javax.script").getPath() + File.pathSeparatorChar).length()))
            .collect(Collectors.toList());


        List<String> requires = new ArrayList<>();

        List<String> capabilities = new ArrayList<>();
        for (String script : scripts)
        {
            String[] parts = script.split("/");

            String rt = parts[0];
            String version = parts.length > 2 ? new Version(parts[1]).toString() : null;
            String name = parts.length > 2 ? parts[2] : parts[1];
            String scriptExtension;
            int idx = name.lastIndexOf('.');
            if (idx != -1)
            {
                scriptExtension = name.substring(idx + 1);
                name = name.substring(0, idx);
            }
            else
            {
                scriptExtension = null;
            }

            String extension;
            idx = name.lastIndexOf('.');
            if (idx != -1)
            {
                extension = name.substring(idx + 1);
                name = name.substring(0, idx);
            }
            else
            {
                extension = "html";
            }

            String method;
            idx = name.indexOf('.');
            if (idx != -1)
            {
                String methodString = name.substring(0, idx);
                if (METHODS.contains(methodString.toUpperCase()))
                {
                    method = methodString.toUpperCase();
                    name = name.substring(idx + 1);
                }
                else
                {
                    method = null;
                }
            }
            else if (METHODS.contains(name.toUpperCase()))
            {
                method = name.toUpperCase();
                name = "";
            }
            else
            {
                method = null;
            }

            String capability = "sling.resourceType;sling.resourceType=\"" + rt.replace("\"", "\\\"") + "\"";

            if (!(rt.equals(name) || rt.endsWith("." + name) || name.isEmpty()))
            {
                if (!name.equalsIgnoreCase("requires"))
                {
                    if (!name.equalsIgnoreCase("extends"))
                    {
                        capability += ";sling.resourceType.selectors:List<String>=\"" + name.replace("\"", "\\\"") + "\"";
                    }
                    else
                    {
                        try (BufferedReader input = new BufferedReader(new FileReader(new File(new File(project.getBuild().getOutputDirectory(), "javax.script"), script))))
                        {
                            String extend = input.readLine();

                            capability += ";extends=\"" + extend.split(";")[0].replace("\"", "\\\"") + "\"";
                            requires.add(extend);
                        }
                        catch (Exception ex)
                        {
                            getLog().error(ex);
                        }
                    }
                }
                else
                {
                    try (BufferedReader input = new BufferedReader(new FileReader(new File(new File(project.getBuild().getOutputDirectory(), "javax.script"), script))))
                    {
                        for (String line = input.readLine(); line != null; line = input.readLine())
                        {
                            requires.add(line);
                        }
                    }
                    catch (Exception ex)
                    {
                        getLog().error(ex);
                    }
                }
            }
            capability += ";sling.resourceType.extensions:List<String>=\"" + extension.replace("\"", "\\\"") + "\"";

            if (method != null)
            {
                capability += ";sling.servlet.methods:List<String>=\"" + method.replace("\"", "\\\"") + "\"";
            }
            if (version != null)
            {
                capability += ";version:Version=\"" + version + "\"";
            }
            capabilities.add(capability);
        }
        List<String> requirements = new ArrayList<>();
        for (String require : requires)
        {
            String[] parts = require.split(";");
            String rt = parts[0];
            String filter = "(sling.resourceType=\"" + rt.replace("\"", "\\\"") + "\")";

            if (parts.length > 1)
            {
                VersionRange range = new VersionRange(parts[1].substring(parts[1].indexOf("=") + 1).replace("\"", "").trim());
                filter = "(&" + filter + range.toFilterString("version") + ")";
            }
            requirements.add("sling.resourceType;filter:=\"" + filter + "\"");
        }

        project.getProperties().setProperty(ScriptingMavenPlugin.class.getPackage().getName() + "." + Constants.PROVIDE_CAPABILITY, String.join(",", capabilities));
        project.getProperties().setProperty(ScriptingMavenPlugin.class.getPackage().getName() + "." + Constants.REQUIRE_CAPABILITY, String.join(",", requirements));
    }
}
