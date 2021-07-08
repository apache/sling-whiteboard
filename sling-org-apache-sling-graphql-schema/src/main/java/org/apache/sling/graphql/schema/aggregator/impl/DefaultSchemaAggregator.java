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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.HashSet;

import org.apache.commons.io.IOUtils;
import org.apache.sling.graphql.schema.aggregator.api.SchemaAggregator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.util.tracker.BundleTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.sling.graphql.schema.aggregator.impl.PartialConstants.*;

@Component(service = SchemaAggregator.class)
public class DefaultSchemaAggregator implements SchemaAggregator {
    private static final Logger log = LoggerFactory.getLogger(DefaultSchemaAggregator.class.getName());
    private ProviderBundleTracker tracker;
    private ServiceRegistration<?> trackerRegistration;

    @Activate
    public void activate(BundleContext ctx) {
        tracker = new ProviderBundleTracker(ctx);
        trackerRegistration = ctx.registerService(BundleTracker.class, tracker, null);
        tracker.open();
    }

    @Deactivate
    public void deactivate(BundleContext ctx) {
        if(trackerRegistration != null) {
            tracker.close();
            trackerRegistration.unregister();
            trackerRegistration = null;
        }
    }

    private void copySection(Set<Partial> selected, String sectionName, Writer target) throws IOException {
        target.write(String.format("%n%s {%n", sectionName));
        for(Partial p : selected) {
            writeSourceInfo(target, p);
            final Optional<Partial.Section> section = p.getSection(sectionName);
            if(section.isPresent()) {
                IOUtils.copy(section.get().getContent(), target);
                target.write(String.format("%n"));
            }
        }
        target.write(String.format("%n}%n"));
    }

    private void writeSourceInfo(Writer target, Partial p) throws IOException {
        target.write(String.format("%n# %s.source=%s%n", getClass().getSimpleName(), p.getName()));
    }

    @Override
    public void aggregate(Writer target, String ...providerNamesOrRegexp) throws IOException {
        final String info = String.format("Schema aggregated by %s%n", getClass().getSimpleName());
        target.write(String.format("# %s", info));

        // build list of selected providers
        final Map<String, Partial> providers = tracker.getSchemaProviders();
        if(log.isDebugEnabled()) {
            log.debug("Aggregating schemas, request={}, providers={}", Arrays.asList(providerNamesOrRegexp), providers.keySet());
        }
        final Set<String> missing = new HashSet<>();
        final SortedSet<Partial> selected = selectProviders(providers, missing, providerNamesOrRegexp);

        if(!missing.isEmpty()) {
            log.debug("Requested providers {} not found in {}", missing, providers.keySet());
            throw new IOException(String.format("Missing providers: %s", missing));
        }

        // copy sections
        final String [] sections = {
            S_PROLOGUE,
            S_QUERY,
            S_MUTATION,
            S_TYPES
        };
        for(String s : sections) {
            copySection(selected, s, target);
        }
        target.write(String.format("%n# End of %s", info));
    }

    static SortedSet<Partial> selectProviders(Map<String, Partial> providers, Set<String> missing, String ... providerNamesOrRegexp) {
        final SortedSet<Partial> result= new TreeSet<>();
        for(String str : providerNamesOrRegexp) {
            final Pattern p = toRegexp(str);
            if(p != null) {
                log.debug("Selecting providers matching {}", p);
                providers.entrySet().stream()
                    .filter(e -> p.matcher(e.getKey()).matches())
                    .forEach(e -> result.add(e.getValue()));
            } else {
                log.debug("Selecting provider with key={}", str);
                final Partial psp = providers.get(str);
                if(psp == null) {
                    missing.add(str);
                    continue;
                }
                result.add(psp);
            }
        }
        return result;
    }

    static Pattern toRegexp(String input) {
        if(input.startsWith("/") && input.endsWith("/")) {
            return Pattern.compile(input.substring(1, input.length() - 1));
        }
        return null;
    }
}
