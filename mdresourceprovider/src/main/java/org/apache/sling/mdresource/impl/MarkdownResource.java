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

import static java.util.Collections.singleton;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;

import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor;
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;

public class MarkdownResource extends AbstractResource {

    private final ResourceResolver resolver;
    private final String path;
    private final File backingFile;
    private ValueMap valueMap;
    private ResourceMetadata metadata;
    private final List<SpecialHandler> handlers = new ArrayList<>();
    {
    	handlers.add(new HeadingHandler());
    	handlers.add(new YamlFrontMatterHandler());
    }

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
        
        Parser parser = Parser.builder()
        		.extensions(singleton(YamlFrontMatterExtension.create()))
        		.build();
        HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();

        Map<String, Object> props = new HashMap<>();
        props.put("sling:resourceType", "sling/markdown/file");

        try {
            try ( BufferedReader r =  Files.newBufferedReader(backingFile.toPath())) {
                
                Node document = parser.parseReader(r);
                Node currentNode = document.getFirstChild();
                // consume special nodes at the beginning of the file
                // while at least one special node (as defined by the list of handlers) finds
                // something to handle, parsing continues
                //
                // this restriction is mostly for simplicity, as it's easy to skip the first
                // special nodes and pass off the rest to the HTML renderer
                // in the future, we can consider allowing these special nodes anywhere
                while ( currentNode != null ) {
                	boolean handled = false;
                	for ( SpecialHandler handler : handlers ) {
                		handled = handler.consume(currentNode, props);
                		if ( handled ) {
                			currentNode = currentNode.getNext();
                			break;
                		}
                	}
                	
                	if ( !handled ) 
            			break;
                }

                if ( currentNode != null)
                	props.put("jcr:description", htmlRenderer.render(currentNode));
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

	/**
	 * Interface for declaring handlers for 'special' nodes
	 * 
	 * <p>A 'special' node is processed by a separate handler and will not
	 * be included in the parsed HTML body.</p>
	 *
	 */
	private interface SpecialHandler {
    	boolean consume(Node node, Map<String, Object> properties);
    }
    
    /**
     * Handler that populates a resource's properties based on a YAML front matter entry
     *
     */
    private static final class YamlFrontMatterHandler implements SpecialHandler {
		@Override
		public boolean consume(Node n, Map<String, Object> p) {
			AbstractYamlFrontMatterVisitor vis = new AbstractYamlFrontMatterVisitor();
			vis.visit(n);
			if ( vis.getData().isEmpty() )
				return false;
			
			for ( Map.Entry<String, List<String>> entry : vis.getData().entrySet() ) {
				if ( entry.getValue().size() == 1)
					p.put(entry.getKey(), entry.getValue().get(0));
				else
					p.put(entry.getKey(), entry.getValue().toArray(new String[0]));
			}
				
			
			return true;
		}
	}

	/**
	 * Handler that populates a resource's jcr:title property based on a first-level heading
	 *
	 */
	private static final class HeadingHandler implements SpecialHandler {
		@Override
		public boolean consume(Node n, Map<String, Object> p) {
			if ( n instanceof Heading ) {
				Heading h = (Heading) n;
				if ( h.getLevel() == 1 ) {
					p.put("jcr:title", h.getText());
					return true;
				}
			}
			return false;
		}
	}
}
