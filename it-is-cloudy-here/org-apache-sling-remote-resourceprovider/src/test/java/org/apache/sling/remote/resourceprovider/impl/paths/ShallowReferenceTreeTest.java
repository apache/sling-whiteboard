/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Licensed to the Apache Software Foundation (ASF) under one
 ~ or more contributor license agreements.  See the NOTICE file
 ~ distributed with this work for additional information
 ~ regarding copyright ownership.  The ASF licenses this file
 ~ to you under the Apache License, Version 2.0 (the
 ~ "License"); you may not use this file except in compliance
 ~ with the License.  You may obtain a copy of the License at
 ~
 ~   http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package org.apache.sling.remote.resourceprovider.impl.paths;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShallowReferenceTreeTest {

    private Set<String> before;
    private Set<String> after;
    private Set<String> removed;
    private ShallowReferenceTree tree;

    @BeforeEach
    public void beforeEach() {
        removed = new HashSet<>();
    }

    @AfterEach
    public void afterEach() {
        before = null;
        after = null;
        removed = null;
    }

    @Test
    void testSimpleRemoval() {
        prepareTree(
                Set.of(
                        "/content", // root folder will also be added
                        "/content/demo",
                        "/content/demo/a/test-1", // a folder is extra
                        "/content/demo/a/test-2",
                        "/content/demo/a/b/test-1", // b folder is extra
                        "/content/demo/a/b/test-2",
                        "/content/demo/b/test-1", // b folder is extra
                        "/content/test-1"
                )
        );
        assertEquals(12, tree.getReferences().size());

        ShallowReference root = tree.getReference("/");
        assertNotNull(root);
        assertNull(tree.getParent(root));

        tree.remove("/content/demo/a/b/test-2");
        assertEquals(11, tree.getReferences().size());
        assertEquals(Set.of("/content/demo/a/b/test-2"), removed);
    }

    @Test
    void testDeepRemoval() {
        prepareTree(
                Set.of(
                        "/content", // root folder will also be added
                        "/content/demo",
                        "/content/demo/a/test-1", // a folder is extra
                        "/content/demo/a/test-2",
                        "/content/demo/a/b/test-1", // b folder is extra
                        "/content/demo/a/b/test-2",
                        "/content/demo/b/test-1", // b folder is extra
                        "/content/test-1"
                )
        );
        assertEquals(12, tree.getReferences().size());
        tree.remove("/content/demo/a");
        assertEquals(6, tree.getReferences().size());
        assertEquals(
                Set.of(
                        "/content/demo/a",
                        "/content/demo/a/test-1",
                        "/content/demo/a/test-2",
                        "/content/demo/a/b",
                        "/content/demo/a/b/test-1",
                        "/content/demo/a/b/test-2"
                ),
                removed
        );
    }

    @Test
    void testUpdates() {
        Set<String> expectedReferencePaths = Set.of("/", "/demo", "/demo/.sling.json");

        ShallowReference reference = new ShallowReference("/demo/.sling.json");
        reference.markProvidedResource("/content/demo/test-1");
        tree = new ShallowReferenceTree();
        tree.add(reference);
        assertEquals(3, tree.getReferences().size());
        assertEquals(expectedReferencePaths,
                new HashSet<>(tree.getReferences().stream().map(ShallowReference::getPath).collect(Collectors.toList())));

        assertFalse(tree.add(reference));

        ShallowReference reference1 = new ShallowReference("/demo/.sling.json");
        reference1.markProvidedResource("/content/demo/test-1");
        assertFalse(tree.add(reference1));
        reference1.markProvidedResource("/content/demo/test-2");
        assertTrue(tree.add(reference1));

        assertEquals(expectedReferencePaths,
                new HashSet<>(tree.getReferences().stream().map(ShallowReference::getPath).collect(Collectors.toList())));
    }

    private void prepareTree(Set<String> initial) {
        before = Set.copyOf(initial);
        tree = new ShallowReferenceTree(reference -> removed.add(reference.getPath()));
        if (before != null) {
            for (String path : before) {
                tree.add(new ShallowReference(path));
            }
        }
    }


}
