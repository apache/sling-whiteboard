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
package org.apache.sling.feature.support.json;

import org.apache.sling.feature.Application;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Configurations;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import java.io.IOException;
import java.io.Writer;


/**
 * Simple JSON writer for an application
 */
public class ApplicationJSONWriter extends JSONWriterBase {

    /**
     * Writes the application to the writer.
     * The writer is not closed.
     * @param writer Writer
     * @param app The application
     * @throws IOException If writing fails
     */
    public static void write(final Writer writer, final Application app)
    throws IOException {
        final ApplicationJSONWriter w = new ApplicationJSONWriter();
        w.writeApp(writer, app);
    }

   private void writeApp(final Writer writer, final Application app)
    throws IOException {
        final JsonGenerator w = Json.createGenerator(writer);
        w.writeStartObject();

        // framework
        if ( app.getFramework() != null ) {
            w.write(JSONConstants.APP_FRAMEWORK, app.getFramework().toMvnId());
        }

        // features
        if ( !app.getFeatureIds().isEmpty() ) {
            w.writeStartArray(JSONConstants.APP_FEATURES);
            for(final ArtifactId id : app.getFeatureIds()) {
                w.write(id.toMvnId());
            }
            w.writeEnd();
        }

        // bundles
        writeBundles(w, app.getBundles(), app.getConfigurations());

        // configurations
        final Configurations cfgs = new Configurations();
        for(final Configuration cfg : app.getConfigurations()) {
            final String artifactProp = (String)cfg.getProperties().get(Configuration.PROP_ARTIFACT);
            if (  artifactProp == null ) {
                cfgs.add(cfg);
            }
        }
        writeConfigurations(w, cfgs);

        // framework properties
        writeFrameworkProperties(w, app.getFrameworkProperties());

        // extensions
        writeExtensions(w, app.getExtensions(), app.getConfigurations());

        w.writeEnd();
        w.flush();
    }
}
