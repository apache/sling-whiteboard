/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Licensed to the Apache Software Foundation (ASF) under one
 ~ or more contributor license agreements.  See the NOTICE file
 ~ distributed with this work for additional information
 ~ regarding copyright ownership.  The ASF licenses this file
 ~ to you under the Apache License, Version 2.0 (the
 ~ "License"); you may not use this file except in compliance
 ~ with the License.  You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package org.apache.sling.capabilities.internal;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.apache.sling.capabilities.CapabilitiesSource;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/** Test the JSONCapabilitiesWriter */
public class JSONCapabilitiesWriterTest {

    static class TestSource implements CapabilitiesSource {
        private final String namespace;
        private final Map<String, Object> props = new HashMap<>();
        
        TestSource(String namespace, int propsCount) {
            this.namespace = namespace;
            for(int i=0; i < propsCount; i++) {
                props.put("KEY_" + i + "_" + namespace, "VALUE_" + i + "_" + namespace);
            }
        }

        @Override
        public Map<String, Object> getCapabilities() throws Exception {
            return Collections.unmodifiableMap(props);
        }
        
        @Override
        public String getNamespace() {
            return namespace;
        }
        
    }
    
    @Test
    public void testWithTwoSources() throws IOException {
        final List<CapabilitiesSource> sources = new ArrayList<>();
        sources.add(new TestSource("A", 2));
        sources.add(new TestSource("B", 1));
        
        final StringWriter w = new StringWriter();
        new JSONCapabilitiesWriter().writeJson(w, sources);
        
        final JsonReader r = Json.createReader(new StringReader(w.toString()));
        final JsonObject rootJson = r.readObject();
        final JsonObject json = rootJson.getJsonObject(JSONCapabilitiesWriter.CAPS_KEY);
        assertEquals("VALUE_0_A", json.getJsonObject("A").getString("KEY_0_A"));
        assertEquals("VALUE_1_A", json.getJsonObject("A").getString("KEY_1_A"));
        assertEquals("VALUE_0_B", json.getJsonObject("B").getString("KEY_0_B"));
        
        assertEquals("Expecting 1 root key", 1, rootJson.keySet().size());
        assertEquals("Expecting 2 keys at A", 2, json.getJsonObject("A").keySet().size());
        assertEquals("Expecting 1 key at B", 1, json.getJsonObject("B").keySet().size());
    }
}