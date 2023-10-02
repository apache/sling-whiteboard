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
package org.apache.sling.mdresource.impl.md;

import static java.util.Collections.singleton;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.mdresource.impl.md.handler.HeadingHandler;
import org.apache.sling.mdresource.impl.md.handler.NodeHandler;
import org.apache.sling.mdresource.impl.md.handler.YamlFrontMatterHandler;

import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;

public class MarkdownProcessor {

    private final Parser parser = Parser.builder()
                .extensions(singleton(YamlFrontMatterExtension.create()))
                .build();


    public static final MarkdownProcessor INSTANCE = new MarkdownProcessor();

    public ProcessingResult process(final Reader reader,
            final Resource baseResource,
            final ProcessingInstructions inst) throws IOException {
        final ProcessingResult result = new ProcessingResult();

        final List<NodeHandler> handlers = new ArrayList<>();
        if ( inst.handleYamlFrontmatter ) {
            handlers.add(new YamlFrontMatterHandler());
        }
        if ( inst.extractTitle ) {
            handlers.add(new HeadingHandler());
        }

        final Document document = parser.parseReader(reader);

        Node currentNode = document.getFirstChild();
        while ( currentNode != null ) {
            boolean handled = false;
            for(final NodeHandler handler : handlers ) {
                handled = handler.consume(currentNode, result);
                if ( handled ) {
                    break;
                }
            }
            final Node nextNode = currentNode.getNext();
            if ( handled ) {
                currentNode.unlink();
            }

            currentNode = nextNode;
        }

        result.document = document;
        return result;
    }
}
