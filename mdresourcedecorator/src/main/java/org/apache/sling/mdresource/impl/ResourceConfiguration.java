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
package org.apache.sling.mdresource.impl;

public class ResourceConfiguration {

    /**
     * Source for markdown
     *
     */
    public enum SourceType {
        InputStream, // the resource is adapted to an InputStream
        Property     // the resource has a string property
    }

    /** The source type for the markdown */
    public SourceType sourceType;

    /** The name of the property containing the markdown if type is "Property" */
    public String sourceMarkdownProperty;

    /** The resource type for the md resource */
    public String resourceType;

    /** The property holding the markdown (optional) */
    public String markdownProperty;

    /** The property holding the rendered html */
    public String htmlProperty;

    /** The property holding the first title (optional) */
    public String titleProperty;

    /** Rewrite links */
    public boolean rewriteLinks;
}
