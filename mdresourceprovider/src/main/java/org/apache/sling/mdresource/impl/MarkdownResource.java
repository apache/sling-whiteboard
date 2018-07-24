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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;

import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ast.util.TextCollectingVisitor;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;

public class MarkdownResource extends AbstractResource {

    private final ResourceResolver resolver;
    private final String path;
    private final File backingFile;
    private ValueMap valueMap;
    private ResourceMetadata metadata;

    public MarkdownResource(ResourceResolver resourceResolver, String path, File backingFile) {
        this.resolver = resourceResolver;
        this.path = path;
        this.backingFile = backingFile;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getResourceType() {
        return getValueMap().get("sling:resourceType", String.class);
    }

    @Override
    public String getResourceSuperType() {
        return getValueMap().get("sling:resourceSuperType", String.class);
    }

    @Override
    public ResourceMetadata getResourceMetadata() {
        if ( metadata == null ) {
            metadata = getResourceMetadata0();
        }
        return metadata;
    }

    private ResourceMetadata getResourceMetadata0() {
        if ( !backingFile.exists() || !backingFile.canRead() ) {
            return null;
        }
        
        ResourceMetadata metadata = new ResourceMetadata();
        metadata.setModificationTime(backingFile.lastModified());
        metadata.setResolutionPath(path);
        return metadata;
    }

    @Override
    public ResourceResolver getResourceResolver() {
        return resolver;
    }

    @Override
    public ValueMap getValueMap() {
        if (valueMap == null) {
            valueMap = getValueMap0();
        }

        return valueMap;
    }

    private ValueMap getValueMap0() {
        if ( !backingFile.exists() || !backingFile.canRead() ) {
            return null;
        }
        
        Parser parser = Parser.builder().build();
        HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();
        TextCollectingVisitor visitor = new TextCollectingVisitor();

        Map<String, Object> props = new HashMap<>();
        // TODO - do we need the primary type?
        // TODO - allow reading properties from the file, overriding default for resource type
        props.put("sling:resourceType", "sling/markdown/file");

        try {
            try ( BufferedReader r =  Files.newBufferedReader(backingFile.toPath())) {
                
                Node node = parser.parseReader(r);
                Node maybeTitle = node.getFirstChild();
                // TODO - what to do if no title is found?
                if ( maybeTitle instanceof Heading ) {
                    props.put("jcr:title", visitor.collectAndGetText(maybeTitle));
                    node = maybeTitle.getNext();
                }
                // TODO parse excluding the title
                props.put("jcr:description", htmlRenderer.render(node));

            }
        } catch (IOException e) {
            // TODO - handle errors someplace else?
            throw new RuntimeException(e);
        }
        
        return new ValueMapDecorator(props);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T adaptTo(Class<T> type) {
        if ( type == ValueMap.class || type == Map.class ) {
            return (T) getValueMap();
        }
        
        return null;
    }

    @Override
    public String toString() {
        
        return getClass().getSimpleName() + ", path: " + path;
    }
}
