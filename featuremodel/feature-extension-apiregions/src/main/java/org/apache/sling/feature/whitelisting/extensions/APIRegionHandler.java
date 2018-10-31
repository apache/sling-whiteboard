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
package org.apache.sling.feature.whitelisting.extensions;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.builder.HandlerContext;
import org.apache.sling.feature.builder.MergeHandler;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;

public class APIRegionHandler implements MergeHandler {

    @Override
    public boolean canMerge(Extension extension) {
        return "api-regions".equals(extension.getName());
    }

    @Override
    public void merge(HandlerContext context, Feature target, Feature source, Extension targetEx, Extension sourceEx) {
        if (!targetEx.getName().equals("api-regions") || !sourceEx.getName().equals("api-regions"))
            return;

        JsonReader srcJR = Json.createReader(new StringReader(sourceEx.getJSON()));
        JsonArray srcJA = srcJR.readArray();

        JsonReader tgtJR = Json.createReader(new StringReader(targetEx.getJSON()));
        JsonArray tgtJA = tgtJR.readArray();

        StringWriter sw = new StringWriter();
        JsonGenerator gen = Json.createGenerator(sw);
        gen.writeStartArray();
        for (JsonValue jv : tgtJA) {
            gen.write(jv);
        }

        for (int i=0; i < srcJA.size(); i++) {
            gen.writeStartObject();
            JsonObject jo = srcJA.getJsonObject(i);
            for (Map.Entry<String, JsonValue> entry : jo.entrySet()) {
                gen.write(entry.getKey(), entry.getValue());
            }
            gen.write("org-feature", source.getId().toMvnId());
            gen.writeEnd();
        }

        gen.writeEnd();
        gen.close();

        targetEx.setJSON(sw.toString());
    }
}
