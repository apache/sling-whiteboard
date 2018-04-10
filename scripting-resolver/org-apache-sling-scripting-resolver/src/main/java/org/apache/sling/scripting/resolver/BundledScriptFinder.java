/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Licensed to the Apache Software Foundation (ASF) under one
 ~ or more contributor license agreements.  See the NOTICE file
 ~ distributed with this work for additional information
 ~ regarding copyright ownership.  The ASF licenses this file
 ~ to you under the Apache License, Version 2.0 (the
 ~ "License"); you may not use this file except in compliance
 ~ with the License.  You may obtain a copy of the License at
 ~
 ~   http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package org.apache.sling.scripting.resolver;

import java.io.IOException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.Bundle;

@ProviderType
public interface BundledScriptFinder {

    /**
     * Given a {@code request}, this method finds the most appropriate bundled script to execute, taking into account the {@code
     * scriptExtensions} priority.
     *
     * @param request          the request for which the script has to be found
     * @param bundle           the bundle in which the scripts for the current resource type are packed (see
     *                         {@link SlingHttpServletRequest#getResource()})
     * @return the script to execute
     * @throws IOException if the script cannot be located
     */
    Script getScript(SlingHttpServletRequest request, Bundle bundle) throws IOException;

}
