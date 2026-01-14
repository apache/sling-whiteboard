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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.apache.sling.mcp.server.spi.McpServerContribution;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;

/**
 * MCP Tool that provides access to logs with filtering capabilities.
 * Allows filtering by regex pattern, log level, and maximum number of entries.
 */
@Component
public class LogToolContribution implements McpServerContribution {

    @Reference
    private LogReaderService logReaderService;

    @Reference
    private McpJsonMapper jsonMapper;

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
                        .name("aem-logs")
                        .description("Retrieve logs with optional filtering. "
                                + "Supports filtering by regex pattern, log level (ERROR, WARN, INFO, DEBUG, TRACE), "
                                + "and maximum number of entries. Returns most recent logs first.")
                        .inputSchema(jsonMapper, schema)
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

                    int minLogLevel = LogService.LOG_ERROR;
                    if (logLevelStr != null && !logLevelStr.isEmpty()) {
                        minLogLevel = parseLogLevel(logLevelStr);
                        if (minLogLevel == -1) {
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

                    // Collect and filter logs
                    List<LogEntry> filteredLogs = collectLogs(pattern, minLogLevel, maxEntries);

                    // Format output
                    String result = formatLogs(filteredLogs, regexPattern, minLogLevel, maxEntries);

                    return CallToolResult.builder().addTextContent(result).build();
                }));
    }

    private List<LogEntry> collectLogs(Pattern pattern, int minLogLevel, int maxEntries) {
        List<LogEntry> logs = new ArrayList<>();

        @SuppressWarnings("unchecked")
        Enumeration<LogEntry> logEntries = logReaderService.getLog();
        while (logEntries.hasMoreElements() && logs.size() < maxEntries) {
            LogEntry entry = logEntries.nextElement();

            // Filter by log level (lower values = higher severity)
            if (entry.getLevel() > minLogLevel) {
                continue;
            }

            // Filter by regex pattern if provided - search entire log entry
            if (pattern != null) {
                String fullLogEntry = buildFullLogEntryText(entry);
                if (!pattern.matcher(fullLogEntry).find()) {
                    continue;
                }
            }

            logs.add(entry);
        }

        return logs;
    }

    private String buildFullLogEntryText(LogEntry entry) {
        StringBuilder text = new StringBuilder();

        // Add log level
        text.append(getLogLevelName(entry.getLevel())).append(" ");

        // Add bundle name
        Bundle bundle = entry.getBundle();
        if (bundle != null) {
            text.append(getBundleName(bundle)).append(" ");
        }

        // Add message
        String message = entry.getMessage();
        if (message != null) {
            text.append(message).append(" ");
        }

        // Add service reference info
        ServiceReference<?> serviceRef = entry.getServiceReference();
        if (serviceRef != null) {
            String serviceDesc = getServiceDescription(serviceRef);
            if (serviceDesc != null && !serviceDesc.isEmpty()) {
                text.append(serviceDesc).append(" ");
            }
        }

        // Add exception info
        Throwable exception = entry.getException();
        if (exception != null) {
            text.append(exception.getClass().getName()).append(" ");
            if (exception.getMessage() != null) {
                text.append(exception.getMessage()).append(" ");
            }

            // Add stack trace
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            exception.printStackTrace(pw);
            text.append(sw.toString());
        }

        return text.toString();
    }

    private int parseLogLevel(String levelStr) {
        return switch (levelStr.toUpperCase()) {
            case "ERROR" -> LogService.LOG_ERROR;
            case "WARN", "WARNING" -> LogService.LOG_WARNING;
            case "INFO" -> LogService.LOG_INFO;
            case "DEBUG" -> LogService.LOG_DEBUG;
            default -> -1;
        };
    }

    private String formatLogs(List<LogEntry> logs, String regexPattern, int minLogLevel, int maxEntries) {
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
            LogEntry entry = logs.get(i);
            formatLogEntry(entry, i + 1, result);

            if (i < logs.size() - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }

    private void formatLogEntry(LogEntry entry, int index, StringBuilder result) {
        result.append("[").append(index).append("] ");
        result.append(DATE_FORMAT.format(new Date(entry.getTime())));
        result.append(" [").append(getLogLevelName(entry.getLevel())).append("] ");

        // Add bundle information
        Bundle bundle = entry.getBundle();
        if (bundle != null) {
            String bundleName = getBundleName(bundle);
            result.append("[").append(bundleName).append("] ");
        }

        // Add message
        String message = entry.getMessage();
        result.append(message != null ? message : "(no message)");
        result.append("\n");

        // Add service reference info if available
        ServiceReference<?> serviceRef = entry.getServiceReference();
        if (serviceRef != null) {
            String serviceDesc = getServiceDescription(serviceRef);
            if (serviceDesc != null && !serviceDesc.isEmpty()) {
                result.append("    Service: ").append(serviceDesc).append("\n");
            }
        }

        // Add exception info if available
        Throwable exception = entry.getException();
        if (exception != null) {
            result.append("    Exception: ").append(exception.getClass().getName());
            if (exception.getMessage() != null) {
                result.append(": ").append(exception.getMessage());
            }
            result.append("\n");

            // Add stack trace (first few lines)
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            exception.printStackTrace(pw);
            String stackTrace = sw.toString();

            // Limit stack trace to first 10 lines
            String[] lines = stackTrace.split("\n");
            int maxLines = Math.min(lines.length, 10);
            for (int i = 0; i < maxLines; i++) {
                result.append("      ").append(lines[i]).append("\n");
            }
            if (lines.length > maxLines) {
                result.append("      ... (").append(lines.length - maxLines).append(" more lines)\n");
            }
        }
    }

    private String getBundleName(Bundle bundle) {
        String name = bundle.getHeaders().get(Constants.BUNDLE_NAME);
        if (name == null || name.isEmpty()) {
            name = bundle.getSymbolicName();
        }
        if (name == null || name.isEmpty()) {
            name = "Bundle#" + bundle.getBundleId();
        }
        return name;
    }

    private String getServiceDescription(ServiceReference<?> ref) {
        if (ref == null) {
            return null;
        }

        Object serviceId = ref.getProperty(Constants.SERVICE_ID);
        Object objectClass = ref.getProperty(Constants.OBJECTCLASS);

        StringBuilder desc = new StringBuilder();
        if (objectClass instanceof String[]) {
            String[] classes = (String[]) objectClass;
            if (classes.length > 0) {
                desc.append(classes[0]);
                if (classes.length > 1) {
                    desc.append(" (").append(classes.length - 1).append(" more interfaces)");
                }
            }
        }

        if (serviceId != null) {
            if (desc.length() > 0) {
                desc.append(" ");
            }
            desc.append("[id=").append(serviceId).append("]");
        }

        return desc.toString();
    }

    private String getLogLevelName(int level) {
        return switch (level) {
            case LogService.LOG_ERROR -> "ERROR";
            case LogService.LOG_WARNING -> "WARN";
            case LogService.LOG_INFO -> "INFO";
            case LogService.LOG_DEBUG -> "DEBUG";
            default -> "LEVEL_" + level;
        };
    }
}
