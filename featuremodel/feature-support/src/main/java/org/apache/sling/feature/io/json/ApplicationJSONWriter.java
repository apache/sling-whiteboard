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
package org.apache.sling.feature.io.json;

import org.apache.sling.feature.Application;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Configurations;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

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
       JsonObjectBuilder ob = Json.createObjectBuilder();

        // framework
        if ( app.getFramework() != null ) {
            ob.add(JSONConstants.APP_FRAMEWORK, app.getFramework().toMvnId());
        }

        // features
        if ( !app.getFeatureIds().isEmpty() ) {
            JsonArrayBuilder featuresArr = Json.createArrayBuilder();

            for(final ArtifactId id : app.getFeatureIds()) {
                featuresArr.add(id.toMvnId());
            }
            ob.add(JSONConstants.APP_FEATURES, featuresArr.build());
        }

        // bundles
        writeBundles(ob, app.getBundles(), app.getConfigurations());

        // configurations
        final Configurations cfgs = new Configurations();
        for(final Configuration cfg : app.getConfigurations()) {
            final String artifactProp = (String)cfg.getProperties().get(Configuration.PROP_ARTIFACT);
            if (  artifactProp == null ) {
                cfgs.add(cfg);
            }
        }
        writeConfigurations(ob, cfgs);

        // framework properties
        writeFrameworkProperties(ob, app.getFrameworkProperties());

        // extensions
        writeExtensions(ob, app.getExtensions(), app.getConfigurations());

        JsonWriterFactory writerFactory = Json.createWriterFactory(
                Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true));
        JsonWriter jw = writerFactory.createWriter(writer);
        jw.writeObject(ob.build());
        jw.close();
    }
}
