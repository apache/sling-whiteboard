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
package org.apache.sling.graphql.schema.aggregator;

import static org.junit.Assert.fail;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/** Capture logs for testing */
public class LogCapture extends ListAppender<ILoggingEvent> implements Closeable {
    private final boolean verboseFailure;

    /** Setup the capture and start it */
    public LogCapture(String loggerName, boolean verboseFailure) {
        this.verboseFailure = verboseFailure;
        Logger logger = (Logger) LoggerFactory.getLogger(loggerName);
        logger.setLevel(Level.ALL);
        setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.addAppender(this);
        start();
    }

    public boolean anyMatch(Predicate<ILoggingEvent> p) {
        return this.list.stream().anyMatch(p);
    }

    public void assertContains(Level atLevel, String ... substrings) {
        Stream.of(substrings).forEach(substring -> {
            if(!anyMatch(event -> event.getLevel() == atLevel && event.getFormattedMessage().contains(substring))) {
                if(verboseFailure) {
                    fail(String.format("No log message contains [%s] in log\n%s", substring, this.list.toString()));
                } else {
                    fail(String.format("No log message contains [%s]", substring));
                }
            }
        });
    }

    @Override
    public void close() throws IOException {
        stop();
    }
}