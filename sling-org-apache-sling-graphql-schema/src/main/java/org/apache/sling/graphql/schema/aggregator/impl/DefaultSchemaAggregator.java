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

package org.apache.sling.graphql.schema.aggregator.impl;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.sling.graphql.schema.aggregator.api.PartialSchemaProvider;
import org.apache.sling.graphql.schema.aggregator.api.SchemaAggregator;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

@Component(service = SchemaAggregator.class)
public class DefaultSchemaAggregator implements SchemaAggregator {
    private ProviderBundleTracker tracker;

    @Activate
    public void activate(BundleContext ctx) {
        tracker = new ProviderBundleTracker(ctx);
    }

    private void copySection(List<PartialSchemaProvider> selected, String sectionName, Writer target) throws IOException {
        target.write(String.format("%n%s {%n", sectionName));
        for(PartialSchemaProvider p : selected) {
            writeSourceInfo(target, p);
            IOUtils.copy(p.getSectionContent(sectionName), target);
            target.write(String.format("%n"));
        }
        target.write(String.format("%n}%n"));
    }

    private void writeSourceInfo(Writer target, PartialSchemaProvider psp) throws IOException {
        target.write(String.format("%n# %s.source=%s%n", getClass().getSimpleName(), psp.getName()));
    }

    public void aggregate(Writer target, String ...providerNames) throws IOException {
        final String info = String.format("Schema aggregated by %s%n", getClass().getSimpleName());
        target.write(String.format("# %s", info));

        // build list of selected providers
        final Map<String, PartialSchemaProvider> providers = tracker.getSchemaProviders();
        final List<PartialSchemaProvider> selected = new ArrayList<>();
        final List<String> missing = new ArrayList<>();
        for(String provider : providerNames) {
            final PartialSchemaProvider psp = providers.get(provider);
            if(psp == null) {
                missing.add(provider);
                continue;
            }
            selected.add(psp);
        }

        if(!missing.isEmpty()) {
            throw new IOException(String.format("Missing providers: %s", missing));
        }

        // copy sections
        copySection(selected, "query", target);
        copySection(selected, "mutation", target);
        for(PartialSchemaProvider p : selected) {
            writeSourceInfo(target, p);
            IOUtils.copy(p.getBodyContent(), target);
        }
        target.write(String.format("%n# End of %s", info));
    }
}
