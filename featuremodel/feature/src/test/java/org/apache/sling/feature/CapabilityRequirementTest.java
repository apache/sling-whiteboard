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
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class CapabilityRequirementTest {
    @Test
    public void testCapability() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("org.foo", "1234");
        attrs.put("bar", 456);
        Map<String, String> dirs = new HashMap<>();
        dirs.put("my_dir", "my_value");
        Capability c = new OSGiCapability("org.foo", attrs, dirs);
        assertEquals("org.foo", c.getNamespace());
        assertEquals(attrs, c.getAttributes());
        assertEquals(dirs, c.getDirectives());
        assertNull(c.getResource());
    }

    @Test
    public void testRequirement() {
        Resource tr = new TestResource();
        Requirement r = new OSGiRequirement(tr, "testing",
                Collections.emptyMap(), Collections.emptyMap());
        assertEquals(tr, r.getResource());
        assertEquals(0, r.getAttributes().size());
        assertEquals(0, r.getDirectives().size());
    }

    private static class TestResource implements Resource {
        @Override
        public List<Capability> getCapabilities(String namespace) {
            return Collections.emptyList();
        }

        @Override
        public List<Requirement> getRequirements(String namespace) {
            return Collections.emptyList();
        }
    }
}
