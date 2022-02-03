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

import java.util.Optional;

import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

/**
 * OSGi Service for loading logger-specific configurations
 */
@Component(name = "org.apache.sling.commons.log.LogManager.factory.config", service = LoggerConfiguration.class)
@Designate(ocd = LoggerConfiguration.Config.class, factory = true)
public class LoggerConfiguration {

    private final Config config;
    private final String pid;

    @Activate
    public LoggerConfiguration(Config config, ComponentContext componentContext) {
        this.config = config;
        pid = Optional.ofNullable(componentContext.getProperties().get(Constants.SERVICE_PID)).map(String.class::cast)
                .orElse(getClass().getName() + "@" + System.identityHashCode(this));
    }

    public Config getConfig() {
        return config;
    }

    public String getPid() {
        return pid;
    }

    @ObjectClassDefinition(name = "%log.factory.config.name", description = "%log.factory.config.description", localization = "OSGI-INF/l10n/metatype")
    public @interface Config {

        @AttributeDefinition(name = "%log.file.name", description = "%log.file.description")
        public String org_apache_sling_commons_log_file() default "logs/errorlog";

        @AttributeDefinition(name = "%log.level.name", description = "%log.level.description", options = {
                @Option(value = "off", label = "Off"),
                @Option(value = "trace", label = "Trace"),
                @Option(value = "debug", label = "Debug"),
                @Option(value = "info", label = "Information"),
                @Option(value = "warn", label = "Warnings"),
                @Option(value = "error", label = "Error") })
        public String org_apache_sling_commons_log_level() default "info";

        @AttributeDefinition(name = "%log.loggers.name", description = "%log.loggers.description")
        public String[] org_apache_sling_commons_log_names();

    }

}
