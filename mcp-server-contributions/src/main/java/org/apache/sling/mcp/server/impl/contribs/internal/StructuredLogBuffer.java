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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Pattern;

import ch.qos.logback.classic.Level;

public class StructuredLogBuffer {

    private final Object lock = new Object();
    private final Deque<LogSnapshot> entries = new ArrayDeque<>();
    private int maxEntriesKept;

    public StructuredLogBuffer(int maxEntriesKept) {
        this.maxEntriesKept = Math.max(1, maxEntriesKept);
    }

    public void append(LogSnapshot snapshot) {
        synchronized (lock) {
            entries.addLast(snapshot);
            trimToSize();
        }
    }

    public List<LogSnapshot> getRecent(Pattern pattern, Level minLevel, int maxEntries) {
        synchronized (lock) {
            List<LogSnapshot> matches = new ArrayList<>();
            int remaining = Math.max(1, maxEntries);

            for (var iterator = entries.descendingIterator(); iterator.hasNext() && remaining > 0; ) {
                LogSnapshot snapshot = iterator.next();
                if (!matches(snapshot, pattern, minLevel)) {
                    continue;
                }
                matches.add(snapshot);
                remaining--;
            }

            return matches;
        }
    }

    private boolean matches(LogSnapshot snapshot, Pattern pattern, Level minLevel) {
        if (snapshot.level().isGreaterOrEqual(minLevel)) {
            if (pattern == null) {
                return true;
            }
            return matchesField(pattern, snapshot.level() != null ? snapshot.level().levelStr : null)
                    || matchesField(pattern, snapshot.loggerName())
                    || matchesField(pattern, snapshot.threadName())
                    || matchesField(pattern, snapshot.formattedMessage())
                    || matchesField(pattern, snapshot.throwableText())
                    || matchesMdc(pattern, snapshot);
        }
        return false;
    }

    private boolean matchesMdc(Pattern pattern, LogSnapshot snapshot) {
        if (snapshot.mdc().isEmpty()) {
            return false;
        }
        for (var entry : snapshot.mdc().entrySet()) {
            if (matchesField(pattern, entry.getKey()) || matchesField(pattern, entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesField(Pattern pattern, String value) {
        return value != null && !value.isEmpty() && pattern.matcher(value).find();
    }

    private void trimToSize() {
        while (entries.size() > maxEntriesKept) {
            entries.removeFirst();
        }
    }
}
