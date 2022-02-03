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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

public class OsgiConfiguratorTest {

    @Rule
    public final OsgiContext context = new OsgiContext();

    @Before
    public void init() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.start();
    }

    @After
    public void reset() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.reset();
    }

    @Test
    public void tes() throws InterruptedException {

        Map<String, Object> loggerConfiguration = new HashMap<>();
        loggerConfiguration.put("org.apache.sling.commons.log.level", "debug");
        loggerConfiguration.put("org.apache.sling.commons.log.file", "logs/test.log");
        loggerConfiguration.put("org.apache.sling.commons.log.names", new String[] { "org.apache.sling" });
        context.registerInjectActivateService(LoggerConfiguration.class,
                loggerConfiguration);

        context.registerInjectActivateService(LogManager.class,
                Collections.singletonMap("org.apache.sling.commons.log.level", "warn"));

        context.registerInjectActivateService(OsgiConfigurator.class, new OsgiConfigurator(),
                Collections.singletonMap("appenderName", "console"));

        OsgiConfigurator configurator = context.getService(OsgiConfigurator.class);

        TimeUnit.SECONDS.sleep(1);

        Map<String, Object> loggerConfiguration2 = new HashMap<>();
        loggerConfiguration.put("org.apache.sling.commons.log.level", "debug");
        loggerConfiguration.put("org.apache.sling.commons.log.file", "logs/another.log");
        loggerConfiguration.put("org.apache.sling.commons.log.names", new String[] { "org.apache.felix" });
        context.registerInjectActivateService(LoggerConfiguration.class,
                loggerConfiguration2);

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        assertEquals(Level.WARN, rootLogger.getLevel());

        assertTrue(configurator.isInitialized());

        assertEquals(2, configurator.getAttachedLoggers().size());
    }
}
