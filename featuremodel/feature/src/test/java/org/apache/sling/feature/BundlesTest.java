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

import java.util.List;
import java.util.Map;

import org.junit.Test;

public class BundlesTest {

    @Test
    public void testIterator() {
        final Bundles bundles = new Bundles();
        bundles.add(createBundle("1/a/1", 1));
        bundles.add(createBundle("5/a/5", 5));
        bundles.add(createBundle("5/b/6", 5));
        bundles.add(createBundle("2/b/2", 2));
        bundles.add(createBundle("2/a/3", 2));
        bundles.add(createBundle("4/x/4", 4));

        int index = 1;
        for(final Map.Entry<Integer, List<Artifact>> entry : bundles.getBundlesByStartOrder().entrySet()) {
            for(final Artifact a : entry.getValue()) {
                assertEquals(entry.getKey().toString(), a.getId().getGroupId());
                assertEquals(index, a.getId().getOSGiVersion().getMajor());
                index++;
            }
        }
        assertEquals(7, index);
    }

    public static Artifact createBundle(final String id, final int startOrder) {
        final Artifact a = new Artifact(ArtifactId.parse(id));
        a.getMetadata().put(Artifact.KEY_START_ORDER, String.valueOf(startOrder));

        return a;
    }
}
