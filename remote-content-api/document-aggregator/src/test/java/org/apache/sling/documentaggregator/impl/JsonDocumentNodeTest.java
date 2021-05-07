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

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.apache.sling.documentaggregator.api.DocumentTree;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;

public class JsonDocumentNodeTest {

    private JsonDocumentNode node;

    @BeforeEach
    public void setup() {
        node = new JsonDocumentNode("name");
    }

    @Test
    public void emptyNode() {
        assertEquals("{}", node.adaptTo(String.class));
    }
    
    @Test
    public void withChild() {
        final DocumentTree.DocumentNode child = node.addChild("child");
        child.addValue("foo", "bar");
        child.addValue("number", 42);
        child.addValue("boolean", false);
        final String json = node.adaptTo(String.class);
        assertThat(json, hasJsonPath("child.foo", equalTo("bar")));
        assertThat(json, hasJsonPath("child.number", equalTo("42")));
        assertThat(json, hasJsonPath("child.boolean", equalTo("false")));
    }

    public void withChildLevels() {
        final DocumentTree.DocumentNode child = node.addChild("one").addChild("two").addChild("three");
        child.addValue("foo", "barre");
        final String json = node.adaptTo(String.class);
        assertThat(json, hasJsonPath("child.one.two.three", equalTo("barre")));
    }

    @Test
    public void withArray() {
        final DocumentTree.DocumentNode child = node.addChild("child");
        child.addValue("foo", new String [] { "one", "two", "three"});
        final String json = node.adaptTo(String.class);
        assertThat(json, hasJsonPath("child.foo", equalTo("[one, two, three]")));
    }

    @Test
    public void withArrayAndExtraCloseCall() {
        final DocumentTree.DocumentNode child = node.addChild("child");
        child.addValue("foo", new String [] { "one", "two", "three"});
        child.close();
        node.close();
        node.close();
        final String json = node.adaptTo(String.class);
        assertThat(json, hasJsonPath("child.foo", equalTo("[one, two, three]")));
    }

    @Test
    public void wrongAdaptTo() {
        try {
            node.adaptTo(Map.class);
        } catch(IllegalArgumentException ix) {
            assertTrue(ix.getMessage().contains("can only adapt"));
        }
        
    }
}
