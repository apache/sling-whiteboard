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
package org.apache.sling.commons.log.json.impl;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@Component(immediate = true, service = LogManager.class, name = "org.apache.sling.commons.log.LogManager")
@Designate(ocd = OsgiConfigurator.Config.class)
public class LogManager {

    private final Config config;

    @Activate
    public LogManager(Config config) {
        this.config = config;
    }

    public Config getConfig() {
        return config;
    }

    @ObjectClassDefinition(name = "%log.name", description = "%log.description", localization = "OSGI-INF/l10n/metatype")
    public @interface Config {
        @AttributeDefinition(name = "%log.level.name", description = "%log.level.description", options = {
                @Option(value = "off", label = "Off"),
                @Option(value = "trace", label = "Trace"),
                @Option(value = "debug", label = "Debug"),
                @Option(value = "info", label = "Information"),
                @Option(value = "warn", label = "Warnings"),
                @Option(value = "error", label = "Error") })
        String org_apache_sling_commons_log_level() default "info";

        @AttributeDefinition(name = "%log.config.maxCallerDataDepth.name", description = "%log.config.maxCallerDataDepth.description")
        int org_apache_sling_commons_log_maxCallerDataDepth() default 7;

        @AttributeDefinition(name = "%log.config.packagingData.name", description = "%log.config.packagingData.description")
        boolean org_apache_sling_commons_log_packagingDataEnabled() default true;
    }
}
