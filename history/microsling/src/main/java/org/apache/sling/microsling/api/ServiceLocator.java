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
package org.apache.sling.microsling.api;

import org.apache.sling.microsling.api.exceptions.SlingException;

/** Retrieve named services from a Services registry.
 *  Sling OSGi, makes extensive use of this interface to
 *  locate OSGi services.
 *  Microsling also uses it, but that's mostly to do things 
 *  the same way as in Sling OSGi, as services are statically
 *  defined in microsling. 
 */

public interface ServiceLocator {
    
    /** Lookup a single service
     * @param serviceName The name (interface) of the service.
     * @return The service instance, or null if the service is not available.
     */
    Object getService(String serviceName);
    
    /** Same as getService, but throws a ServiceNotAvailableException if the requested
     *  service is not currently available
     */
    Object getRequiredService(String serviceName) throws ServiceNotAvailableException;

    /** Lookup one or several services
     * @param serviceName The name (interface) of the service.
     * @param filter An optional filter (LDAP-like, see OSGi spec)
     * @return The services object or null.
     */
    Object[] getServices(String serviceName, String filter) throws InvalidServiceFilterSyntaxException;
    
    /** Thrown when an invalid service filter is used */ 
    public static class InvalidServiceFilterSyntaxException extends SlingException {
        private static final long serialVersionUID = 1L;

        public InvalidServiceFilterSyntaxException(String reason) {
            super(reason);
        }
        
        public InvalidServiceFilterSyntaxException(String reason, Throwable cause) {
            super(reason,cause);
        }
    };    

    /** Thrown when a required service is not found */
    public static class ServiceNotAvailableException extends SlingException {
        private static final long serialVersionUID = 1L;

        public ServiceNotAvailableException(String reason) {
            super(reason);
        }
        
        public ServiceNotAvailableException(String reason, Throwable cause) {
            super(reason,cause);
        }
    };    
    


}