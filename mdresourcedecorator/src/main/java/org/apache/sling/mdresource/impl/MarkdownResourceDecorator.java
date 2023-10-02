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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceDecorator;
import org.apache.sling.api.resource.path.Path;
import org.apache.sling.api.resource.path.PathSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "org.apache.sling.resource.MarkdownResourceDecorator",
        service = ResourceDecorator.class,
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = MarkdownResourceDecorator.Config.class, factory = true)
public class MarkdownResourceDecorator implements ResourceDecorator {

    @ObjectClassDefinition(name = "Apache Sling Markdown Resource Decorator")
    public @interface Config {

        @AttributeDefinition(name = "Decoration Paths",
                description = "Resources contained in the tree below these paths are decorated. Patterns are supported, e.g. \"/content/**.md\".")
        String[] decoration_paths();

        @AttributeDefinition(name = "Decoration Extensions",
                description = "Only resources with these extensions are decorated. If set to \"*\" all resources match.")
        String[] decoration_extensions() default {"md"};

        @AttributeDefinition(name = "Decoration Resource Types",
                description = "Only resources with these resource types are decorated. If set to \"*\" all resources match.")
        String[] decoration_types() default {RESOURCE_TYPE_FILE};

        @AttributeDefinition(name = "Resource Type",
                description = "The resource type for the decorated resources")
        String resource_type() default "sling/markdown/file";

        @AttributeDefinition(name = "Source",
                description = "Set the source of the markdown, either InputStream (default) or Property",
                options = {
                        @Option(label = "Input Stream", value ="InputStream"),
                        @Option(label = "Property", value = "Property")
                })
        ResourceConfiguration.SourceType source_type() default ResourceConfiguration.SourceType.InputStream;

        @AttributeDefinition(name = "Source Markdown Property",
                description = "The property is used to read the markdown if source is set to Property.")
        String source_markdown_property();

        @AttributeDefinition(name = "Html Property",
                description = "Name of the property holding the rendered html")
        String html_property() default "jcr:description";

        @AttributeDefinition(name = "Markdown Property",
                description = "Name of the property holding the read markdown (optional)")
        String markdown_property();

        @AttributeDefinition(name = "Title Property",
                description = "Name of the property holding the title (optional)")
        String title_property() default "jcr:title";

        @AttributeDefinition(name = "Rewrite Links",
                description = "If enabled, links in the markdown are rewritten.")
        boolean rewrite_links() default true;
    }

    public static final Logger LOGGER = LoggerFactory.getLogger(MarkdownResourceDecorator.class);

    private static final String RESOURCE_TYPE_FILE = "nt:file";

    private final PathSet paths;
    private final Set<String> resourceTypes;
    private final ResourceConfiguration config = new ResourceConfiguration();
    private final Set<String> extensions;

    @Activate
    public MarkdownResourceDecorator(final Config cfg) {
        this.paths = PathSet.fromStringCollection(Arrays.stream(cfg.decoration_paths())
                .map(path -> path.contains("*") ? Path.GLOB_PREFIX.concat(path) : path)
                .collect(Collectors.toList()));
        final Set<String> exts =  new HashSet<>(Arrays.asList(cfg.decoration_extensions()));
        if (exts.contains("*") ) {
            this.extensions = null;
        } else {
            this.extensions = exts;
        }
        final Set<String> rts =  new HashSet<>(Arrays.asList(cfg.decoration_types()));
        if (rts.contains("*") ) {
            this.resourceTypes = null;
        } else {
            this.resourceTypes = rts;
        }
        this.config.sourceType = cfg.source_type();
        this.config.resourceType = cfg.resource_type();
        this.config.sourceMarkdownProperty = cleanInput(cfg.source_markdown_property());
        this.config.htmlProperty = cleanInput(cfg.html_property());
        this.config.titleProperty = cleanInput(cfg.title_property());
        this.config.markdownProperty = cleanInput(cfg.markdown_property());
        this.config.rewriteLinks = cfg.rewrite_links();
    }

    private String cleanInput(final String value) {
        if ( value != null && value.isBlank() ) {
            return null;
        }
        return value;
    }

    @Override
    public @Nullable Resource decorate(final @NotNull Resource resource) {
        // check resource type and path
        if ( (this.resourceTypes == null || this.resourceTypes.contains(resource.getResourceType()))
             && this.paths.matches( resource.getPath() ) != null ) {

            final int pos = resource.getName().lastIndexOf('.');
            if ( this.extensions == null || (pos != -1 && this.extensions.contains(resource.getName().substring(pos + 1))) ) {
                return new MarkdownResourceWrapper(resource, this.config);
            }
        }
        return null;
    }

    @Override
    public @Nullable Resource decorate(final @NotNull Resource resource, final @NotNull HttpServletRequest request) {
        // This method is deprecated but just in case....
        return this.decorate(null);
    }
}
