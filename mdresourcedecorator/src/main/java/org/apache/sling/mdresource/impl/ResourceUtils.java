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
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.api.wrappers.ValueMapUtil;
import org.apache.sling.mdresource.impl.md.MarkdownProcessor;
import org.apache.sling.mdresource.impl.md.ProcessingInstructions;
import org.apache.sling.mdresource.impl.md.ProcessingResult;
import org.apache.sling.mdresource.impl.md.links.CustomLinkResolverFactory;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.util.ast.Node;

public class ResourceUtils {

    public static final String PROPERTY_RESOURCE_SUPER_TYPE = "sling:resourceSuperType";

    public static ValueMap newValueMap(final ResourceConfiguration config, final Resource rsrc, final ValueMap origProps) {
        final Map<String, Object> props = new HashMap<>();
        props.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, rsrc.getResourceType());
        props.put(PROPERTY_RESOURCE_SUPER_TYPE, rsrc.getResourceSuperType());

        final ProcessingInstructions inst = new ProcessingInstructions();
        inst.extractTitle = config.titleProperty != null;
        inst.handleYamlFrontmatter = true;

        try ( final Reader r = new InputStreamReader(rsrc.adaptTo(InputStream.class), StandardCharsets.UTF_8)) {

            final ProcessingResult result = MarkdownProcessor.INSTANCE.process(r, rsrc.getParent(), inst);
            props.putAll(result.properties);
            if ( result.title != null ) {
                props.put(config.titleProperty, result.title);
            }
            final HtmlRenderer.Builder builder = HtmlRenderer.builder();
            builder.linkResolverFactory(new CustomLinkResolverFactory());
            final HtmlRenderer htmlRenderer = builder.build();

            final List<Map.Entry<String, String>> elements = new ArrayList<>();
            final List<Node> nodes = new ArrayList<>();
            for(final Node node : result.document.getChildren()) {
                nodes.add(node);
                node.unlink();
            }
            for(final Node node : nodes) {
                result.document.appendChild(node);
                elements.add(new AbstractMap.SimpleEntry<>(node.getNodeName(), htmlRenderer.render(result.document)));
                node.unlink();
            }
            props.put(config.elementsProperty, elements);
        } catch (final IOException e) {
            MarkdownResourceDecorator.LOGGER.error("Unable to read markdown : " + e.getMessage(), e);
        }

        return ValueMapUtil.merge(new ValueMapDecorator(props), origProps);
    }
}
