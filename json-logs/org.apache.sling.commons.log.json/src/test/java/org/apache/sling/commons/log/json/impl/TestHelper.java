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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

public class TestHelper {

    private TestHelper() {
    };

    public static OsgiConfigurator mockOsgiConfigurator(String appenderName) {

        OsgiConfigurator osgiConfigurator = mock(OsgiConfigurator.class);
        when(osgiConfigurator.isInitialized()).thenReturn(true);

        OsgiConfigurator.Config config = new OsgiConfigurator.Config() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public String appenderName() {
                return appenderName;
            }

        };

        when(osgiConfigurator.getConfig()).thenReturn(config);

        Map<LoggerConfiguration, List<Logger>> attachedLoggers = new ConcurrentHashMap<>();
        doAnswer((inv) -> {
            LoggerConfiguration lc = inv.getArgumentAt(0, LoggerConfiguration.class);
            attachedLoggers.put(lc,
                    Arrays.stream(lc.getConfig().org_apache_sling_commons_log_names()).map(TestHelper::getLogger)
                            .collect(Collectors.toList()));
            return null;
        }).when(osgiConfigurator).bindLoggerConfiguration(any(LoggerConfiguration.class));
        when(osgiConfigurator.getAttachedLoggers()).thenReturn(attachedLoggers);

        return osgiConfigurator;
    }

    public static LogManager mockLogManager(String level) {

        LogManager logManager = mock(LogManager.class);

        LogManager.Config config = new LogManager.Config() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public String org_apache_sling_commons_log_level() {
                return level;
            }

            @Override
            public int org_apache_sling_commons_log_maxCallerDataDepth() {
                return 7;
            }

            @Override
            public boolean org_apache_sling_commons_log_packagingDataEnabled() {
                return true;
            }
        };

        when(logManager.getConfig()).thenReturn(config);
        return logManager;
    }

    public static Logger getLogger(String name) {
        return (Logger) LoggerFactory.getLogger(name);
    }

    public static LoggerConfiguration mockLoggerConfiguration(String file, String level, String[] names) {

        LoggerConfiguration loggerConfiguration = mock(LoggerConfiguration.class);
        when(loggerConfiguration.getPid()).thenReturn(LoggerConfiguration.class.getName() + "~" + file);

        LoggerConfiguration.Config config = createLoggerConfigurationConfig(file, level, names);
        when(loggerConfiguration.getConfig()).thenReturn(config);

        return loggerConfiguration;
    }

    public static LoggerConfiguration.Config createLoggerConfigurationConfig(String file, String level,
            String[] names) {
        return new LoggerConfiguration.Config() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public String org_apache_sling_commons_log_file() {
                return file;
            }

            @Override
            public String org_apache_sling_commons_log_level() {
                return level;
            }

            @Override
            public String[] org_apache_sling_commons_log_names() {
                return names;
            }

        };
    }
}
