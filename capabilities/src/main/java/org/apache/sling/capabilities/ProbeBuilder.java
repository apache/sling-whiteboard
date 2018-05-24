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

import org.osgi.annotation.versioning.ProviderType;

/** Service that builds a Probe if it gets a suitable definition */
@ProviderType
public interface ProbeBuilder {
    
    String DEF_SEPARATOR = ":";
    
    /**
     * @param definition A Probe definition in a syntax that this services supports
     * @return null if the definition doesn't start with our prefix followed by a colon
     * @throws IllegalArgumentException if the definition starts with our prefix
     *      but is otherwise invalid
     */
    Probe buildProbe(String definition) throws IllegalArgumentException;
    
    /** The prefix which definitions must start with (followed by
     *  our DEF_SEPARATOR) to be considered by this builder.
     * @return our prefix
     */
    String getPrefix();
}
