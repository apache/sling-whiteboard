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
import java.util.Map;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.jetbrains.annotations.NotNull;
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

        @AttributeDefinition(name = "Modifiable", description = "Whether the resources provided by this provider are "
                + "modifiable.")
        boolean provider_modifiable() default false;

        // Internal Name hint for web console.
        String webconsole_configurationFactory_nameHint() default "{"
                + ResourceProvider.PROPERTY_ROOT + "}";
    }

    private final String pathPrefix;

    private final String home;

    private final boolean modifiable;

    @Activate
    public SFSResourceProvider(final Config config) {
        if (config.provider_root().endsWith("/")) {
            this.pathPrefix = config.provider_root();
        } else {
            this.pathPrefix = config.provider_root().concat("/");
        }
        this.home = config.provider_file();
        this.modifiable = config.provider_modifiable();
    }

    private boolean include(final File file) {
        return file.exists()
            && !file.getName().startsWith(".")
            && (file.isDirectory() || file.canRead());
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
        if (include(file)) {
            return new FileResource(ctx.getResourceResolver(), path, file, modifiable);
        }
        return null;
    }

    @Override
    public Iterator<Resource> listChildren(final ResolveContext<Object> ctx, final Resource parent) {
        if (FileResource.RESOURCE_TYPE_FOLDER.equals(parent.getResourceType())) {
            final File file = parent.adaptTo(File.class);
            if (file != null && file.isDirectory()) {
                final List<File> children = new ArrayList<>();
                for (final File c : file.listFiles()) {
                    if (include(c)) {
                        children.add(c);
                    }
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
                                parent.getPath().concat("/").concat(file.getName()), file, modifiable);
                    }
                };
            }
        }
        return null;
    }

    @Override
    public void commit(@NotNull ResolveContext<Object> ctx) throws PersistenceException {
        // gets only called if modifiable is true
        super.commit(ctx);
    }

    @Override
    public boolean copy(@NotNull ResolveContext<Object> ctx, @NotNull String srcAbsPath, @NotNull String destAbsPath)
            throws PersistenceException {
        // gets only called if modifiable is true
        return super.copy(ctx, srcAbsPath, destAbsPath);
    }

    @Override
    public @NotNull Resource create(@NotNull ResolveContext<Object> ctx, String path, Map<String, Object> properties)
            throws PersistenceException {
        // gets only called if modifiable is true
        return super.create(ctx, path, properties);
    }

    @Override
    public void delete(@NotNull ResolveContext<Object> ctx, @NotNull Resource resource) throws PersistenceException {
        // gets only called if modifiable is true
        super.delete(ctx, resource);
    }

    @Override
    public boolean isLive(@NotNull ResolveContext<Object> ctx) {
        // gets only called if modifiable is true
        return super.isLive(ctx);
    }

    @Override
    public boolean move(@NotNull ResolveContext<Object> ctx, @NotNull String srcAbsPath, @NotNull String destAbsPath)
            throws PersistenceException {
        // gets only called if modifiable is true
        return super.move(ctx, srcAbsPath, destAbsPath);
    }

    @Override
    public void revert(@NotNull ResolveContext<Object> ctx) {
        // gets only called if modifiable is true
        super.revert(ctx);
    }
}
