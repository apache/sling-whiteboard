/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.feature.support.util;

import org.apache.sling.commons.osgi.ManifestHeader;
import org.apache.sling.feature.Capability;
import org.apache.sling.feature.Requirement;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import static org.apache.sling.feature.support.util.ManifestParser.convertProvideCapabilities;
import static org.apache.sling.feature.support.util.ManifestParser.normalizeCapabilityClauses;
import static org.apache.sling.feature.support.util.ManifestParser.parseStandardHeader;

public class ManifestUtil {

    /**
     * Get the manifest from the artifact.
     * @param artifact The file
     * @throws IOException If the manifest can't be read
     */
    public static Manifest getManifest(final File artifact) throws IOException {
        try (final JarFile file = new JarFile(artifact) ) {
            return file.getManifest();
        }
    }

    public static List<PackageInfo> extractPackages(final Manifest m,
            final String headerName,
            final String defaultVersion,
            final boolean checkOptional) {
        final String pckInfo = m.getMainAttributes().getValue(headerName);
        if (pckInfo != null) {
            final ManifestHeader header = ManifestHeader.parse(pckInfo);

            final List<PackageInfo> pcks = new ArrayList<>();
            for(final ManifestHeader.Entry entry : header.getEntries()) {
                String version = entry.getAttributeValue("version");
                if ( version == null ) {
                    version = defaultVersion;
                }
                boolean optional = false;
                if ( checkOptional ) {
                    final String resolution = entry.getDirectiveValue("resolution");
                    optional = "optional".equalsIgnoreCase(resolution);
                }
                final PackageInfo pck = new PackageInfo(entry.getValue(),
                        version,
                        optional);
                pcks.add(pck);
            }

            return pcks;
        }
        return Collections.emptyList();
    }

    public static List<PackageInfo> extractExportedPackages(final Manifest m) {
        return extractPackages(m, Constants.EXPORT_PACKAGE, "0.0.0", false);
    }

    public static List<PackageInfo> extractImportedPackages(final Manifest m) {
        return extractPackages(m, Constants.IMPORT_PACKAGE, null, true);
    }

    public static List<PackageInfo> extractDynamicImportedPackages(final Manifest m) {
        return extractPackages(m, Constants.DYNAMICIMPORT_PACKAGE, null, false);
    }

    public static List<Capability> extractCapabilities(ManifestParser parser) {
        return parser.getCapabilities();
    }

    public static List<Requirement> extractRequirements(ManifestParser parser)  {
        return parser.getRequirements();
    }

    public static void unmarshalAttribute(String key, Object value, BiConsumer<String, Object> sink) throws IOException {
        unmarshal(key + "=" + value, Capability::getAttributes, sink);
    }

    public static void unmarshalDirective(String key, Object value, BiConsumer<String, Object> sink) throws IOException {
        unmarshal(key + ":=" + value, Capability::getDirectives, sink);
    }

    private static void unmarshal(String header, Function<Capability, Map<String, Object>> lookup, BiConsumer<String, Object> sink) throws IOException {
        try {
            convertProvideCapabilities(
                    normalizeCapabilityClauses(parseStandardHeader("foo;" + header), "2"))
                    .forEach(capability -> lookup.apply(capability).forEach(sink));
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public static void marshalAttribute(String key, Object value, BiConsumer<String, String> sink) {
        marshal(key, value, sink);
    }

    public static void marshalDirective(String key, Object value, BiConsumer<String, String> sink) {
        marshal(key, value, sink);
    }

    private static void marshal(String key, Object value, BiConsumer<String, String> sink) {
        StringBuilder keyBuilder = new StringBuilder(key);
        if (value instanceof  List) {
            List list = (List) value;
            keyBuilder.append(":List");
            if (!list.isEmpty()) {
                String type = type(list.get(0));
                if (!type.equals("String")) {
                    keyBuilder.append('<').append(type).append('>');
                }
                value = list.stream().map(
                        v -> v.toString().replace(",", "\\,")
                ).collect(Collectors.joining(","));
            }
            else {
                value = "";
            }
        }
        else {
            String type = type(value);
            if (!type.equals("String")) {
                keyBuilder.append(':').append(type);
            }
        }
        sink.accept(keyBuilder.toString(), value.toString());
    }

    private static String type(Object value) {
        if (value instanceof Long) {
            return "Long";
        }
        else if (value instanceof Double)
        {
            return "Double";
        }
        else if (value instanceof Version)
        {
            return "Version";
        }
        else
        {
            return "String";
        }
    }
}
