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
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Directive;
import org.apache.felix.utils.manifest.Parser;
import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.builder.ArtifactProvider;

public class Unpack {
    public static final String UNPACK_EXTENSIONS_PROP = "org.apache.sling.feature.unpack.extensions";

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

    public boolean handle(Extension extension, ArtifactProvider provider, BiConsumer<URL, Map<String, Object>> handler) {
        if (extension.getType() == ExtensionType.ARTIFACTS &&
            this.registry.containsKey(extension.getName())) {
            for (Artifact artifact : extension.getArtifacts()) {
                String dir = this.registry.get(extension.getName()).get("dir");
                boolean override = Boolean.parseBoolean(this.registry.get(extension.getName()).get("override"));
                URL url = provider.provide(artifact.getId());
                String key = this.registry.get(extension.getName()).get("key");
                String value = this.registry.get(extension.getName()).get("value");
                String index = this.registry.get(extension.getName()).get("index");
                Map<String, Object> context = new HashMap<>();
                context.put("artifact.id", artifact.getId());
                context.put("dir", dir);
                context.put("override", Boolean.toString(override));
                context.put("key", key);
                context.put("value", value);
                context.put("index", index);
                handler.accept(url, context);
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean handles(InputStream stream, Map<String, Object> context) {
        String contextDir = (String) context.get("dir");
        String dir;
        String key;
        String value;
        if (contextDir == null && this.defaultMapping != null) {
            dir = this.registry.get(defaultMapping).get("dir");
            key = this.registry.get(defaultMapping).get("key");
            value = this.registry.get(defaultMapping).get("value");
        } else {
            dir = contextDir;
            key = (String) context.get("key");
            value = (String) context.get("value");
        }
        if (dir == null) {
            return false;
        } else if (key != null && value != null) {
            try (JarInputStream jarInputStream = new JarInputStream(stream)) {
                Manifest mf = jarInputStream.getManifest();
                if (mf != null) {
                    return value.equalsIgnoreCase(mf.getMainAttributes().getValue(key));
                }
            } catch (Exception ex) {
                return false;
            }
        } else if (contextDir != null) {
            return true;
        }

        return false;
    }


    private void unpack(URL url, Map<String, Object> context) {
        try {
            unpack(url.openStream(), context);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void unpack(InputStream stream, Map<String, Object> context) {
        try {
            String dir = (String) context.get("dir");
            boolean override;
            String index;
            if (dir == null && this.defaultMapping != null) {
                dir = this.registry.get(defaultMapping).get("dir");
                override = Boolean.parseBoolean(this.registry.get(defaultMapping).get("override"));
                index = this.registry.get(defaultMapping).get("index");
            } else {
                override = Boolean.parseBoolean((String) context.get("override"));
                index = (String) context.get("index");
            }

            if (dir == null) {
                throw new IllegalStateException("No target dir and no default configured");
            }
            unpack(dir, stream, override, index);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static Unpack fromMapping(String mapping) {
        Map<String, Map<String, String>> registry = new HashMap<>();

        // Syntax: system-fonts;dir:=abc;overwrite:=true,customer-fonts;dir:=eft;default:=true;key:=foobar;value:=baz
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

    private void unpack(String dir, InputStream stream, boolean override, String index) throws IOException {
        File base = new File(dir);
        if (!base.isDirectory() && !base.mkdirs()) {
            throw new IOException("Unable to find or created base dir: " + base);
        }

        try (JarInputStream jarInputStream = new JarInputStream(stream)) {
            String indexValue = null;
            if (index != null) {
                Manifest mf = jarInputStream.getManifest();
                if (mf != null) {
                    indexValue = mf.getMainAttributes().getValue(index);
                }
            }

            List<String> roots = parseRoots(indexValue);
            for (ZipEntry entry = jarInputStream.getNextEntry(); entry != null; entry = jarInputStream.getNextEntry()) {
                if (!entry.isDirectory() && !entry.getName().toLowerCase().startsWith("meta-inf/") && isRoot(roots, entry.getName())) {
                    File target = new File(base, relativize(roots, entry.getName()));
                    if (target.getParentFile().toPath().startsWith(base.toPath())) {
                        if (target.getParentFile().isDirectory() || target.getParentFile().mkdirs()) {
                            if (override) {
                                Files.copy(jarInputStream, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            } else if (!target.exists()) {
                                try {
                                    Files.copy(jarInputStream, target.toPath());
                                } catch (FileAlreadyExistsException ex) {

                                }
                            }
                        } else {
                            throw new IOException("Can't create parent dir:" + target.getParentFile());
                        }
                    } else {
                        throw new IOException("Zip slip detected for: " + entry.getName());
                    }
                }
            }
        }
    }

    private boolean isRoot(List<String> roots, String path) {
        return roots.stream().anyMatch(root -> ("/" + path).startsWith(root));
    }

    private List<String> parseRoots(String index) {
        List<String> roots = new ArrayList<>();

        if (index != null) {
            roots.addAll(Stream.of(Parser.parseDelimitedString(index, ",")).map(root -> root.endsWith("/") ? root : root + "/").map(root -> root.startsWith("/") ? root : "/" + root).collect(Collectors.toList()));
        } else {
            roots.add("/");
        }
        return roots;
    }

    private String relativize(List<String> roots, String path) {
        for (String root : roots) {
            if (("/" + path).startsWith(root)) {
                return path.substring(root.length() -1);
            }
        }
        throw new IllegalStateException("Can't find a root for: " + path);
    }
}
