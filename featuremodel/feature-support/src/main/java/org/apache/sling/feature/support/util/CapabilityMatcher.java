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
package org.apache.sling.feature.support.util;

import org.apache.felix.utils.resource.CapabilitySet;
import org.osgi.resource.Capability;


public class CapabilityMatcher
{
    static boolean matches(Capability cap, SimpleFilter sf)
    {
        // Translate the SimpleFilter into the one from Felix Utils.
        // Once the SimpleFilter has moved to Felix Utils, this class can be removed.
        org.apache.felix.utils.resource.SimpleFilter sf2 =
                org.apache.felix.utils.resource.SimpleFilter.parse(sf.toString());

        return CapabilitySet.matches(cap, sf2);
    }
}
