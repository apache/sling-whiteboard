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
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.apiregions.ApiRegion;
import org.apache.sling.feature.apiregions.ApiRegions;

/**
 * <code>api-regions</code> JSON format serializer implementation.
 */
public final class ApiRegionsJSONSerializer implements JSONConstants {

    private static final JsonGeneratorFactory GENERATOR_FACTORY = Json.createGeneratorFactory(Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true));

    private ApiRegionsJSONSerializer() {
        // this class must not be instantiated from outside
    }

    /**
     * Serializes the input <code>api-regions</code> to the target stream.
     *
     * @param apiRegions the <code>api-regions</code> has to be serialized
     * @param output the target stream where serializing the <code>api-regions</code>
     */
    public static void serializeApiRegions(ApiRegions apiRegions, OutputStream output) {
        requireNonNull(output, "Impossible to serialize api-regions to a null stream");
        serializeApiRegions(apiRegions, new OutputStreamWriter(output));
    }

    /**
     * Serializes the input <code>api-regions</code> to the target writer.
     *
     * @param apiRegions the <code>api-regions</code> has to be serialized
     * @param writer the target writer where serializing the <code>api-regions</code>
     */
    public static void serializeApiRegions(ApiRegions apiRegions, Writer writer) {
        requireNonNull(apiRegions, "Impossible to serialize null api-regions");
        requireNonNull(writer, "Impossible to serialize api-regions to a null stream");

        JsonGenerator generator = GENERATOR_FACTORY.createGenerator(writer)
                                                   .writeStartArray();

        for (ApiRegion apiRegion : apiRegions) {
            generator.writeStartObject()
                     .write(NAME_KEY, apiRegion.getName())
                     .writeStartArray(EXPORTS_KEY);

            for (String api : apiRegion.getExports()) {
                generator.write(api);
            }

            generator.writeEnd().writeEnd();
        }

        generator.writeEnd().close();
    }

    /**
     * Maps the input <code>api-regions</code> to the <code>api-regions</code> Feature Model Extension.
     *
     * @param apiRegions the <code>api-regions</code> has to be serialized
     * @return the mapped <code>api-regions</code> Feature Model Extension
     */
    public static Extension serializeApiRegions(ApiRegions apiRegions) {
        StringWriter stringWriter = new StringWriter();
        serializeApiRegions(apiRegions, stringWriter);

        Extension apiRegionsExtension = new Extension(ExtensionType.JSON, API_REGIONS_KEY, false);
        apiRegionsExtension.setJSON(stringWriter.toString());
        return apiRegionsExtension;
    }

    /**
     * Maps the input <code>api-regions</code> to the <code>api-regions</code> Feature Model Extension
     * and add it to the target Feature.
     *
     * @param apiRegions the <code>api-regions</code> has to be serialized
     * @param feature the target Feature where adding the extension
     */
    public static void serializeApiRegions(ApiRegions apiRegions, Feature feature) {
        requireNonNull(feature, "Impossible to serialize api-regions to a null Feature");
        Extension apiRegionsExtension = serializeApiRegions(apiRegions);
        feature.getExtensions().add(apiRegionsExtension);
    }

}
