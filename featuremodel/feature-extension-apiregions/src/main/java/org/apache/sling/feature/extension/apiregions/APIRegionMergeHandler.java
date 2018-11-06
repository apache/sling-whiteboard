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
package org.apache.sling.feature.extension.apiregions;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.builder.HandlerContext;
import org.apache.sling.feature.builder.MergeHandler;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;

import static org.apache.sling.feature.extension.apiregions.AbstractHandler.API_REGIONS_NAME;
import static org.apache.sling.feature.extension.apiregions.AbstractHandler.EXPORTS_KEY;
import static org.apache.sling.feature.extension.apiregions.AbstractHandler.GLOBAL_NAME;
import static org.apache.sling.feature.extension.apiregions.AbstractHandler.NAME_KEY;
import static org.apache.sling.feature.extension.apiregions.AbstractHandler.ORG_FEATURE_KEY;

public class APIRegionMergeHandler implements MergeHandler {
    @Override
    public boolean canMerge(Extension extension) {
        return API_REGIONS_NAME.equals(extension.getName());
    }

    @Override
    public void merge(HandlerContext context, Feature target, Feature source, Extension targetEx, Extension sourceEx) {
        if (!sourceEx.getName().equals(API_REGIONS_NAME))
            return;
        if (targetEx != null && !targetEx.getName().equals(API_REGIONS_NAME))
            return;

        JsonReader srcJR = Json.createReader(new StringReader(sourceEx.getJSON()));
        JsonArray srcJA = srcJR.readArray();

        JsonArray tgtJA;
        if (targetEx != null) {
            JsonReader tgtJR = Json.createReader(new StringReader(targetEx.getJSON()));
            tgtJA = tgtJR.readArray();
        } else {
            targetEx = new Extension(sourceEx.getType(), sourceEx.getName(), sourceEx.isRequired());
            target.getExtensions().add(targetEx);

            tgtJA = Json.createArrayBuilder().build();
        }

        StringWriter sw = new StringWriter();
        JsonGenerator gen = Json.createGenerator(sw);
        gen.writeStartArray();
        for (JsonValue jv : tgtJA) {
            gen.write(jv);
        }

        Map<String, List<String>> inheritedPackages = new LinkedHashMap<>(); // keep the insertion order
        for (int i=0; i < srcJA.size(); i++) {
            gen.writeStartObject();
            JsonObject jo = srcJA.getJsonObject(i);
            boolean exportsWritten = false;
            if (!jo.containsKey(ORG_FEATURE_KEY)) {
                gen.write(ORG_FEATURE_KEY, source.getId().toMvnId());

                List<String> exports = new ArrayList<>();
                if (jo.containsKey(EXPORTS_KEY)) {
                    JsonArray ja = jo.getJsonArray(EXPORTS_KEY);
                    for (JsonValue jv : ja) {
                        if (jv instanceof JsonString) {
                            exports.add(((JsonString) jv).getString());
                        }
                    }
                }

                String name = jo.getString(NAME_KEY);
                if (!GLOBAL_NAME.equals(name)) {
                    ArrayList<String> localExports = new ArrayList<>(exports);
                    for (Map.Entry<String, List<String>> entry : inheritedPackages.entrySet()) {
                        entry.getValue().stream().filter(p -> !exports.contains(p)).forEach(exports::add);
                    }
                    inheritedPackages.put(name, localExports);

                    JsonArrayBuilder eab = Json.createArrayBuilder();
                    exports.stream().forEach(e -> eab.add(e));
                    gen.write(EXPORTS_KEY, eab.build());
                    exportsWritten = true;
                }
            }
            for (Map.Entry<String, JsonValue> entry : jo.entrySet()) {
                if (EXPORTS_KEY.equals(entry.getKey()) && exportsWritten)
                    continue;
                gen.write(entry.getKey(), entry.getValue());
            }
            gen.writeEnd();
        }

        gen.writeEnd();
        gen.close();

        targetEx.setJSON(sw.toString());
    }
}
