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
import static org.apache.sling.capabilities.ProbeBuilder.DEF_SEPARATOR;
import org.osgi.service.component.annotations.Component;

/** A Probe that executes a script to find out if 
 *  specific capabilities are present.
 *  The capability value is the output of the script.
 */

@Component(service=ProbeBuilder.class)
public class JvmProbeBuilder implements ProbeBuilder {
    
    public static final String PREFIX = "jvm" + DEF_SEPARATOR;
    
    static class JvmProbe implements Probe {
        @Override
        public String getName() {
            return getClass().getSimpleName();
        }

        @Override
        public Map<String, String> getValues() throws Exception {
            // Return semi-useful JVM properties for our proof of concept
            final Map<String, String> result = new HashMap<>();
            
            final String [] props = {
                "java.specification.version",
                "java.vm.vendor",
                "java.vm.version"
            };
            
            for(String prop : props) {
                result.put(prop, System.getProperty(prop));
            }
            
            return result;
        }
    }
    
    @Override
    public Probe buildProbe(String definition) throws IllegalArgumentException {
        if(!definition.startsWith(PREFIX)) {
            return null;
        }
        
        return new JvmProbe();
    }
    
    @Override
    public String getPrefix() {
        return PREFIX;
    }
}