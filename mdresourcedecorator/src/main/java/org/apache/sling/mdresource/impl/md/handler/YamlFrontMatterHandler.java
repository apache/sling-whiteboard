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
package org.apache.sling.mdresource.impl.md.handler;

import java.util.List;
import java.util.Map;

import org.apache.sling.mdresource.impl.md.ProcessingResult;

import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor;
import com.vladsch.flexmark.util.ast.Node;

/**
 * Handler that populates a resource's properties based on a YAML front matter entry
 *
 */
public class YamlFrontMatterHandler implements NodeHandler {

    @Override
    public boolean consume(final Node n, final ProcessingResult result) {
        final AbstractYamlFrontMatterVisitor vis = new AbstractYamlFrontMatterVisitor();
        vis.visit(n);
        if ( vis.getData().isEmpty() ) {
            return false;
        }

        for ( final Map.Entry<String, List<String>> entry : vis.getData().entrySet() ) {
            if ( entry.getValue().size() == 1) {
                result.properties.put(entry.getKey(), entry.getValue().get(0));
            } else {
                result.properties.put(entry.getKey(), entry.getValue().toArray(new String[0]));
            }
        }

        return true;
    }
}
