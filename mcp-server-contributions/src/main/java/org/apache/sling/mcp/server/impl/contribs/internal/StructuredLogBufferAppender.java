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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.LinkedHashMap;
import java.util.Map;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(
        service = {Appender.class, StructuredLogBufferAppender.class},
        property = {
            "loggers=ROOT",
            "service.description=Structured in-memory MCP log appender",
            "service.vendor=The Apache Software Foundation"
        })
@Designate(ocd = StructuredLogBufferAppender.Configuration.class)
public class StructuredLogBufferAppender extends AppenderBase<ILoggingEvent> {

    // Forward compatibility with logback 1.5+, where IThrowableProxy may expose getOverridingMessage().
    private static final MethodHandle GET_OVERRIDING_MESSAGE = findGetOverridingMessage();

    @ObjectClassDefinition(name = "Apache Sling MCP Structured Log Buffer")
    public @interface Configuration {

        @AttributeDefinition(name = "Max entries")
        int maxEntries() default 10000;
    }

    private final StructuredLogBuffer buffer;

    @Activate
    public StructuredLogBufferAppender(Configuration configuration) {
        buffer = new StructuredLogBuffer(configuration.maxEntries());
        setName("mcp-structured-log-buffer");
    }

    public StructuredLogBuffer getBuffer() {
        return buffer;
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (eventObject == null) {
            return;
        }

        buffer.append(new LogSnapshot(
                eventObject.getTimeStamp(),
                eventObject.getLevel(),
                eventObject.getLoggerName(),
                eventObject.getThreadName(),
                eventObject.getFormattedMessage(),
                getThrowableText(eventObject),
                copyMdc(eventObject)));
    }

    private Map<String, String> copyMdc(ILoggingEvent eventObject) {
        Map<String, String> mdc = eventObject.getMDCPropertyMap();
        if (mdc == null || mdc.isEmpty()) {
            return Map.of();
        }
        return new LinkedHashMap<>(mdc);
    }

    private String getThrowableText(ILoggingEvent eventObject) {
        IThrowableProxy throwableProxy = eventObject.getThrowableProxy();
        if (throwableProxy == null) {
            return null;
        }

        StringBuilder text = new StringBuilder();
        appendThrowable(text, throwableProxy, null);
        return text.toString();
    }

    private void appendThrowable(StringBuilder text, IThrowableProxy throwableProxy, String prefix) {
        if (prefix != null) {
            text.append(prefix);
        }
        text.append(getThrowableHeader(throwableProxy)).append('\n');

        StackTraceElementProxy[] stackTrace = throwableProxy.getStackTraceElementProxyArray();
        if (stackTrace != null) {
            int framesToRender = Math.max(0, stackTrace.length - Math.max(0, throwableProxy.getCommonFrames()));
            for (int i = 0; i < framesToRender; i++) {
                text.append('\t').append(stackTrace[i]).append('\n');
            }
            if (throwableProxy.getCommonFrames() > 0) {
                text.append("\t... ")
                        .append(throwableProxy.getCommonFrames())
                        .append(" common frames omitted")
                        .append('\n');
            }
        }

        IThrowableProxy[] suppressed = throwableProxy.getSuppressed();
        if (suppressed != null) {
            for (IThrowableProxy suppressedThrowable : suppressed) {
                appendThrowable(text, suppressedThrowable, "Suppressed: ");
            }
        }

        IThrowableProxy cause = throwableProxy.getCause();
        if (cause != null) {
            appendThrowable(text, cause, "Caused by: ");
        }
    }

    private String getThrowableHeader(IThrowableProxy throwableProxy) {
        String overridingMessage = getOverridingMessage(throwableProxy);
        if (overridingMessage != null && !overridingMessage.isEmpty()) {
            return overridingMessage;
        }

        StringBuilder header = new StringBuilder(throwableProxy.getClassName());
        String message = throwableProxy.getMessage();
        if (message != null && !message.isEmpty()) {
            header.append(": ").append(message);
        }
        return header.toString();
    }

    private String getOverridingMessage(IThrowableProxy throwableProxy) {
        if (GET_OVERRIDING_MESSAGE == null) {
            return null;
        }

        try {
            Object overridingMessage = GET_OVERRIDING_MESSAGE.invoke(throwableProxy);
            return overridingMessage instanceof String ? (String) overridingMessage : null;
        } catch (Throwable e) {
            return null;
        }
    }

    private static MethodHandle findGetOverridingMessage() {
        try {
            return MethodHandles.publicLookup()
                    .findVirtual(IThrowableProxy.class, "getOverridingMessage", MethodType.methodType(String.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            return null;
        }
    }
}
