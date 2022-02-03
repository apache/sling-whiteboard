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

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.BasicStatusManager;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.util.StatusPrinter;

/**
 * Console plugin for printing the logback status and registered custom loggers
 */
@Component(property = {
        "felix.webconsole.label=log_json",
        "felix.webconsole.title=" + LoggerPrinter.HEADLINE,
        "felix.webconsole.configprinter.modes=always",

}, service = LoggerPrinter.class)
public class LoggerPrinter extends BasicStatusManager {

    private final LoggerContext loggerContext;
    private final OsgiConfigurator configurator;

    static final String HEADLINE = "Sling Commons Log - JSON";

    @Activate
    public LoggerPrinter(@Reference OsgiConfigurator configurator) {
        loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.getStatusManager().getCopyOfStatusList().forEach(this::add);
        loggerContext.setStatusManager(this);
        this.configurator = configurator;
    }

    private void renderHeader(PrintWriter pw, String header) {
        pw.println("\n\n" + header + "\n-------------------\n");
    }

    private String statusToString(Status status) {
        StringBuilder sb = new StringBuilder();
        StatusPrinter.buildStr(sb, "", status);
        return sb.toString();
    }

    /**
     * Print out the logging status
     * 
     * @see org.apache.felix.webconsole.ConfigurationPrinter#printConfiguration(java.io.PrintWriter)
     */
    public void printConfiguration(PrintWriter pw) {

        pw.println(HEADLINE + "\n===========================");

        renderHeader(pw, "Status");
        this.getCopyOfStatusList().stream().map(this::statusToString).forEach(pw::println);

        renderHeader(pw, "Attached Loggers");
        configurator.getAttachedLoggers().entrySet().forEach(e -> {
            renderHeader(pw, e.getKey().getPid());
            pw.println("Log File: " + e.getKey().getConfig().org_apache_sling_commons_log_file());
            pw.println("Log Log Level: " + e.getKey().getConfig().org_apache_sling_commons_log_level());
            pw.println("Defined Logger Names: "
                    + Arrays.stream(e.getKey().getConfig().org_apache_sling_commons_log_names())
                            .collect(Collectors.joining(",")));
            pw.println(
                    "Attached Logger Names: "
                            + e.getValue().stream().map(Logger::getName).collect(Collectors.joining(",")));
        });

    }

}
