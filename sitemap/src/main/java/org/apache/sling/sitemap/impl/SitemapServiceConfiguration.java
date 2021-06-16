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
package org.apache.sling.sitemap.impl;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(service = SitemapServiceConfiguration.class)
@Designate(ocd = SitemapServiceConfiguration.Configuration.class)
public class SitemapServiceConfiguration {

    @ObjectClassDefinition(name = "Apache Sling Sitemap - Sitemap Service")
    @interface Configuration {

        @AttributeDefinition(name = "Max Size", description = "The maximum size of a sitemap in bytes. Files that " +
                "exceed the size will be flagged with a warning.")
        int maxSize() default 10 * 1024 * 1024;

        @AttributeDefinition(name = "Max Entries", description = "The maximum number of urls of a sitemap. Files " +
                "that exceed this number will be flagged with a warning.")
        int maxEntries() default 50000;
    }

    private int maxSize;
    private int maxEntries;

    @Activate
    protected void activate(Configuration configuration) {
        maxSize = configuration.maxSize();
        maxEntries = configuration.maxEntries();
    }

    public int getMaxSize() {
        return maxSize;
    }

    public int getMaxEntries() {
        return maxEntries;
    }
}
