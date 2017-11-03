/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.feature;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

public class BundlesTest {

    @Test
    public void testIterator() {
        final Bundles bundles = new Bundles();
        bundles.add(1, new Artifact(ArtifactId.parse("1/a/1")));
        bundles.add(5, new Artifact(ArtifactId.parse("5/a/5")));
        bundles.add(5, new Artifact(ArtifactId.parse("5/b/6")));
        bundles.add(2, new Artifact(ArtifactId.parse("2/b/2")));
        bundles.add(2, new Artifact(ArtifactId.parse("2/a/3")));
        bundles.add(4, new Artifact(ArtifactId.parse("4/x/4")));

        int index = 1;
        for(final Map.Entry<Integer, Artifact> entry : bundles) {
            assertEquals(entry.getKey().toString(), entry.getValue().getId().getGroupId());
            assertEquals(index, entry.getValue().getId().getOSGiVersion().getMajor());
            index++;
        }
        assertEquals(7, index);
    }
}
