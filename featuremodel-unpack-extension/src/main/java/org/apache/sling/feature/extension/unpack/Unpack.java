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


import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Directive;
import org.apache.felix.utils.manifest.Parser;
import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.builder.ArtifactProvider;

public class Unpack
{
    private final Map<String, Map<String, String>> registry;

    private Unpack(Map<String, Map<String, String>> registry) {
        this.registry = registry;
    }

    public boolean handle(Extension extension, ArtifactProvider provider) throws Exception
    {
        if (extension.getType() == ExtensionType.ARTIFACTS &&
            this.registry.containsKey(extension.getName())) {
            for (Artifact artifact : extension.getArtifacts()) {
                unpack(this.registry.get(extension.getName()).get("dir"),
                    provider.provide(artifact.getId()),
                    Boolean.parseBoolean(this.registry.get(extension.getName()).get("override")));
            }
            return true;
        } else {
            return false;
        }
    }

    public static Unpack fromMapping(String mapping)
    {
        Map<String, Map<String, String>> registry = new HashMap<>();

        // Syntax: system-fonts;dir:=abc;overwrite:=true,customer-fonts;dir:=eft
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

    private void unpack(String dir, URL url, boolean override) throws Exception {

    }
}
