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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChange.ChangeType;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.mcp.server.impl.DiscoveredPrompt;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class RepositoryPromptsRegistrar {

    private static final String PROMPT_LIBS_DIR = "/libs/sling/mcp/prompts";
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private ConcurrentMap<String, ServiceRegistration<DiscoveredPrompt>> registrations = new ConcurrentHashMap<>();

    @Activate
    public RepositoryPromptsRegistrar(@Reference ResourceResolverFactory rrf, BundleContext ctx) throws LoginException {

        ctx.registerService(
                ResourceChangeListener.class,
                new ResourceChangeListener() {

                    @Override
                    public void onChange(@NotNull List<ResourceChange> changes) {
                        changes.forEach(a -> logger.info("Received change {} at {}", a.getType(), a.getPath()));

                        try (ResourceResolver resolver = rrf.getAdministrativeResourceResolver(null)) {
                            for (ResourceChange change : changes) {
                                if (change.getType() == ChangeType.REMOVED) {
                                    String promptName = getPromptName(change.getPath());
                                    ServiceRegistration<DiscoveredPrompt> sr = registrations.remove(promptName);
                                    if (sr != null) {
                                        sr.unregister();
                                    } else {
                                        logger.warn(
                                                "No registered prompt found for removed prompt {} at path {}, unable to unregister prompt.",
                                                promptName,
                                                change.getPath());
                                    }
                                } else {
                                    String promptName = getPromptName(change.getPath());
                                    ServiceRegistration<DiscoveredPrompt> sr = registrations.remove(promptName);
                                    if (sr != null) {
                                        sr.unregister();
                                    }
                                    Resource prompt = resolver.getResource(change.getPath());
                                    if (prompt != null) {
                                        registerPrompt(ctx, promptName, prompt);
                                    } else {
                                        logger.warn(
                                                "Prompt resource at {} not found for change type {}, unable to register prompt.",
                                                change.getPath(),
                                                change.getType());
                                    }
                                }
                            }
                        } catch (LoginException e) {
                            logger.error("Error processing resource change", e);
                        }
                    }
                },
                new Hashtable<>(Map.of(
                        ResourceChangeListener.PATHS, PROMPT_LIBS_DIR, ResourceChangeListener.CHANGES, new String[] {
                            ResourceChangeListener.CHANGE_ADDED,
                            ResourceChangeListener.CHANGE_CHANGED,
                            ResourceChangeListener.CHANGE_REMOVED
                        })));

        // TODO - use service user
        try (ResourceResolver resolver = rrf.getAdministrativeResourceResolver(null)) {

            Iterator<Resource> prompts = resolver.findResources(
                    "/jcr:root" + PROMPT_LIBS_DIR + "//element(*,nt:file)[jcr:like(fn:name(), '%.md')]", "xpath");
            while (prompts.hasNext()) {
                Resource prompt = prompts.next();
                String promptName = getPromptName(prompt.getPath());

                registerPrompt(ctx, promptName, prompt);
            }
        }
    }

    private void registerPrompt(BundleContext ctx, String promptName, Resource prompt) {

        Map<String, String> serviceProps = Map.of(DiscoveredPrompt.SERVICE_PROP_NAME, promptName);

        // TODO - discover additional properties from the markdown file (front matter)

        var sr = ctx.registerService(
                DiscoveredPrompt.class,
                new RepositoryPrompt(prompt.getPath()),
                new Hashtable<String, String>(serviceProps));

        registrations.put(promptName, sr);
    }

    private String getPromptName(String path) {

        // remove prefix
        String promptName = path.substring(PROMPT_LIBS_DIR.length() + 1); // account for trailing slash

        // remove optional /jcr:content node name
        if (promptName.endsWith("/jcr:content")) {
            promptName = promptName.substring(0, promptName.length() - "/jcr:content".length());
        }

        // remove .md extension
        return promptName.substring(0, promptName.length() - ".md".length());
    }

    class RepositoryPrompt implements DiscoveredPrompt {
        private final String promptPath;

        RepositoryPrompt(String promptPath) {
            this.promptPath = promptPath;
        }

        @Override
        public List<PromptMessage> getPromptMessages(ResourceResolver resolver) {

            try {
                Resource promptResource = resolver.getResource(promptPath);
                String encoding = promptResource.getResourceMetadata().getCharacterEncoding();
                if (encoding == null) {
                    encoding = StandardCharsets.UTF_8.name();
                }
                try (InputStream stream = promptResource.adaptTo(InputStream.class)) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    stream.transferTo(baos);
                    String contents = baos.toString(encoding);
                    return List.of(new PromptMessage(Role.ASSISTANT, new TextContent(contents)));
                }
            } catch (IOException e) {
                return List.of();
            }
        }
    }
}
