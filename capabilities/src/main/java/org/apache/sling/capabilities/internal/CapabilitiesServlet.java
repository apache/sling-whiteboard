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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import javax.servlet.ServletException;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.felix.utils.json.JSONWriter;

@SlingServlet(
        resourceTypes = {"sling/capabilities"},
        selectors = {"capabilities"},
        methods = "GET",
        extensions = "json"
)
public class CapabilitiesServlet extends SlingSafeMethodsServlet {
    
    public final static String PROBE_PROP_SUFFIX = "_probe";
    public final static String CAPS_KEY = "org.apache.sling.capabilities";
    private final ProbeFactory factory = new ProbeFactory();
    
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        final JSONWriter jw = new JSONWriter(response.getWriter());
        jw.object();
        jw.key(CAPS_KEY);
        jw.object();
        
        for(String def : getProbeDefinitions(request.getResource())) {
            final Probe p = factory.buildProbe(def);
            String value = null;
            try {
                value = p.getValue();
            } catch(Exception e) {
                value = "EXCEPTION:" + e.getClass().getName() + ":" + e.getMessage();
            }
            jw.key(p.getName());
            jw.value(value);
        }
        
        jw.endObject();
        jw.endObject();
        response.getWriter().flush();
    }
    
    Collection<String> getProbeDefinitions(Resource r) {
        final ArrayList<String> result = new ArrayList();
        final ValueMap vm = r.adaptTo(ValueMap.class);
        if(vm != null) {
            for(String key : vm.keySet()) {
                if(key.endsWith(PROBE_PROP_SUFFIX)) {
                    result.add(vm.get(key, String.class));
                }
            }
        }
        return result;
    }
}