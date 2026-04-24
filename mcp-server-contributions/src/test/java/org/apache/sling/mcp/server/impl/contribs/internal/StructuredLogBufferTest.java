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
import java.util.regex.Pattern;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StructuredLogBufferTest {

    @Test
    void keepsOnlyNewestEntriesWithinCapacity() {
        StructuredLogBuffer buffer = new StructuredLogBuffer(2);

        buffer.append(snapshot(1L, Level.INFO, "first"));
        buffer.append(snapshot(2L, Level.INFO, "second"));
        buffer.append(snapshot(3L, Level.INFO, "third"));

        List<LogSnapshot> logs = buffer.getRecent(null, Level.TRACE, 10);
        assertEquals(
                List.of("third", "second"),
                logs.stream().map(LogSnapshot::formattedMessage).toList());
    }

    @Test
    void filtersByLevelAndRegex() {
        StructuredLogBuffer buffer = new StructuredLogBuffer(10);

        buffer.append(snapshot(1L, Level.DEBUG, "debug trace"));
        buffer.append(snapshot(2L, Level.INFO, "first user ok"));
        buffer.append(snapshot(3L, Level.ERROR, "first user failure"));

        List<LogSnapshot> logs = buffer.getRecent(Pattern.compile("first", Pattern.CASE_INSENSITIVE), Level.INFO, 10);

        assertEquals(
                List.of("first user failure", "first user ok"),
                logs.stream().map(LogSnapshot::formattedMessage).toList());
    }

    private LogSnapshot snapshot(long timeMillis, Level level, String message) {
        return new LogSnapshot(timeMillis, level, "logger", "thread", message, null, Map.of());
    }
}
