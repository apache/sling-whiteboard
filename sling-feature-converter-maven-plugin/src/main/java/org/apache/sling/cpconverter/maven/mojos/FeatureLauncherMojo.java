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
import org.apache.sling.feature.launcher.impl.Main;

import java.io.File;

/**
 * Launches the given Feature File
 */
@Mojo(
    name = "launch-features",
    requiresProject = true,
    threadSafe = true
)
public final class FeatureLauncherMojo
    extends AbstractBaseMojo
{

    public static final String CFG_FEATURE_FILE = "featureFile";

    /**
     * The path of the file that is launched here
     */
    @Parameter(property = CFG_FEATURE_FILE, required = true)
    private File featureFile;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if(!featureFile.isFile()) {
            throw new MojoFailureException("Feature File is not a file: " + featureFile);
        }
        if(!featureFile.canRead()) {
            throw new MojoFailureException("Feature File is cannot be read: " + featureFile);
        }
        Main.main(new String[] {"-f", featureFile.getAbsolutePath()});
    }
}
