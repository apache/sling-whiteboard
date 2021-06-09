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

package org.apache.sling.remotecontent.contentmodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.sling.api.resource.Resource;

/** Header for ContentItems */
public class ContentItemHeader {
    protected final Resource resource;
    private List<Link> links;

    ContentItemHeader(Resource r) {
        resource = r;
    }

    private void setupLinksIfNeeded() {
        if(links == null) {
            links = new ArrayList<>();
            links.add(new Link("self", resource.getPath()));
        }
    }

    public Collection<Link> getLinks() {
        setupLinksIfNeeded();
        return Collections.unmodifiableList(links);
    }

    public String getResourceType() {
        return resource.getResourceType();
    }

    public String getResourceSuperType() {
        return resource.getResourceSuperType();
    }

    public String getParent() {
        final Resource parent = resource.getParent();
        return parent == null ? null : parent.getPath();
    }

    public String getTitle() {
        return null;
    }

    public String getDescription() {
        return null;
    }

    public String getSummary() {
        return null;
    }
}