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
package org.apache.sling.mcp.server.impl;

import java.util.Optional;

import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncCompletionSpecification;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncResourceTemplateSpecification;
import io.modelcontextprotocol.server.McpStatelessServerFeatures.SyncToolSpecification;

// Temporary abstraction to make it easier to contribute various MCP server features
//
// The major problem with this approach is that all contributions are loaded once the MCPServlet
// is activated and therefore it does not take into account added/changed/removed contributions
//
// Expect this abstraction to change
public interface McpServerContribution {

    default Optional<SyncToolSpecification> getSyncToolSpecification() {
        return Optional.empty();
    }

    default Optional<SyncResourceSpecification> getSyncResourceSpecification() {
        return Optional.empty();
    }

    default Optional<SyncResourceTemplateSpecification> getSyncResourceTemplateSpecification() {
        return Optional.empty();
    }

    default Optional<SyncPromptSpecification> getSyncPromptSpecification() {
        return Optional.empty();
    }

    default Optional<SyncCompletionSpecification> getSyncCompletionSpecification() {
        return Optional.empty();
    }
}
