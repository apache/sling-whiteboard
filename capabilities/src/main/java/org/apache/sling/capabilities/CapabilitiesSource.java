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
package org.apache.sling.capabilities;

import java.util.Map;
import org.osgi.annotation.versioning.ProviderType;

/** A CapabilitiesSource provides capabilities, as a Map of key/value
 *  pairs. 
 *  Various types of CapabilitiesSources are meant
 *  to be implemented, using Health Checks, OSGi environment status
 *  or any suitable input to find out which capabilities are present.
 */
@ProviderType
public interface CapabilitiesSource {
    
    /** @return the namespace to use to group our capabilities.
     *  That name must be unique in a given Sling instance.
     */
    String getNamespace();
    
    /** @return zero to N capabilities, each being represented by
     *      a key/value pair.
     * @throws Exception if the capabilities could not be computed.
     */
    Map<String, Object> getCapabilities() throws Exception;
}