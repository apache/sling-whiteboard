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
package org.apache.sling.feature.support.json;

import org.apache.felix.configurator.impl.json.JSONUtil;
import org.apache.sling.feature.Configurations;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.Map;

/**
 * JSON Reader for configurations.
 */
public class ConfigurationJSONReader extends JSONReaderBase {

    /**
     * Read a map of configurations from the reader
     * The reader is not closed. It is up to the caller to close the reader.
     *
     * @param reader The reader for the configuration
     * @param location Optional location
     * @return The read configurations
     * @throws IOException If an IO errors occurs or the JSON is invalid.
     */
    public static Configurations read(final Reader reader, final String location)
    throws IOException {
        try {
            final ConfigurationJSONReader mr = new ConfigurationJSONReader(location);
            return mr.readConfigurations(reader);
        } catch (final IllegalStateException | IllegalArgumentException e) {
            throw new IOException(e);
        }
    }

    /**
     * Private constructor
     * @param location Optional location
     */
    ConfigurationJSONReader(final String location) {
        super(location);
    }

    Configurations readConfigurations(final Reader reader) throws IOException {
        final Configurations result = new Configurations();

        final JsonObject json = Json.createReader(new StringReader(minify(reader))).readObject();

        @SuppressWarnings("unchecked")
        final Map<String, Object> map = (Map<String, Object>) JSONUtil.getValue(json);

        final Map<String, Object> objMap = Collections.singletonMap(JSONConstants.FEATURE_CONFIGURATIONS, (Object)map);

        readConfigurations(objMap, result);

        return result;
    }
}


