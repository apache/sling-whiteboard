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

import org.apache.sling.api.resource.Resource;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventProperties;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.apache.sling.sitemap.generator.SitemapGenerator.*;

/**
 * A utility class to create new {@link Event}s for sitemap storage operations.
 */
class SitemapEventUtil {

    private SitemapEventUtil() {
        super();
    }

    static Event newUpdateEvent(SitemapStorageInfo storageInfo, Resource sitemapRoot) {
        Map<String, Object> props = new HashMap<>(5);
        props.put(EVENT_PROPERTY_SITEMAP_NAME, storageInfo.getName());
        props.put(EVENT_PROPERTY_SITEMAP_ROOT, sitemapRoot.getPath());
        props.put(EVENT_PROPERTY_SITEMAP_URLS, storageInfo.getEntries());
        props.put(EVENT_PROPERTY_SITEMAP_STORAGE_PATH, storageInfo.getPath());
        props.put(EVENT_PROPERTY_SITEMAP_STORAGE_SIZE, storageInfo.getSize());
        return new Event(EVENT_TOPIC_SITEMAP_UPDATED, new EventProperties(props));
    }

    static Event newPurgeEvent(String path) {
        Map<String, Object> properties = Collections.singletonMap(EVENT_PROPERTY_SITEMAP_STORAGE_PATH, path);
        return new Event(EVENT_TOPIC_SITEMAP_PURGED, new EventProperties(properties));
    }
}
