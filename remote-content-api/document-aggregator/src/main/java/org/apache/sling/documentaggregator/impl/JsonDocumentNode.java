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

package org.apache.sling.documentaggregator.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.apache.sling.documentaggregator.api.DocumentTree;
import org.apache.sling.documentaggregator.api.DocumentTree.DocumentNode;

/** A DocumentNode that outputs to a JSON document */
public class JsonDocumentNode implements DocumentTree.DocumentNode {

    private final String name;
    private final JsonObjectBuilder builder;
    private List<JsonDocumentNode> children;

    JsonDocumentNode(String name) {
        this.name = name;
        this.builder = Json.createObjectBuilder();
    }

    @Override
    public DocumentNode addChild(String name) {
        if(children == null) {
            children = new ArrayList<>();
        }
        final JsonDocumentNode child = new JsonDocumentNode(name);
        children.add(child);
        return child;
    }

    @Override
    public DocumentNode addValue(String name, Object value) {
        builder.add(name, String.valueOf(value));
        return this;
    }

    @Override
    public DocumentNode addValue(String name, Object[] value) {
        builder.add(name, String.valueOf(Arrays.asList(value)));
        return this;
    }

    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if(type.equals(String.class)) {
            close();
            return (AdapterType)builder.build().toString();
        }
        throw new IllegalArgumentException("For now, can only adapt to a String");
    }

    @Override
    public void close() {
        if(children != null) {
            children.stream().forEach(c -> {
                c.close();
                builder.add(c.name, c.builder);
            });
        }
    }
}