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

import java.util.HashMap;

/** Metadata for the Resource, can hold content-related metadata 
 *  (last modification time, etc.) as well as Sling-specific 
 *  metadata (path used to resolve the Resource, etc.)
 */
public class ResourceMetadata extends HashMap<String, Object> {
    private static final long serialVersionUID = 4529624506305380870L;

    /** Use this prefix for Sling-specific metadata */
    public final static String SLING_PREFIX = "sling.";

    /** Set by the ResourceResolver when resolving the Resource 
     *  @see ResourceResolver
     */ 
    public final static String RESOLUTION_PATH = SLING_PREFIX + "resolutionPath";
}
