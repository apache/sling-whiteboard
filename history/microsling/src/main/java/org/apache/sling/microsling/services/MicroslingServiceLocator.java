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
package org.apache.sling.microsling.services;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.microsling.api.ResourceResolver;
import org.apache.sling.microsling.api.ServiceLocator;
import org.apache.sling.microsling.resource.MicroslingResourceResolver;

/** Poor man's ServiceLocator (no, poorer than that) which uses a 
 *  static list of services. This is mostly meant to introduce
 *  the ServiceLocator interface in microsling.
 *  See Sling OSGi for the real McCoy.
 */

public class MicroslingServiceLocator implements ServiceLocator {

    protected final Map <String,Object> services = new HashMap <String, Object> ();
    
    public MicroslingServiceLocator() {
        // initialize our services (pure rocket science, isn't it? ;-)
        services.put(ResourceResolver.class.getName(),new MicroslingResourceResolver());
    }
    
    public Object getService(String serviceName) {
        return services.get(serviceName);
    }
    
    
    public Object getRequiredService(String serviceName) throws ServiceNotAvailableException {
        final Object result = services.get(serviceName);
        if(result== null) {
            throw new ServiceNotAvailableException("Service '" + serviceName + "' is not available");
        }
        return result;
    }

    public Object[] getServices(String serviceName, String filter) {
        // we don't have this feature
        return null;
    }

}
