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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.PromptArgument;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.mcp.server.impl.DiscoveredPrompt;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DiscoveredPromptBuilderTest {

    @Test
    void basicMarkdownFile() throws IOException {

        String markdownFile = """
                # Basic Prompt
                """;

        Resource promptResource = mock(Resource.class);
        when(promptResource.getResourceMetadata()).thenReturn(new ResourceMetadata());
        when(promptResource.adaptTo(InputStream.class))
                .thenReturn(new ByteArrayInputStream(markdownFile.getBytes(StandardCharsets.UTF_8)));

        DiscoveredPromptBuilder builder = new DiscoveredPromptBuilder();
        DiscoveredPrompt prompt = builder.buildPrompt(promptResource, "basic-prompt");

        assertThat(prompt)
                .as("prompt")
                .extracting(DiscoveredPrompt::asPrompt)
                .extracting(Prompt::name, Prompt::title, Prompt::description, Prompt::arguments)
                .containsExactly("basic-prompt", null, null, List.of());
    }

    @Test
    void basicFrontMatter() throws IOException {

        String markdownFile = """
                ---
                title: Basic Prompt
                description: |
                    A basic prompt for testing.
                    Spans multiple lines.
                ---
                # Basic Prompt
                """;

        Resource promptResource = mock(Resource.class);
        when(promptResource.getResourceMetadata()).thenReturn(new ResourceMetadata());
        when(promptResource.adaptTo(InputStream.class))
                .thenReturn(new ByteArrayInputStream(markdownFile.getBytes(StandardCharsets.UTF_8)));

        DiscoveredPromptBuilder builder = new DiscoveredPromptBuilder();
        DiscoveredPrompt prompt = builder.buildPrompt(promptResource, "prompt-with-front-matter");

        assertThat(prompt)
                .as("prompt")
                .extracting(DiscoveredPrompt::asPrompt)
                .extracting(Prompt::name, Prompt::title, Prompt::description, Prompt::arguments)
                .containsExactly(
                        "prompt-with-front-matter",
                        "Basic Prompt",
                        "A basic prompt for testing.\nSpans multiple lines.",
                        List.of());
    }

    @Test
    void arguments() throws IOException {

        String markdownFile = """
                ---
                title: Basic Prompt
                argument.first:
                    - title: First Argument
                    - description: This is the first argument
                    - required: true
                argument.second:
                    - title: Second Argument
                ---
                # Basic Prompt
                """;

        Resource promptResource = mock(Resource.class);
        when(promptResource.getResourceMetadata()).thenReturn(new ResourceMetadata());
        when(promptResource.adaptTo(InputStream.class))
                .thenReturn(new ByteArrayInputStream(markdownFile.getBytes(StandardCharsets.UTF_8)));

        DiscoveredPromptBuilder builder = new DiscoveredPromptBuilder();
        DiscoveredPrompt prompt = builder.buildPrompt(promptResource, "prompt-with-arguments");
        assertThat(prompt)
                .as("prompt")
                .extracting(DiscoveredPrompt::asPrompt)
                .extracting(Prompt::name, Prompt::title, Prompt::description, Prompt::arguments)
                .containsExactly(
                        "prompt-with-arguments",
                        "Basic Prompt",
                        null,
                        List.of(
                                new PromptArgument("first", "First Argument", "This is the first argument", true),
                                new PromptArgument("second", "Second Argument", null, false)));
    }
}
