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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonGenerator;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import net.logstash.logback.composite.AbstractJsonProvider;

/**
 * Appends the property org.apache.sling.commons.log.file to loggers based on
 * the custom logger mappings.
 */
public class LogFileSourceJsonProvider extends AbstractJsonProvider<ILoggingEvent> {

    private Map<String, String> loggersToFiles = Collections.emptyMap();

    public void setAttachedLoggers(Map<LoggerConfiguration, List<Logger>> attachedLoggers) {
        addInfo("Updating attached providers");
        Map<String, String> newMapping = new HashMap<>();
        attachedLoggers.entrySet().forEach(al -> al.getValue().stream().map(Logger::getName)
                .forEach(l -> newMapping.put(l, al.getKey().getConfig().org_apache_sling_commons_log_file())));
        loggersToFiles = Collections.unmodifiableMap(newMapping);
        addInfo("Attached providers updated");
    }

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        if (isStarted()) {
            Optional<String> logFile = loggersToFiles.entrySet().stream()
                    .filter(e -> event.getLoggerName().startsWith(e.getKey())).findAny()
                    .map(Entry::getValue);
            if (logFile.isPresent()) {
                generator.writeStringField("org.apache.sling.commons.log.file", logFile.get());
            }
        }
    }

}
