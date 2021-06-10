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

import java.util.function.Supplier;

import org.apache.sling.api.resource.Resource;

/** Base class for folders and documents */
public class ContentItem {
    protected final Resource resource;
    protected ContentItemHeader header;
    protected final Supplier<ContentGenerator> contentGeneratorSupplier;

    ContentItem(Resource r, Supplier<ContentGenerator> contentGeneratorSupplier) {
        this.resource = r;
        this.contentGeneratorSupplier = contentGeneratorSupplier;
    }

    public String getPath() {
        return resource.getPath();
    }

    public ContentItemHeader getHeader() {
        if(header == null) {
            header = new ContentItemHeader(resource);
        }
        return header;
    }
}