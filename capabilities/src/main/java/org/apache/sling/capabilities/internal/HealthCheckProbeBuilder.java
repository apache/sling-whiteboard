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

import java.util.HashMap;
import java.util.Map;
import org.apache.sling.capabilities.Probe;
import org.apache.sling.capabilities.ProbeBuilder;
import org.osgi.service.component.annotations.Component;

/** A Probe that executes Health Checks to find out if 
 *  specific capabilities are present.
 *  The capability value is "true" if all HCs are OK,
 *  "false" otherwise.
 * 
 *  TODO move to a separate module to minimize dependencies of the 
 *  core capabilities module.
 */
@Component(service=ProbeBuilder.class)
public class HealthCheckProbeBuilder implements ProbeBuilder {
    public static final String PREFIX = "hc" + DEF_SEPARATOR;
    
    static class HcProbe implements Probe {
        private final String name;
        private final String tags;
        
        HcProbe(String name, String tags) {
            this.name = name;
            this.tags = tags;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Map<String, String> getValues() throws Exception {
            final Map<String, String> result = new HashMap<> ();
            result.put("TODO", "would run HCs with tags " + tags);
            return result;
        }
    }
    
    @Override
    public Probe buildProbe(String definition) throws IllegalArgumentException {
        if(!definition.startsWith(PREFIX)) {
            return null;
        }
        
        final String [] parts = definition.split(DEF_SEPARATOR);
        if(parts.length != 3) {
            throw new IllegalArgumentException("Invalid definition:" + definition);
        }
        return new HcProbe(parts[1], parts[2]);
    }
    
    @Override
    public String getPrefix() {
        return PREFIX;
    }
}
