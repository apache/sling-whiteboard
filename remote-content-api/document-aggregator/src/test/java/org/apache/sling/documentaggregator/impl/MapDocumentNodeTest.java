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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.regex.Pattern;

import org.apache.sling.documentaggregator.api.DocumentTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MapDocumentNodeTest {
    private MapDocumentNode node;
    
    @BeforeEach
    public void setup() {
        node = new MapDocumentNode("name");
    }

    @Test
    public void emptyNode() {
        final Map<String, Object> m = node.adaptTo(Map.class);
        assertEquals(0, m.size());
    }

    private static String despace(String s) {
        return s.replaceAll(" ", "");
    }

    @Test
    public void childLevels() {
        node.addValue("root", "yes");
        DocumentTree.DocumentNode child = node.addChild("one");
        child.addValue("none", "vone");
        child = child.addChild("two");
        child.addValue("ntwo", "vtwo");
        final Map<String, Object> m = node.adaptTo(Map.class);
        assertEquals(2, m.size());
        assertEquals("{root=yes,one={none=vone,two={ntwo=vtwo}}}", despace(m.toString()));
    }

    @Test
    public void arrayValue() {
        node.addChild("A").addChild("B").addValue("array", new String [] { "cat", "dog", "mouse" });
        final String str = node.toString();
        final Pattern pattern = Pattern.compile(".A=.B=.array=\\[Ljava.lang.String;.*.....");
        assertTrue(pattern.matcher(str).matches(), "Expecting output '" + str + "' to match '" + pattern + "'");
    }

    @Test
    public void wrongAdaptTo() {
        try {
            node.adaptTo(String.class);
        } catch(IllegalArgumentException ix) {
            assertTrue(ix.getMessage().contains("can only adapt"));
        }
        
    }
}
