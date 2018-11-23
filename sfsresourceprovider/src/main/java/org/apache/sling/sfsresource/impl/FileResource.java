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
package org.apache.sling.sfsresource.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.AbstractResource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FileResource extends AbstractResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileResource.class);

    /**
     * The resource type for file system files mapped into the resource tree by the
     * {@link SFSResourceProvider} (value is "nt:file").
     */
    static final String RESOURCE_TYPE_FILE = "nt:file";

    /**
     * The resource type for file system folders mapped into the resource tree by
     * the {@link SFSResourceProvider} (value is "nt:folder").
     */
    static final String RESOURCE_TYPE_FOLDER = "nt:folder";

    private final ResourceResolver resolver;
    private final String path;
    private final File file;

    private final ResourceMetadata metadata;

    public FileResource(final ResourceResolver resourceResolver, final String path, final File file) {
        this.resolver = resourceResolver;
        this.path = path;
        this.file = file;
        this.metadata = new ResourceMetadata();
        this.metadata.setModificationTime(file.lastModified());
        this.metadata.setResolutionPath(path);
        if (file.isFile()) {
            this.metadata.setContentLength(file.length());
        }
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getResourceType() {
        return this.file.isDirectory() ? RESOURCE_TYPE_FOLDER : RESOURCE_TYPE_FILE;
    }

    @Override
    public String getResourceSuperType() {
        return null;
    }

    @Override
    public ResourceMetadata getResourceMetadata() {
        return metadata;
    }

    @Override
    public ResourceResolver getResourceResolver() {
        return resolver;
    }

    /**
     * Returns an adapter for this resource. This implementation supports
     * <code>File</code>, <code>InputStream</code> and <code>URL</code> plus those
     * supported by the adapter manager.
     */
    @Override
    public <AdapterType> AdapterType adaptTo(final Class<AdapterType> type) {
        if (type == File.class) {
            return type.cast(this.file);
        } else if (type == InputStream.class) {
            if (file.isFile() && file.canRead()) {
                try {
                    return type.cast(new FileInputStream(file));
                } catch (final IOException ioe) {
                    LOGGER.info("adaptTo: Cannot open a stream on the file " + file, ioe);
                }
            } else {
                LOGGER.debug("adaptTo: File {} is not a readable file", file);
            }
        } else if (type == URL.class) {
            try {
                return type.cast(file.toURI().toURL());
            } catch (MalformedURLException mue) {
                LOGGER.info("adaptTo: Cannot convert the file path " + file + " to an URL", mue);
            }
        } else if (type == ValueMap.class) {
            return type.cast(getValueMap());
        }

        return super.adaptTo(type);
    }

    @Override
    public ValueMap getValueMap() {
        Map<String, Object> props = new HashMap<>();
        props.put("sling:resourceType", getResourceType());

        return new ValueMapDecorator(props);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ", path: " + path + ", file: " + file;
    }
}
