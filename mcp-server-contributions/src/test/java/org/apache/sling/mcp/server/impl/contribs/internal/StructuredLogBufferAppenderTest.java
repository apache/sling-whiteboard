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
package org.apache.sling.mcp.server.impl.contribs.internal;

import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.Test;
import org.osgi.util.converter.Converters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StructuredLogBufferAppenderTest {

    @Test
    void appenderSnapshotsFormattedMessageAndThrowable() {
        StructuredLogBufferAppender appender = new StructuredLogBufferAppender(configuration(5));

        LoggerContext context = new LoggerContext();
        appender.setContext(context);
        Logger logger = context.getLogger("test.logger");
        RuntimeException failure = new RuntimeException("error");
        LoggingEvent event = new LoggingEvent(getClass().getName(), logger, Level.ERROR, "message", failure, null);
        event.setMDCPropertyMap(java.util.Map.of());
        event.setThreadName("worker-1");

        appender.append(event);

        List<LogSnapshot> logs = appender.getBuffer().getRecent(null, Level.TRACE, 10);
        assertEquals(1, logs.size());
        assertEquals("message", logs.get(0).formattedMessage());
        assertEquals("worker-1", logs.get(0).threadName());
        assertNotNull(logs.get(0).throwableText());
    }

    private StructuredLogBufferAppender.Configuration configuration(int maxEntries) {
        return Converters.standardConverter()
                .convert(Map.of("maxEntries", maxEntries))
                .to(StructuredLogBufferAppender.Configuration.class);
    }
}
