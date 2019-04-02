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
package org.apache.sling.feature.apiregions.io.json;

import static java.util.Objects.requireNonNull;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collections;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

import org.apache.sling.feature.apiregions.ApiRegion;
import org.apache.sling.feature.apiregions.ApiRegions;

public final class ApiRegionsJSONSerializer implements JSONConstants {

    private static final JsonGeneratorFactory GENERATOR_FACTORY = Json.createGeneratorFactory(Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true));

    private ApiRegionsJSONSerializer() {
        // this class must not be instantiated from outside
    }

    public static void serializeApiRegions(ApiRegions apiRegions, OutputStream output) {
        requireNonNull(output, "Impossible to serialize api-regions to a null stream");
        serializeApiRegions(apiRegions, new OutputStreamWriter(output));
    }

    public static void serializeApiRegions(ApiRegions apiRegions, Writer writer) {
        requireNonNull(apiRegions, "Impossible to serialize null api-regions");
        requireNonNull(writer, "Impossible to serialize api-regions to a null stream");

        JsonGenerator generator = GENERATOR_FACTORY.createGenerator(writer);
        generator.writeStartArray();

        for (ApiRegion apiRegion : apiRegions) {
            generator.writeStartObject();

            generator.write(NAME_KEY, apiRegion.getName());

            generator.writeStartArray(EXPORTS_KEY);
            for (String api : apiRegion.getExports()) {
                generator.write(api);
            }
            generator.writeEnd();

            generator.writeEnd();
        }

        generator.writeEnd();
        generator.close();
    }

}
