/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sling.jsonstore.internal.api;

import java.util.HashSet;
import java.util.Set;

import org.apache.sling.api.resource.Resource;
import org.osgi.annotation.versioning.ProviderType;

/** Defines the behavior of this module for a given
 *  Resource, based on its path and other properties
 */
@ProviderType
public interface ResourceBehaviorProvider {

    public static class ResourceBehavior {
        private final String nonExistentResourceType;
        private final Set<String> allowedMethods;

        public ResourceBehavior(String nonExistentResourceType, String ... allowedMethodsArg) {
            this.nonExistentResourceType = nonExistentResourceType;
            this.allowedMethods = new HashSet<>();
            for(String m : allowedMethodsArg) {
                this.allowedMethods.add(m.toUpperCase());
            }
        }

        /** Resource type to use for non-existent resources, to
         *  map it to the right servlet.
         */
        public String getNonExistentResourceType() {
            return nonExistentResourceType;
        }

        public boolean isMethodAllowed(String method) {
            return allowedMethods.contains(method.toUpperCase());
        }
    }

    ResourceBehavior getBehavior(Resource r);
}