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

import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;

public class LoggerPrinterTest {

    private OsgiConfigurator osgiConfig;

    @Before
    public void init() {
        osgiConfig = TestHelper.mockOsgiConfigurator("info");
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.start();
    }

    @After
    public void reset() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.reset();
    }

    @Test
    public void testPrinter() {
        LoggerConfiguration loggerConfig = TestHelper.mockLoggerConfiguration("logs/test.log", "debug",
                new String[] { "com.text" });
        osgiConfig.bindLoggerConfiguration(loggerConfig);

        LoggerPrinter printer = new LoggerPrinter(osgiConfig);

        StringWriter sw = new StringWriter();
        printer.printConfiguration(new PrintWriter(sw));

        String written = sw.toString();

        assertTrue("Does not contain console header",
                written.contains(String.format("Sling Commons Log - JSON%n===========================")));
        assertTrue("Does not contain status header",
                written.contains(String.format("Status%n-------------------")));
        assertTrue("Does not contain attached loggers header",
                written.contains(String.format("Attached Loggers%n-------------------")));
        assertTrue("Does not contain attached loggers",
                written.contains(String.format(
                        "org.apache.sling.commons.log.json.impl.LoggerConfiguration~logs/test.log%n-------------------%n%nLog File: logs/test.log%nLog Log Level: debug%nDefined Logger Names: com.text%nAttached Logger Names: com.text")));
    }
}
