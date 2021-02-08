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

package org.apache.sling.contentmapper.api;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface AnnotationNames {
    String SLING_PREFIX = "sling:";
    String NAVIGABLE = SLING_PREFIX + "isNavigable";
    String DOCUMENT_ROOT = SLING_PREFIX + "documentRoot";
    String VISIT_CONTENT = SLING_PREFIX + "visitContent";
    String VISIT_CONTENT_RESOURCE_NAME_PATTERN = SLING_PREFIX + "visitContentResourceNamePattern";
    String CONTENT_INCLUDE_PROPERTY_REGEXP = SLING_PREFIX + "includePropertyRegexp";
    String CONTENT_EXCLUDE_PROPERTY_REGEXP = SLING_PREFIX + "excludePropertyRegexp";
    String NAVIGATION_PROPERTIES_LIST = SLING_PREFIX + "navigationPropertiesList";
}