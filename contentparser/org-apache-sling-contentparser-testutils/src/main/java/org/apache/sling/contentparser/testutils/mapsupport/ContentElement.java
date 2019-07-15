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
package org.apache.sling.contentparser.testutils.mapsupport;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * Implements support for a {@link org.apache.sling.contentparser.api.ContentHandler} parsed resource to use during
 * {@link org.apache.sling.contentparser.api.ContentParser} tests.
 */
public final class ContentElement {

    private final String name;
    private final Map<String, Object> properties;
    private final Map<String, ContentElement> children = new LinkedHashMap<>();

    ContentElement(String name, Map<String, Object> properties) {
        this.name = name;
        this.properties = properties;
    }

    /**
     * Returns the name of the resource.
     *
     * @return resource name; the root resource has no name (null).
     */
    public String getName() {
        return name;
    }

    /**
     * Properties of this resource.
     *
     * @return this resource's properties (keys, values)
     */
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Returns the children of this resource. The Map preserves the children's ordering.
     *
     * @return the children of this resource (child names, child objects)
     */
    public Map<String, ContentElement> getChildren() {
        return children;
    }

    /**
     * Returns the child with the specified {@code path}.
     *
     * @param path relative path to address child or one of its descendants (use "/" as hierarchy separator)
     * @return child or {@code null} if no child was found for the specified {@code path}
     */
    public ContentElement getChild(String path) {
        String name = StringUtils.substringBefore(path, "/");
        ContentElement child = children.get(name);
        if (child == null) {
            return null;
        }
        String remainingPath = StringUtils.substringAfter(path, "/");
        if (StringUtils.isEmpty(remainingPath)) {
            return child;
        } else {
            return child.getChild(remainingPath);
        }
    }

}
