/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.feature;

import java.util.Map;

import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

/**
 * Implementation of the OSGi Requirement interface.
 */
public class OSGiRequirement extends AbstractCapabilityRequirement implements Requirement {
    /**
     * Create a requirement that is not associated with a resource.
     * @param res The resource associated with the requirement.
     * @param ns The namespace of the requirement.
     * @param attrs The attributes of the requirement.
     * @param dirs The directives of the requirement.
     */
    public OSGiRequirement(String ns, Map<String, Object> attrs, Map<String, String> dirs) {
        this(null, ns, attrs, dirs);
    }

    /**
     * Create a requirement.
     * @param res The resource associated with the requirement.
     * @param ns The namespace of the requirement.
     * @param attrs The attributes of the requirement.
     * @param dirs The directives of the requirement.
     */
    public OSGiRequirement(Resource res, String ns, Map<String, Object> attrs, Map<String, String> dirs) {
        super(res, ns, attrs, dirs);
    }

    /**
     * Create a requirement based on an existing requirement, providing the resource.
     * The namespace, attributes and directives are copied from the provided requirement.
     * @param resource The resource to be associated with the requirement
     * @param requirement The requirement to base the new requirement on.
     */
    public OSGiRequirement(Resource resource, Requirement requirement) {
        this(resource, requirement.getNamespace(), requirement.getAttributes(), requirement.getDirectives());
    }
}
