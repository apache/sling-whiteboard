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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * A simple {@code ResourceProvider} for the file system.
 */
@Component(
        service = ResourceProvider.class,
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = SFSResourceProvider.Config.class, factory = true)
public class SFSResourceProvider extends ResourceProvider<Object> {

    @ObjectClassDefinition(name = "Apache Sling Simple File System Resource Provider", description = "Configure an instance of the file system resource provider in terms of provider root and file system location")
    public @interface Config {

        @AttributeDefinition(name = "File System Root", description = "File system directory mapped to the virtual "
                + "resource tree. This property must not be an empty string. If the path is "
                + "relative it is resolved against sling.home or the current working directory. "
                + "The path may be a file or folder. If the path does not address an existing "
                + "file or folder, an empty folder is created.")
        String provider_file();

        @AttributeDefinition(name = "Provider Root", description = "Location in the virtual resource tree where the "
                + "file system resources are mapped in. This property must not be an empty string.")
        String provider_root();

        // Internal Name hint for web console.
        String webconsole_configurationFactory_nameHint() default "{"
                + ResourceProvider.PROPERTY_ROOT + "}";
    }

    private final String pathPrefix;

    private final String home;

    @Activate
    public SFSResourceProvider(final Config config) {
        if (config.provider_root().endsWith("/")) {
            this.pathPrefix = config.provider_root();
        } else {
            this.pathPrefix = config.provider_root().concat("/");
        }
        this.home = config.provider_file();
    }

    @Override
    public Resource getResource(final ResolveContext<Object> ctx,
            final String path,
            final ResourceContext resourceContext,
            final Resource parent) {
        final String rsrcPath = path.length() < this.pathPrefix.length() ? ""
                : path.substring(this.pathPrefix.length());

        // try one to one mapping
        final Path filePath = Paths.get(this.home, rsrcPath.replace('/', File.separatorChar));
        final File file = filePath.toFile();
        if (file.isDirectory() || file.canRead()) {
            return new FileResource(ctx.getResourceResolver(), path, file);
        }

        final int lastSlash = rsrcPath.lastIndexOf('/');
        final int lastDot = rsrcPath.lastIndexOf('.');

        if ( lastDot > lastSlash ) {
            // has extension, don't try anything else
            return null;
        }
        return null;
    }

    @Override
    public Iterator<Resource> listChildren(final ResolveContext<Object> ctx, final Resource parent) {
        if (FileResource.RESOURCE_TYPE_FOLDER.equals(parent.getResourceType())) {
            final File file = parent.adaptTo(File.class);
            if (file != null && file.isDirectory()) {
                final List<File> children = new ArrayList<>();
                // TODO filter out some files
                for (final File c : file.listFiles()) {
                    children.add(c);
                }
                Collections.sort(children);
                final Iterator<File> i = children.iterator();
                return new Iterator<Resource>() {

                    @Override
                    public boolean hasNext() {
                        return i.hasNext();
                    }

                    @Override
                    public Resource next() {
                        final File file = i.next();
                        return new FileResource(ctx.getResourceResolver(),
                                parent.getPath().concat("/").concat(file.getName()), file);
                    }
                };
            }
        }
        return null;
    }
}
