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
import java.util.concurrent.ConcurrentHashMap;

/**
 * A requirement for a feature.
 * The requirement is modeled after an OSGi requirement: it
 * belongs to a namespace and might have attributes and / or
 * directives.
 */
public class Requirement {

    /*public static final String RESOLUTION_OPTIONAL = "optional";

    public static final String	RESOLUTION_DIRECTIVE = "resolution";*/

    /** The namspace. */
    private final String namespace;

    /** Map of attributes. */
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    /** Map of directives. */
    private final Map<String, Object> directives = new ConcurrentHashMap<>();

    /**
     * Create a new Requirement.
     * @param namespace The namespace
     * @throws IllegalArgumentException If namespace is {@code null}.
     */
    public Requirement(final String namespace) {
        if ( namespace == null ) {
            throw new IllegalArgumentException("namespace must not be null.");
        }
        this.namespace = namespace;
    }

    /**
     * The namespace
     * @return The namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Get the map of attributes.
     * The map is modifiable.
     * @return The map of attributes.
     */
    public Map<String,Object> getAttributes() {
        return attributes;
    }

    /**
     * Get the map of directives.
     * The map is modifiable.
     * @return The map of directives.
     */
    public Map<String,Object> getDirectives() {
        return directives;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + attributes.hashCode();
        result = prime * result + directives.hashCode();
        result = prime * result + namespace.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Requirement other = (Requirement) obj;
        if (!attributes.equals(other.attributes)
            || !directives.equals(other.directives)
            || !namespace.equals(other.namespace)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "[Requirement namespace='" + namespace + "' attributes=" + attributes + " directives=" + directives + "]";
    }
}
