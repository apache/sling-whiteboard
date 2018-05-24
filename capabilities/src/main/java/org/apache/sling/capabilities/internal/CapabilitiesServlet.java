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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.felix.utils.json.JSONWriter;
import org.apache.sling.capabilities.Probe;
import org.apache.sling.capabilities.ProbeBuilder;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

@Component(service = Servlet.class,
property = {
    "sling.servlet.resourceTypes=sling/capabilities",
    "sling.servlet.methods=GET",
    "sling.servlet.selectors=capabilities",
    "sling.servlet.extensions=json"
})

public class CapabilitiesServlet extends SlingSafeMethodsServlet {
    
    @Reference(
        policy=ReferencePolicy.DYNAMIC,
        cardinality=ReferenceCardinality.AT_LEAST_ONE, 
        policyOption=ReferencePolicyOption.GREEDY)
    volatile List<ProbeBuilder> builders;

    public final static String PROBE_PROP_SUFFIX = "_probe";
    public final static String CAPS_KEY = "org.apache.sling.capabilities";
    
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        final JSONWriter jw = new JSONWriter(response.getWriter());
        jw.object();
        jw.key(CAPS_KEY);
        jw.object();
        
        for(String def : getProbeDefinitions(request.getResource())) {
            Map<String, String> values = null;
            Probe p = null;
            for(ProbeBuilder b : builders) {
                p = b.buildProbe(def);
                if(p != null) {
                    try {
                        values = p.getValues();
                    } catch(Exception e) {
                        values = new HashMap<>();
                        values.put("_EXCEPTION_", e.getClass().getName() + ":" + e.getMessage());
                    }
                    break;
                }
            }
            if(p != null && values != null) {
                jw.key(p.getName());
                jw.object();
                for(Map.Entry<String, String> e : values.entrySet()) {
                    jw.key(e.getKey());
                    jw.value(e.getValue());
                }
                jw.endObject();
            }
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