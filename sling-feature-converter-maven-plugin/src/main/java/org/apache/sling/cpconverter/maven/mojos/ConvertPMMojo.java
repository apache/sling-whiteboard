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
package org.apache.sling.cpconverter.maven.mojos;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.sling.feature.modelconverter.ProvisioningToFeature;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts the given list of Provisioning Models into
 * Feature Models
 */
@Mojo(
    name = "convert-pm",
    requiresProject = true,
    threadSafe = true
)
public final class ConvertPMMojo
    extends AbstractBaseMojo
{
    public static final String CFG_INPUT_FOLDER = "inputFolder";

    public static final String CFG_OUTPUT_FOLDER = "outputFolder";

    public static final String CFG_GROUP_ID = "groupId";

    public static final String CFG_ARTIFACT_ID = "artifactId";

    public static final String CFG_VERSION = "version";

    public static final String CFG_FRAMEWORK_PROPERTIES = "frameworkProperties";

    public static final String CFG_NO_PROVISIIONING_MODEL_NAME = "noProvisioningModelName";

    public static final String CFG_EXCLUDE_BUNDLES = "excludeBundles";

    public static final String CFG_RUN_MODES = "runModes";

    public static final String DEFAULT_OUTPUT_FOLDER = "target/features";

    /**
     * The folder that contains the Provisioning Models
     */
    @Parameter(property = CFG_INPUT_FOLDER, required = true)
    private File inputFolder;

    /**
     * The folder that will contain the created Feature Models relative to the Maven Basedir Folder
     */
    @Parameter(property = CFG_OUTPUT_FOLDER, defaultValue = DEFAULT_OUTPUT_FOLDER, required = true)
    private String outputFolder;

    /**
     * If provided this becomes the Feature's Group Id
     */
    @Parameter(property = CFG_GROUP_ID)
    private String groupId;

    /**
     * If provided this becomes the Feature's Artifact Id. Tis will also
     * make the original name to become the classifier
     */
    @Parameter(property = CFG_ARTIFACT_ID)
    private String artifactId;

    /**
     * If provided this becomes the Feature's Version
     */
    @Parameter(property = CFG_VERSION)
    private String version;

    /**
     * Franework Properties to be added to a given Feature Model.
     * The format looks like this:
     * 'launchpad:felix.systempackages.substitution=true' where
     * launchpad is the name of the PM
     */
    @Parameter(property = CFG_FRAMEWORK_PROPERTIES)
    private List<String> frameworkProperties;

    /**
     * If set to {@code true} the converted Content Package will be installed in the local Maven Repository
     */
    @Parameter(property = CFG_NO_PROVISIIONING_MODEL_NAME, defaultValue = "true")
    private boolean noProvisioningModelName;

    /**
     * List of Bundles to be excluded from the Feature Model files
     */
    @Parameter(property = CFG_EXCLUDE_BUNDLES)
    private List<String> excludeBundles;

    /**
     * List of all Runmodes that are considered for this conversion
     *
     * The default and all parts of all runmodes listed are placed into
     * the Feature Models, all others are excluded
     */
    @Parameter(property = CFG_RUN_MODES)
    private List<String> runModes;

    private Pattern pattern = Pattern.compile("^(.*?):(.*?)=(.*?)$");

    private String checkForPlaceholder(String text) {
        String answer = text;
        if(answer != null) {
            answer = answer.replaceAll("\\$\\{\\{", "\\$\\{");
            answer = answer.replaceAll("}}", "}");
            getLog().info("Replaced Old Artifact Id Override: '" + text + "', with new one: '" + answer + "'");
        }
        return answer;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!inputFolder.isDirectory()) {
            throw new MojoFailureException("Input Folder is not a directory: " + inputFolder);
        }
        if (!inputFolder.canRead()) {
            throw new MojoFailureException("Input Folder is not readable:" + inputFolder);
        }
        String outputFolderPath = project.getBasedir() + "/" + outputFolder;
        File output = new File(outputFolderPath);
        if (!output.exists()) {
            if(!output.mkdirs()) {
                throw new MojoFailureException("Could not create Output Folder: " + outputFolder);
            }
        }
        if (!output.isDirectory()) {
            throw new MojoFailureException("Output Folder is not a directory: " + output);
        }
        if (!output.canWrite()) {
            throw new MojoFailureException("Output Folder is not writable: " + output);
        }
        List<File> provisioningFiles = Arrays.asList(inputFolder.listFiles());
        Map<String, Object> options = new HashMap<>();
        if (groupId != null && !groupId.isEmpty()) {
            groupId = checkForPlaceholder(groupId);
            options.put("groupId", groupId);
        }
        if (version != null && !version.isEmpty()) {
            version = checkForPlaceholder(version);
            options.put("version", version);
        }
        // Todo: do we have a way to check the name?
        if (artifactId != null && !artifactId.isEmpty()) {
            artifactId = checkForPlaceholder(artifactId);
            options.put("name", artifactId);
        }
        options.put("useProvidedVersion", version != null && !version.isEmpty());
        options.put("noProvisioningModelName", noProvisioningModelName);
        frameworkProperties = trimList(frameworkProperties);
        Map<String, Map<String, String>> frameworkPropertiesMap = new HashMap<>();
        for (String value : frameworkProperties) {
            // Separate Model from Property Name Value pair. Create Sub Map if needed and then add
            getLog().info("Check Add Framework Properties Line: " + value);
            Matcher matcher = pattern.matcher(value);
            if (matcher.matches() && matcher.groupCount() == 3) {
                String modelName = matcher.group(1);
                String propName = matcher.group(2);
                String propValue = matcher.group(3);
                getLog().info("Model Name: " + modelName + ", Prop Name: " + propName + ", Value: " + propValue);
                Map<String, String> modelMap = frameworkPropertiesMap.get(modelName);
                if (modelMap == null) {
                    modelMap = new HashMap<>();
                    frameworkPropertiesMap.put(modelName, modelMap);
                }
                modelMap.put(propName, propValue);
            }
        }
        options.put("addFrameworkProperties", frameworkPropertiesMap);
        excludeBundles = trimList(excludeBundles);
        options.put("excludeBundles", excludeBundles);
        getLog().info("Excluded Bundles: " + excludeBundles);
        runModes = trimList(runModes);
        options.put("runModes", runModes);
        getLog().info("Runmodes: " + runModes);

        // Start the Conversion
        for (File file : provisioningFiles) {
            getLog().info("Handle File: " + file.getAbsolutePath());
            ProvisioningToFeature.convert(file, output, options);
        }
    }

    // Because of the XML structure run modes can have leading, trailing spaces so we need to trim them here
    public List<String> trimList(List<String> list) {
        List<String> answer = new ArrayList<>();
        if(list != null) {
            for(String entry: list) {
                if(entry != null) {
                    entry = entry.trim();
                    if(!entry.isEmpty()) {
                        answer.add(entry);
                    }
                }
            }
        }
        return answer;
    }
}
