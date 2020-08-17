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
package org.apache.sling.feature.extension.unpack;


import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Directive;
import org.apache.felix.utils.manifest.Parser;
import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.builder.ArtifactProvider;
import org.apache.sling.feature.io.IOUtils;

public class Unpack
{
    private final Map<String, Map<String, String>> registry;
    private final String defaultMapping;

    private Unpack(Map<String, Map<String, String>> registry) {
        this.registry = registry;
        defaultMapping = registry.entrySet().stream()
            .filter(entry -> Boolean.parseBoolean(entry.getValue().get("default")))
            .findFirst()
            .map(entry -> entry.getKey())
            .orElse(null);
    }

    public boolean handle(Extension extension, ArtifactProvider provider) {
        return handle(extension, provider, this::unpack);
    }

    public boolean handle(Extension extension, ArtifactProvider provider, BiConsumer<URL, Map<String, String>> handler)
    {
        if (extension.getType() == ExtensionType.ARTIFACTS &&
            this.registry.containsKey(extension.getName())) {
            for (Artifact artifact : extension.getArtifacts()) {
                String dir = this.registry.get(extension.getName()).get("dir");
                boolean override = Boolean.parseBoolean(this.registry.get(extension.getName()).get("override"));
                URL url = provider.provide(artifact.getId());
                Map<String, String> context = new HashMap<>();
                context.put("dir", dir);
                context.put("override", Boolean.toString(override));
                handler.accept(url, context);
            }
            return true;
        } else {
            return false;
        }
    }

    public void unpack(URL url, Map<String, String> context) {
        try
        {
            String dir = context.get("dir");
            boolean override;
            if (dir == null && this.defaultMapping != null) {
                dir = this.registry.get(defaultMapping).get("dir");
                override = Boolean.parseBoolean(this.registry.get(defaultMapping).get("override"));
            }
            else {
                override = Boolean.parseBoolean(context.get("override"));
            }

            if (dir == null) {
                throw new IllegalStateException("No target dir and no default configured");
            }
            unpack(dir, url, override);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static Unpack fromMapping(String mapping)
    {
        Map<String, Map<String, String>> registry = new HashMap<>();

        // Syntax: system-fonts;dir:=abc;overwrite:=true,customer-fonts;dir:=eft;default=true
        Clause[] extClauses = Parser.parseHeader(mapping);
        for (Clause c : extClauses) {
            Map<String,String> cfg = new HashMap<>();

            for (Directive d : c.getDirectives()) {
                cfg.put(d.getName(), d.getValue());
            }
            registry.put(c.getName(), Collections.unmodifiableMap(cfg));
        }
        return new Unpack(registry);
    }

    private void unpack(String dir, URL url, boolean override) throws IOException {
        File base = new File(dir);
        if (!base.isDirectory() && !base.mkdirs()) {
            throw new IOException("Unable to find or created base dir: " + base);
        }

        try (JarFile jarFile = IOUtils.getJarFileFromURL(url, true, null)) {
            jarFile.stream().filter(entry -> !entry.isDirectory() && !entry.getName().toLowerCase().startsWith("meta-inf/")).forEach(entry -> {
                File target = new File(base, entry.getName());
                if (target.getParentFile().toPath().startsWith(base.toPath())) {
                    if (target.getParentFile().isDirectory() || target.getParentFile().mkdirs()) {
                        try
                        {
                            if (override)
                            {
                                Files.copy(jarFile.getInputStream(entry), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            }
                            else if (!target.exists())
                            {
                                try
                                {
                                    Files.copy(jarFile.getInputStream(entry), target.toPath());
                                } catch (FileAlreadyExistsException ex) {

                                }
                            }
                        } catch (IOException ex) {
                            throw new UncheckedIOException(ex);
                        }
                    }
                    else {
                        throw new IllegalStateException("Can't create parent dir:" + target.getParentFile());
                    }
                }
                else {
                    throw new IllegalStateException("Zip slip detected for: " + entry.getName());
                }
            });
        }
    }
}
