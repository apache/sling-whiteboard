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
package org.apache.sling.sitemap;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.sitemap.builder.Sitemap;
import org.apache.sling.sitemap.generator.SitemapGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static org.junit.jupiter.api.Assertions.fail;

public abstract class TestGenerator implements SitemapGenerator {

    private Map<String, List<String>> names = new HashMap<>();
    private Map<String, List<String>> onDemandNames = new HashMap<>();

    public TestGenerator() {
        this.names.put(null, Collections.emptyList());
        this.onDemandNames.put(null, Collections.emptyList());
    }

    public void setNames(String... names) {
        this.names.put(null, Arrays.asList(names));
    }

    public void setNames(Resource resource, String... names) {
        this.names.put(resource.getPath(), Arrays.asList(names));
    }

    public void setOnDemandNames(String... names) {
        this.onDemandNames.put(null, Arrays.asList(names));
    }

    public void setOnDemandNames(Resource resource, String... names) {
        this.onDemandNames.put(resource.getPath(), Arrays.asList(names));
    }

    @Override
    @NotNull
    public Set<String> getNames(@NotNull Resource sitemapRoot) {
        return new HashSet<>(names.containsKey(sitemapRoot.getPath())
                ? names.get(sitemapRoot.getPath())
                : names.get(null));
    }

    @Override
    public @NotNull Set<String> getOnDemandNames(@NotNull Resource sitemapRoot) {
        return new HashSet<>(onDemandNames.containsKey(sitemapRoot.getPath())
                ? onDemandNames.get(sitemapRoot.getPath())
                : onDemandNames.get(null));
    }

    @Override
    public void generate(@NotNull Resource sitemapRoot, @NotNull String name, @NotNull Sitemap sitemap, @NotNull GenerationContext context) throws SitemapException {
        fail();
    }
}
