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
package org.apache.sling.mdresource.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.api.wrappers.ValueMapUtil;
import org.apache.sling.mdresource.impl.ResourceConfiguration.SourceType;
import org.apache.sling.mdresource.impl.md.MarkdownProcessor;
import org.apache.sling.mdresource.impl.md.ProcessingInstructions;
import org.apache.sling.mdresource.impl.md.ProcessingResult;
import org.apache.sling.mdresource.impl.md.links.CustomLinkResolverFactory;
import org.slf4j.LoggerFactory;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.util.ast.Node;

public class ResourceUtils {

    public static final String PROPERTY_RESOURCE_SUPER_TYPE = "sling:resourceSuperType";

    private static String readMarkdown(final Reader reader) throws IOException {
        try {
            final StringBuilder sb = new StringBuilder();
            final char[] buf = new char[4096];
            int l;
            while ( (l = reader.read(buf)) > 0) {
                sb.append(buf, 0, l);
            }

            return sb.toString();
        } finally {
            if ( reader != null ) {
                reader.close();
            }
        }

    }

    private static Reader getMarkdownReader(final ResourceConfiguration config,
            final Resource rsrc,
            final ValueMap origProps,
            final Map<String, Object> props) throws IOException {
        if ( config.markdownProperty != null ) {
            final String md;
            if ( config.sourceType == SourceType.Property) {
                md = origProps.get(config.sourceMarkdownProperty, String.class);
            } else {
                md = readMarkdown(new InputStreamReader(rsrc.adaptTo(InputStream.class), StandardCharsets.UTF_8));
            }
            props.put(config.markdownProperty, md);

            return new StringReader(md);
        }
        if ( config.sourceType == SourceType.Property) {
            final String md = origProps.get(config.sourceMarkdownProperty, String.class);
            return new StringReader(md);
        }
        return new InputStreamReader(rsrc.adaptTo(InputStream.class), StandardCharsets.UTF_8);
    }

    public static ValueMap newValueMap(final ResourceConfiguration config, final Resource rsrc, final ValueMap origProps) {
        final Map<String, Object> props = new HashMap<>();
        props.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, rsrc.getResourceType());
        props.put(PROPERTY_RESOURCE_SUPER_TYPE, rsrc.getResourceSuperType());

        final ProcessingInstructions inst = new ProcessingInstructions();
        inst.extractTitle = config.titleProperty != null;
        inst.handleYamlFrontmatter = true;

        try ( final Reader r = getMarkdownReader(config, rsrc, origProps, props)) {

            final ProcessingResult result = MarkdownProcessor.INSTANCE.process(r, rsrc.getParent(), inst);
            props.putAll(result.properties);
            if ( result.title != null ) {
                props.put(config.titleProperty, result.title);
            }
            if ( result.html != null ) {
                props.put(config.htmlProperty, result.html);
            }
            if ( result.document.hasChildren()) {
                final HtmlRenderer.Builder builder = HtmlRenderer.builder();
                if ( config.rewriteLinks ) {
                    builder.linkResolverFactory(new CustomLinkResolverFactory(rsrc));
                }
                final HtmlRenderer htmlRenderer = builder.build();

                if (config.htmlProperty != null) {
                    props.put(config.htmlProperty, htmlRenderer.render(result.document));
                }
                if ( config.elementsProperty != null ) {
                    final List<Map.Entry<String, String>> elements = new ArrayList<>();
                    for(final Node node : result.document.getChildren()) {
                        elements.add(new AbstractMap.SimpleEntry<>(node.getNodeName(), htmlRenderer.render(node)));
                    }
                    props.put(config.elementsProperty, elements);
                    LoggerFactory.getLogger("foo").info("Setting {} to {}", config.elementsProperty, elements.size());
                }
            } else {
                if (config.htmlProperty != null) {
                    props.put(config.htmlProperty, "");
                }
                if ( config.elementsProperty != null ) {
                    props.put(config.elementsProperty, Collections.emptyMap());
                }
            }


        } catch (final IOException e) {
            MarkdownResourceDecorator.LOGGER.error("Unable to read markdown : " + e.getMessage(), e);
        }

        if ( origProps == null ) {
            return new ValueMapDecorator(props);
        }
        return ValueMapUtil.merge(new ValueMapDecorator(props), origProps);
    }
}
