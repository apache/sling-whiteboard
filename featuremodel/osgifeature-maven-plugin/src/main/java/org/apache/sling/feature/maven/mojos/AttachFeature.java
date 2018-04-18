/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.feature.maven.mojos;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.io.json.FeatureJSONWriter;
import org.apache.sling.feature.maven.FeatureConstants;
import org.apache.sling.feature.maven.ProjectHelper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Attach the feature as a project artifact.
 */
@Mojo(name = "attach-feature",
      defaultPhase = LifecyclePhase.PACKAGE,
      requiresDependencyResolution = ResolutionScope.TEST,
      threadSafe = true
    )
public class AttachFeature extends AbstractFeatureMojo {

    private void attach(final Feature feature,
            final String artifactName,
            final String classifier)
    throws MojoExecutionException {
        if ( feature != null ) {

            // write the feature
            final File outputFile = new File(this.project.getBuild().getDirectory() + File.separatorChar + artifactName);
            outputFile.getParentFile().mkdirs();

            try ( final Writer writer = new FileWriter(outputFile)) {
                FeatureJSONWriter.write(writer, feature);
            } catch (final IOException e) {
                throw new MojoExecutionException("Unable to write feature to " + outputFile, e);
            }

            // if this project is a feature, it's the main artifact
            if ( project.getPackaging().equals(FeatureConstants.PACKAGING_FEATURE)
                 && (FeatureConstants.CLASSIFIER_FEATURE.equals(classifier))) {
                project.getArtifact().setFile(outputFile);
            } else {
                // otherwise attach it as an additional artifact
                projectHelper.attachArtifact(project, FeatureConstants.PACKAGING_FEATURE,
                    classifier, outputFile);
            }
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        attach(ProjectHelper.getFeature(this.project), FeatureConstants.FEATURE_ARTIFACT_NAME, FeatureConstants.CLASSIFIER_FEATURE);
        attach(ProjectHelper.getTestFeature(this.project), FeatureConstants.TEST_FEATURE_ARTIFACT_NAME, FeatureConstants.CLASSIFIER_TEST_FEATURE);
    }
}
