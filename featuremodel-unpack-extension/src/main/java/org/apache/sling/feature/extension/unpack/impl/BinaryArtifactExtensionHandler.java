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
package org.apache.sling.feature.extension.unpack.impl;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.extension.unpack.Unpack;
import org.apache.sling.feature.spi.context.ExtensionHandler;
import org.apache.sling.feature.spi.context.ExtensionHandlerContext;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class BinaryArtifactExtensionHandler implements ExtensionHandler {
    static final String BINARY_EXTENSIONS_PROP = "org.apache.sling.feature.binary.extensions";

//    private final Map<String, Map<String, String>> binaryExtensions;

    private final BundleContext bundleContext;

    @Activate
    public BinaryArtifactExtensionHandler(BundleContext bc) {
        bundleContext = bc;
        /*
        Map<String, Map<String, String>> be = new HashMap<>();

        // Syntax: system-fonts;dir:=abc;overwrite:=true,customer-fonts;dir:=eft
        Clause[] extClauses = Parser.parseHeader(bc.getProperty(BINARY_EXTENSIONS_PROP));
        for (Clause c : extClauses) {
            Map<String,String> cfg = new HashMap<>();

            for (Directive d : c.getDirectives()) {
                cfg.put(d.getName(), d.getValue());
            }
            be.put(c.getName(), Collections.unmodifiableMap(cfg));
        }

        binaryExtensions = Collections.unmodifiableMap(be);
        */
    }

    @Override
    public boolean handle(ExtensionHandlerContext context, Extension extension, Feature feature) throws Exception {
        Unpack unpack = Unpack.fromMapping(bundleContext.getProperty(BINARY_EXTENSIONS_PROP));

        return unpack.handle(extension, context.getArtifactProvider(),
                (u,m) -> {
                    // TODO maybe Unpack can use Map<String,Object> as context?
                    Map<String,Object> m2 = new HashMap<>(m);
                    m2.put("__unpack__", unpack);
                    m2.put("artifact.id", ArtifactId.fromMvnId(m.get("artifact.id")));

                    context.addInstallableArtifact((ArtifactId) m2.get("artifact.id"), u, m2);
                }
            );

//        return unpack.handle(extension, context.getArtifactProvider(),
//                (u,m) -> context.addInstallableArtifact(u, m));
        /*

        if (extension.getType() != ExtensionType.ARTIFACTS ||
                binaryExtensions.get(extension.getName()) == null) {
            return false;
        } else {
            Map<String,String> extensionConfig = binaryExtensions.getOrDefault(extension.getName(),
                    Collections.emptyMap());

            for (Artifact art : extension.getArtifacts()) {
                URL artifact = context.getArtifactProvider().provide(art.getId());

                Hashtable<String,Object> props = new Hashtable<>();
                props.put("artifact.id", art.getId());
                // the props.computeIfAbsent() ensures that entries aren't put in map if they have no value
                props.computeIfAbsent("dir", v -> extensionConfig.get("dir"));
                props.computeIfAbsent("overwrite", v -> extensionConfig.get("overwrite"));

                context.addInstallableArtifact(art.getId(), artifact, props);
            }
            return true;
        }
        */
    }
}

