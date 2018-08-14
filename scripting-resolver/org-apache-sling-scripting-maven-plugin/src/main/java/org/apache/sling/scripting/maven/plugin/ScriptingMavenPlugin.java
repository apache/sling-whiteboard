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
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mojo(name = "metadata", defaultPhase = LifecyclePhase.PACKAGE)
public class ScriptingMavenPlugin extends AbstractMojo
{

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Parameter(defaultValue = "${project.basedir}/src/main/resources/javax.script")
    private String scriptsDirectory;

    private static final Set<String> METHODS = new HashSet<>(Arrays.asList(new String[]{"TRACE", "OPTIONS", "GET", "HEAD", "POST", "PUT",
            "DELETE", "PATCH"}));

    private static final Set<String> FILE_SEPARATORS = new HashSet<>(Arrays.asList("\\", "/"));

    public void execute() throws MojoExecutionException
    {
        File sdFile = new File(scriptsDirectory);
        if (!sdFile.exists()) {
            sdFile = new File(project.getBasedir(), scriptsDirectory);
            if (!sdFile.exists()) {
                throw new MojoExecutionException("Cannot find file " + scriptsDirectory + ".");
            }
        }
        final AtomicReference<File> scriptsDirectoryReference = new AtomicReference<>();
        scriptsDirectoryReference.set(sdFile);;
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(sdFile);
        scanner.setIncludes("**");
        scanner.setExcludes("**/*.class");
        scanner.addDefaultExcludes();
        scanner.scan();

        List<String> scriptPaths = Stream.of(scanner.getIncludedFiles()).map(path -> new File(scriptsDirectoryReference.get(), path))
            .map(file -> file.getPath().substring((scriptsDirectoryReference.get().getPath() + File.pathSeparatorChar).length()))
            .collect(Collectors.toList());


        List<String> requires = new ArrayList<>();

        List<String> capabilities = new ArrayList<>();
        for (String scriptPath : scriptPaths)
        {
            Script script = getScripts(scriptPath);

            String capability = "sling.resourceType;sling.resourceType=\"" + script.rt.replace("\"", "\\\"") + "\"";

            if (!(script.rt.equals(script.name) || script.rt.endsWith("." + script.name) || script.name.isEmpty()))
            {
                if (!script.name.equalsIgnoreCase("requires"))
                {
                    if (!script.name.equalsIgnoreCase("extends"))
                    {
                        capability += ";sling.resourceType.selectors:List<String>=\"" + script.name.replace("\"", "\\\"") + "\"";
                    }
                    else
                    {
                        try (BufferedReader input = new BufferedReader(new FileReader(new File(scriptsDirectoryReference.get(), scriptPath))))
                        {
                            String extend = input.readLine();

                            capability += ";extends=\"" + extend.split(";")[0].replace("\"", "\\\"") + "\"";
                            requires.add(extend + ";extends=true" );
                        }
                        catch (Exception ex)
                        {
                            getLog().error(ex);
                        }
                    }
                }
                else
                {
                    try (BufferedReader input = new BufferedReader(new FileReader(new File(scriptsDirectoryReference.get(), scriptPath))))
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
            if (script.extension != null)
            {
                capability += ";sling.resourceType.extensions:List<String>=\"" + script.extension.replace("\"", "\\\"") + "\"";
            }

            if (script.method != null)
            {
                capability += ";sling.servlet.methods:List<String>=\"" + script.method.replace("\"", "\\\"") + "\"";
            }
            if (script.version != null)
            {
                capability += ";version:Version=\"" + script.version + "\"";
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
            if (parts.length > 2)
            {
                filter = "(&" + filter + "(!(sling.resourceType.selectors=*)))";
            }
            requirements.add("sling.resourceType;filter:=\"" + filter + "\"");
        }

        project.getProperties().setProperty(ScriptingMavenPlugin.class.getPackage().getName() + "." + Constants.PROVIDE_CAPABILITY, String.join(",", capabilities));
        project.getProperties().setProperty(ScriptingMavenPlugin.class.getPackage().getName() + "." + Constants.REQUIRE_CAPABILITY, String.join(",", requirements));
    }

    static class Script {
        String rt;
        String version;
        String name;
        String extension;
        String scriptExtension;
        String method;
    }

    static Script getScripts(String script) {
        String fileSeparator = null;
        for (String sep : FILE_SEPARATORS) {
            if (script.contains(sep)) {
                fileSeparator = sep;
                break;
            }
        }
        Script result = new Script();
        String[] parts = script.split(Pattern.quote(fileSeparator));

        result.rt = parts[0];
        result.version = parts.length > 2 ? new Version(parts[1]).toString() : null;
        result.name = parts.length > 2 ? parts[2] : parts[1];
        int idx = result.name.lastIndexOf('.');
        if (idx != -1)
        {
            result.scriptExtension = result.name.substring(idx + 1);
            result.name = result.name.substring(0, idx);
            if (result.scriptExtension.isEmpty())
            {
                result.scriptExtension = null;
            }
        }

        idx = result.name.lastIndexOf('.');
        if (idx != -1)
        {
            result.extension = result.name.substring(idx + 1);
            result.name = result.name.substring(0, idx);
            if (result.extension.isEmpty() || result.extension.equalsIgnoreCase("html"))
            {
                result.extension = null;
            }
        }
        else
        {
            result.extension = null;
        }

        idx = result.name.indexOf('.');
        if (idx != -1)
        {
            String methodString = result.name.substring(0, idx).toUpperCase();
            if (METHODS.contains(methodString))
            {
                result.method = methodString;
                result.name = result.name.substring(idx + 1);
            }
        }
        else if (METHODS.contains(result.name.toUpperCase()))
        {
            result.method = result.name.toUpperCase();
            result.name = "";
        }
        return result;
    }
}
