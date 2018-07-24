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
package org.apache.sling.mdresource.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.stream.Collectors;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        service = ResourceProvider.class,
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = MarkdownResourceProvider.Config.class, factory = true)
public class MarkdownResourceProvider extends ResourceProvider<Object> {
    
    @ObjectClassDefinition(name = "Apache Sling Markdown Resource Provider")
    public @interface Config {

        @AttributeDefinition(name="File system root",
                description="Filesystem directory mapped to the virtual resource tree. Expects a file named 'index.md' in order to provide resources")
        String provider_file();
        
        @AttributeDefinition(name = "Provider Root",
                description = "Location in the virtual resource tree where the " +
                "file system resources are mapped in. This property must not be an empty string.")
        String provider_root();
        
        // Internal Name hint for web console.
        String webconsole_configurationFactory_nameHint() default "{" + ResourceProvider.PROPERTY_ROOT + "}";
    }
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private String fsPath;
    private String repoPath;
    
    protected void activate(Config cfg) {
        
        fsPath = cfg.provider_file();
        repoPath = cfg.provider_root();
    }

    @Override
    public Resource getResource(ResolveContext<Object> ctx, String path, ResourceContext resourceContext,
            Resource parent) {
        
        log.info("getResource(" + path + ")");
        
        // try index.md file first
        Path filePath = Paths.get(fsPath, path, "index.md");
        File backingFile = filePath.toFile();
        if ( !backingFile.exists() ) {
            log.info("File at " + filePath + " does not exist");
            // try direct file .md next
            filePath = Paths.get(fsPath, path + ".md");
            backingFile = filePath.toFile();
            if ( !backingFile.exists() ) {
                log.info("File at " + filePath + " does not exist");
                log.info("Returning null");
                return null;
            }
        }
        
        log.info("Returning resource");
        return new MarkdownResource(ctx.getResourceResolver(), path, backingFile);
    }

    @Override
    public Iterator<Resource> listChildren(ResolveContext<Object> ctx, Resource parent) {
        log.info("listChildren(" + parent.getPath() + ")");
        
        Path root = Paths.get(fsPath, parent.getPath());
        
        try {
            return Files.list(root)
                .map( p -> asResource(p, Paths.get(parent.getPath()), ctx))
                .filter( r -> r != null )
                .collect(Collectors.toList())
                .iterator();
        } catch (IOException e) {
            // TODO better place for exception?
            throw new RuntimeException(e);
        }
    }

    private Resource asResource(Path path, Path parent, ResolveContext<Object> ctx) {
        File backingFile = path.toFile();

        if ( backingFile.isDirectory() ) {
            backingFile = new File(backingFile, "index.md");
            if ( backingFile.exists() && backingFile.canRead() ) {
                return asResource0(path, parent, ctx, backingFile);
            }
        }
        
        if ( backingFile.isFile() && backingFile.canRead() && backingFile.getName().endsWith(".md") && !backingFile.getName().equals("index.md")) {
        	Path potentialDirectory = Paths.get(backingFile.getAbsolutePath().substring(0, backingFile.getAbsolutePath().length() - ".md".length() ));
        	if ( potentialDirectory.resolve("index.md").toFile().exists() ) {
        		return null;
        	}
            return asResource0(path, parent, ctx, backingFile);
        }
        
        return null;
    }

    private Resource asResource0(Path path, Path parent, ResolveContext<Object> ctx, File backingFile) {
        Path fsRelativePath = Paths.get(fsPath).relativize(path);
        Path parentRelativePath = Paths.get("/").relativize(parent);
        
        return new MarkdownResource(ctx.getResourceResolver(), "/" + fsRelativePath.toString().replaceAll("\\.md$", ""), backingFile);
    }

}
