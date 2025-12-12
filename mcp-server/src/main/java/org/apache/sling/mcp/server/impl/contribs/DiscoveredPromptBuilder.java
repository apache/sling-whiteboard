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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.PromptArgument;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.mcp.server.impl.DiscoveredPrompt;
import org.commonmark.ext.front.matter.YamlFrontMatterExtension;
import org.commonmark.ext.front.matter.YamlFrontMatterVisitor;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.markdown.MarkdownRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DiscoveredPromptBuilder {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public DiscoveredPrompt buildPrompt(Resource prompt, String promptName) throws IOException {

        Parser parser = Parser.builder()
                .extensions(List.of(YamlFrontMatterExtension.create()))
                .build();
        String charset = prompt.getResourceMetadata().getCharacterEncoding();
        if (charset == null) {
            charset = StandardCharsets.UTF_8.name();
        }
        try (InputStream is = prompt.adaptTo(InputStream.class)) {
            Node node = parser.parseReader(new InputStreamReader(is, charset));
            YamlFrontMatterVisitor visitor = new YamlFrontMatterVisitor();
            node.accept(visitor);

            String title = visitor.getData().getOrDefault("title", List.of()).stream()
                    .findFirst()
                    .orElse(null);
            String description = visitor.getData().getOrDefault("description", List.of()).stream()
                    .findFirst()
                    .orElse(null);

            List<PromptArgument> arguments = new ArrayList<>();

            visitor.getData().entrySet().stream()
                    .filter(e -> e.getKey().startsWith("argument."))
                    .forEach(e -> {
                        String argName = e.getKey().substring("argument.".length());
                        List<String> values = e.getValue();
                        String argTitle = null;
                        String argDesc = null;
                        boolean argRequired = false;
                        for (String value : values) {

                            if (value.startsWith("title:")) {
                                argTitle = value.substring("title:".length()).trim();
                            } else if (value.startsWith("description:")) {
                                argDesc =
                                        value.substring("description:".length()).trim();
                            } else if (value.startsWith("required:")) {
                                String argRequiredRaw =
                                        value.substring("required:".length()).trim();
                                argRequired = Boolean.valueOf(argRequiredRaw);
                            } else {
                                logger.warn("Unknown argument property in prompt {}: {}", promptName, value);
                            }
                        }

                        arguments.add(new PromptArgument(argName, argTitle, argDesc, argRequired));
                    });

            return new RepositoryPrompt(prompt.getPath(), promptName, title, description, arguments);
        }
    }

    static class RepositoryPrompt implements DiscoveredPrompt {

        private final String promptPath;
        private final String promptName;
        private final String promptTitle;
        private final String promptDescription;
        private final List<PromptArgument> arguments;

        RepositoryPrompt(
                String promptPath,
                String promptName,
                String promptTitle,
                String promptDescription,
                List<PromptArgument> arguments) {
            this.promptPath = promptPath;
            this.promptName = promptName;
            this.promptTitle = promptTitle;
            this.promptDescription = promptDescription;
            this.arguments = arguments;
        }

        @Override
        public List<PromptMessage> getPromptMessages(McpTransportContext c, GetPromptRequest req) {

            ResourceResolver rr = (ResourceResolver) c.get("resourceResolver");

            try {
                Resource promptResource = rr.getResource(promptPath);
                String encoding = promptResource.getResourceMetadata().getCharacterEncoding();
                if (encoding == null) {
                    encoding = StandardCharsets.UTF_8.name();
                }

                Parser parser = Parser.builder()
                        .extensions(List.of(YamlFrontMatterExtension.create()))
                        .build();

                try (InputStream stream = promptResource.adaptTo(InputStream.class)) {

                    Node node = parser.parseReader(new InputStreamReader(stream, encoding));

                    // Render markdown without the front matter extension for clarity and for keeping
                    // the context usage low
                    MarkdownRenderer renderer = MarkdownRenderer.builder().build();
                    String contents = renderer.render(node);

                    for (PromptArgument arg : arguments) {
                        String placeholder = "{" + arg.name() + "}";
                        Object requestedArg = req.arguments().get(arg.name());
                        String value = requestedArg != null ? requestedArg.toString() : "";
                        if (!value.isEmpty()) {
                            contents = contents.replace(placeholder, value);
                        }
                    }

                    return List.of(new PromptMessage(Role.ASSISTANT, new TextContent(contents)));
                }
            } catch (IOException e) {
                return List.of();
            }
        }

        @Override
        public Prompt asPrompt() {
            return new Prompt(promptName, promptTitle, promptDescription, arguments);
        }
    }
}
