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

import java.io.Reader;
import java.io.StringReader;
import java.net.URL;

import org.apache.sling.graphql.schema.aggregator.api.PartialSchemaProvider;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@PartialSchemaProvider} build out of a Bundle entry, which must be a valid
 *  partial schema file.
 */
class BundleEntrySchemaProvider implements PartialSchemaProvider {
    private static final Logger log = LoggerFactory.getLogger(BundleEntrySchemaProvider.class.getName());
    private final URL url;
    private final String key;
    private final long bundleId;

    private BundleEntrySchemaProvider(Bundle b, URL bundleEntry) {
        this.url = bundleEntry;
        this.bundleId = b.getBundleId();
        this.key = String.format("%s(%d):%s", b.getSymbolicName(), b.getBundleId(), bundleEntry.toString());
    }

    static BundleEntrySchemaProvider forBundle(Bundle b, String entryPath) {
        final URL entry = b.getEntry(entryPath);
        if(entry == null) {
            log.info("Entry {} not found for bundle {}", entryPath, b.getSymbolicName());
            return null;
        } else {
            // TODO validate entry?
            return new BundleEntrySchemaProvider(b, entry);
        }
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof BundleEntrySchemaProvider) {
            return ((BundleEntrySchemaProvider)other).key.equals(key);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    public String toString() {
        return String.format("%s: %s", getClass().getSimpleName(), key);
    }

    public String getName() {
        return url.toString();
    }

    public long getBundleId() {
        return bundleId;
    }

    @Override
    public @NotNull Reader getSectionContent(String sectionName) {
        return new StringReader(String.format("Fake section %s for %s", sectionName, key));
    }

    @Override
    public @NotNull Reader getBodyContent() {
        return new StringReader(String.format("Fake body for %s", key));
    }
}