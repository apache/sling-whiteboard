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
package org.apache.sling.repositorymaintainance;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Configuration to configure how version cleanup works on a per-path basis
 */
@ObjectClassDefinition(name = "%version.cleanup.path.name", description = "%version.cleanup.path.description", localization = "OSGI-INF/l10n/bundle")
public @interface VersionCleanupPathConfig {

    @AttributeDefinition(name = "%version.path.name", description = "%version.path.description")
    String path();

    @AttributeDefinition(name = "%version.limit.name", description = "%version.limit.description")
    int limit();

    @AttributeDefinition(name = "%version.keepVersions.name", description = "%version.keepVersions.description")
    boolean keepVersions();

}
