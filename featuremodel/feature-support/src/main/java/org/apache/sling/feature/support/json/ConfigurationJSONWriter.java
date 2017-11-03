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

import org.apache.sling.feature.Configurations;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import java.io.IOException;
import java.io.Writer;


/**
 * JSON writer for configurations
 */
public class ConfigurationJSONWriter extends JSONWriterBase {

    /**
     * Writes the configurations to the writer.
     * The writer is not closed.
     * @param writer Writer
     * @param configs List of configurations
     * @throws IOException If writing fails
     */
    public static void write(final Writer writer, final Configurations configs)
    throws IOException {
        final ConfigurationJSONWriter w = new ConfigurationJSONWriter();
        w.writeConfigurations(writer, configs);
    }

    private void writeConfigurations(final Writer writer, final Configurations configs)
    throws IOException {
        final JsonGenerator w = Json.createGenerator(writer);
        w.writeStartObject();

        writeConfigurationsMap(w, configs);

        w.writeEnd();
        w.flush();
    }
}
