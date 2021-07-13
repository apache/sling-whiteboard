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
package org.apache.sling.thumbnails.internal;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Configuration for the FileSettingsImpl
 */
@ObjectClassDefinition(name = "%thumbnailsupport.name", description = "%thumbnailsupport.description", localization = "OSGI-INF/l10n/bundle")
public @interface ThumbnailSupportConfig {

    @AttributeDefinition(name = "%thumbnailsupport.supportedTypes.name", description = "%thumbnailsupport.supportedTypes.description", defaultValue = {
            "nt:file=jcr:content/jcr:mimeType" })
    String[] supportedTypes() default { "nt:file=jcr:content/jcr:mimeType" };

    @AttributeDefinition(name = "%thumbnailsupport.persistableTypes.name", description = "%thumbnailsupport.persistableTypes.description")
    String[] persistableTypes() default {};

    @AttributeDefinition(name = "%transformservlet.errorResourcePath.name", description = "%transformservlet.errorResourcePath.description")
    String errorResourcePath() default "/static/sling-cms/thumbnails/file.png";

    @AttributeDefinition(name = "%transformservlet.errorSuffix.name", description = "%transformservlet.errorSuffix.description")
    String errorSuffix() default "/sling-cms-thumbnail.png";

}
