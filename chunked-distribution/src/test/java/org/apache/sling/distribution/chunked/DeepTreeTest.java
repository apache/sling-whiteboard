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
package org.apache.sling.distribution.chunked;

import static org.junit.Assert.assertThat;

import java.util.List;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.osgi.framework.BundleContext;

public class DeepTreeTest {

    @Test
    public void test() throws PersistenceException {
        BundleContext context = MockOsgi.newBundleContext();
        try (ResourceResolver resourceResolver = MockSling.newResourceResolver(ResourceResolverType.JCR_OAK, context)) {
            Resource root = resourceResolver.getResource("/");
            Resource base = ResourceHelper.createResource(resourceResolver, root, "mybasepath");
            for (int c=0; c<10;c++) {
                Resource sub = ResourceHelper.createResource(resourceResolver, base, "sub" + Integer.valueOf(c).toString());
                for (int c2=0; c2<10;c2++) {
                    ResourceHelper.createResource(resourceResolver, sub, "subsub" + Integer.valueOf(c2).toString());
                }
            }
            
            List<String> paths = DeepTree.getPaths(base);
            assertThat(paths.size(), Matchers.equalTo(100 + 10 + 1));
        }
    }
}
