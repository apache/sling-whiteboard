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
package org.apache.sling.mcp.server.impl.contribs;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import ch.qos.logback.classic.Level;
import io.modelcontextprotocol.json.McpJsonMapperSupplier;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.apache.sling.mcp.server.impl.contribs.internal.LogSnapshot;
import org.apache.sling.mcp.server.impl.contribs.internal.StructuredLogBufferAppender;
import org.apache.sling.mcp.server.spi.McpServerContribution;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * MCP Tool that provides access to logs with filtering capabilities.
 * Allows filtering by regex pattern, log level, and maximum number of entries.
 */
@Component
public class LogToolContribution implements McpServerContribution {

    @Reference
    private StructuredLogBufferAppender structuredLogBufferAppender;

    @Reference
    private McpJsonMapperSupplier jsonMapper;

    private static final int DEFAULT_MAX_LOGS = 200;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public List<SyncToolSpecification> getSyncToolSpecification() {

        var schema = """
                {
                  "type" : "object",
                  "id" : "urn:jsonschema:LogFilterInput",
                  "properties" : {
                    "regex" : {
                      "type" : "string",
                      "description" : "Optional regex pattern to filter log messages. If not provided, all logs are returned."
                    },
                    "logLevel" : {
                      "type" : "string",
                      "description" : "Minimum log level to return. Options: ERROR, WARN, INFO, DEBUG, TRACE. Defaults to ERROR.",
                      "enum" : ["ERROR", "WARN", "INFO", "DEBUG", "TRACE"]
                    },
                    "maxEntries" : {
                      "type" : "integer",
                      "description" : "Maximum number of log entries to return. Defaults to 200.",
                      "minimum" : 1,
                      "maximum" : 1000
                    }
                  }
                }
                """;

        return List.of(new SyncToolSpecification(
                Tool.builder()
                        .name("logs")
                        .description("Retrieve logs with optional filtering. "
                                + "Supports filtering by regex pattern, log level (ERROR, WARN, INFO, DEBUG, TRACE), "
                                + "and maximum number of entries. Returns most recent logs first.")
                        .inputSchema(jsonMapper.get(), schema)
                        .build(),
                (exchange, request) -> {
                    String regexPattern = (String) request.arguments().get("regex");
                    String logLevelStr = (String) request.arguments().get("logLevel");
                    Object maxEntriesObj = request.arguments().get("maxEntries");

                    // Parse parameters
                    int maxEntries = DEFAULT_MAX_LOGS;
                    if (maxEntriesObj instanceof Number) {
                        maxEntries = ((Number) maxEntriesObj).intValue();
                        maxEntries = Math.min(maxEntries, 1000); // Cap at 1000
                    }

                    Level minLogLevel = Level.ERROR;
                    if (logLevelStr != null && !logLevelStr.isEmpty()) {
                        minLogLevel = parseLogLevel(logLevelStr);
                        if (minLogLevel == null) {
                            return CallToolResult.builder()
                                    .addTextContent("Invalid log level: " + logLevelStr
                                            + ". Valid options are: ERROR, WARN, INFO, DEBUG, TRACE")
                                    .isError(true)
                                    .build();
                        }
                    }

                    // Compile regex pattern if provided
                    Pattern pattern = null;
                    if (regexPattern != null && !regexPattern.isEmpty()) {
                        try {
                            pattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);
                        } catch (PatternSyntaxException e) {
                            return CallToolResult.builder()
                                    .addTextContent("Invalid regex pattern: " + e.getMessage())
                                    .isError(true)
                                    .build();
                        }
                    }

                    List<LogSnapshot> filteredLogs =
                            structuredLogBufferAppender.getBuffer().getRecent(pattern, minLogLevel, maxEntries);

                    // Format output
                    String result = formatLogs(filteredLogs, regexPattern, minLogLevel, maxEntries);

                    return CallToolResult.builder().addTextContent(result).build();
                }));
    }

    private Level parseLogLevel(String levelStr) {
        return switch (levelStr.toUpperCase()) {
            case "ERROR" -> Level.ERROR;
            case "WARN", "WARNING" -> Level.WARN;
            case "INFO" -> Level.INFO;
            case "DEBUG" -> Level.DEBUG;
            case "TRACE" -> Level.TRACE;
            default -> null;
        };
    }

    private String formatLogs(List<LogSnapshot> logs, String regexPattern, Level minLogLevel, int maxEntries) {
        StringBuilder result = new StringBuilder();

        result.append("=== Log Entries ===\n\n");
        result.append("Filter Settings:\n");
        result.append("  - Log Level: ").append(getLogLevelName(minLogLevel)).append(" and higher severity\n");
        result.append("  - Regex Pattern: ")
                .append(regexPattern != null ? regexPattern : "(none)")
                .append("\n");
        result.append("  - Max Entries: ").append(maxEntries).append("\n");
        result.append("  - Entries Found: ").append(logs.size()).append("\n\n");

        if (logs.isEmpty()) {
            result.append("No log entries found matching the criteria.\n");
            return result.toString();
        }

        result.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");

        for (int i = 0; i < logs.size(); i++) {
            LogSnapshot entry = logs.get(i);
            formatLogEntry(entry, i + 1, result);

            if (i < logs.size() - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }

    private void formatLogEntry(LogSnapshot entry, int index, StringBuilder result) {
        result.append("[").append(index).append("] ");
        result.append(DATE_FORMAT.format(new Date(entry.timeMillis())));
        result.append(" [").append(getLogLevelName(entry.level())).append("] ");
        result.append("[")
                .append(entry.loggerName() != null ? entry.loggerName() : "(unknown logger)")
                .append("] ");

        String message = entry.formattedMessage();
        result.append(message != null ? message : "(no message)");
        result.append("\n");

        if (entry.threadName() != null && !entry.threadName().isEmpty()) {
            result.append("    Thread: ").append(entry.threadName()).append("\n");
        }

        if (!entry.mdc().isEmpty()) {
            result.append("    MDC: ").append(formatMdc(entry.mdc())).append("\n");
        }

        String throwableText = entry.throwableText();
        if (throwableText != null && !throwableText.isEmpty()) {
            String[] lines = throwableText.split("\n");
            int maxLines = Math.min(lines.length, 10);
            for (int i = 0; i < maxLines; i++) {
                result.append("      ").append(lines[i]).append("\n");
            }
            if (lines.length > maxLines) {
                result.append("      ... (").append(lines.length - maxLines).append(" more lines)\n");
            }
        }
    }

    private String formatMdc(Map<String, String> mdc) {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : mdc.entrySet()) {
            if (!first) {
                result.append(", ");
            }
            result.append(entry.getKey()).append('=').append(entry.getValue());
            first = false;
        }
        return result.toString();
    }

    private String getLogLevelName(Level level) {
        return level != null ? level.levelStr : "UNKNOWN";
    }
}
