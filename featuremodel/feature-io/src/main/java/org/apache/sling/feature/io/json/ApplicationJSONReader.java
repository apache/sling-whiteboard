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
package org.apache.sling.feature.io.json;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;

import org.apache.felix.configurator.impl.json.JSONUtil;
import org.apache.sling.feature.Application;
import org.apache.sling.feature.ArtifactId;

/**
 * This class offers a method to read an {@code Application} using a {@code Reader} instance.
 */
public class ApplicationJSONReader extends JSONReaderBase {

    /**
     * Read a new application from the reader
     * The reader is not closed. It is up to the caller to close the reader.
     *
     * @param reader The reader for the feature
     * @return The application
     * @throws IOException If an IO errors occurs or the JSON is invalid.
     */
    public static Application read(final Reader reader)
    throws IOException {
        try {
            final ApplicationJSONReader mr = new ApplicationJSONReader();
            mr.readApplication(reader);
            return mr.app;
        } catch (final IllegalStateException | IllegalArgumentException e) {
            throw new IOException(e);
        }
    }

    /** The read application. */
    private final Application app;

    /**
     * Private constructor
     */
    private ApplicationJSONReader() {
        super(null);
        this.app = new Application();
    }

    /**
     * Read a full application
     * @param reader The reader
     * @throws IOException If an IO error occurs or the JSON is not valid.
     */
    private void readApplication(final Reader reader)
    throws IOException {
        final JsonObject json = Json.createReader(new StringReader(minify(reader))).readObject();

        @SuppressWarnings("unchecked")
        final Map<String, Object> map = (Map<String, Object>) JSONUtil.getValue(json);

        final String frameworkId = this.getProperty(map, JSONConstants.APP_FRAMEWORK);
        if ( frameworkId != null ) {
            app.setFramework(ArtifactId.parse(frameworkId));
        }
        this.readBundles(map, app.getBundles(), app.getConfigurations());
        this.readFrameworkProperties(map, app.getFrameworkProperties());
        this.readConfigurations(map, app.getConfigurations());

        this.readExtensions(map,
                JSONConstants.APP_KNOWN_PROPERTIES,
                this.app.getExtensions(), this.app.getConfigurations());
    }
}


