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
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.felix.utils.json.JSONWriter;
import org.apache.sling.capabilities.CapabilitiesSource;

/** Create the JSON output of our servlet */
class JSONCapabilitiesWriter {

    static final String CAPS_KEY = "org.apache.sling.capabilities";
    
    /** Write JSON to the supplied Writer, using the supplied sources */
    void writeJson(Writer w, Collection<CapabilitiesSource> sources) throws IOException {
        final JSONWriter jw = new JSONWriter(w);
        jw.object();
        jw.key(CAPS_KEY);
        jw.object();
        
        Map<String, String> values = null;
        for(CapabilitiesSource s : sources) {
            try {
                values = s.getCapabilities();
            } catch(Exception e) {
                values = new HashMap<>();
                values.put("_EXCEPTION_", e.getClass().getName() + ":" + e.getMessage());
            }
            jw.key(s.getNamespace());
            jw.object();
            for(Map.Entry<String, String> e : values.entrySet()) {
                jw.key(e.getKey());
                jw.value(e.getValue());
            }
            jw.endObject();
        }
        
        jw.endObject();
        jw.endObject();
    }
}