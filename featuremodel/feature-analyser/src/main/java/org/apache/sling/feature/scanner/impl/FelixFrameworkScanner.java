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
package org.apache.sling.feature.scanner.impl;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.felix.utils.manifest.Parser;
import org.apache.felix.utils.resource.ResourceBuilder;
import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.KeyValueMap;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.spi.FrameworkScanner;
import org.apache.sling.feature.scanner.PackageInfo;
import org.osgi.framework.Constants;
import org.osgi.resource.Capability;

public class FelixFrameworkScanner implements FrameworkScanner {


    @Override
    public BundleDescriptor scan(final ArtifactId framework,
            final File platformFile,
            final KeyValueMap frameworkProps)
    throws IOException {
        final KeyValueMap fwkProps = getFrameworkProperties(frameworkProps, platformFile);
        if ( fwkProps == null ) {
            return null;
        }
        final Set<PackageInfo> pcks = calculateSystemPackages(fwkProps);
        final List<Capability> capabilities = calculateSystemCapabilities(fwkProps);

        final BundleDescriptor d = new BundleDescriptor() {

            @Override
            public String getBundleSymbolicName() {
                return Constants.SYSTEM_BUNDLE_SYMBOLICNAME;
            }

            @Override
            public String getBundleVersion() {
                return framework.getOSGiVersion().toString();
            }

            @Override
            public int getBundleStartLevel() {
                return 0;
            }

            @Override
            public File getArtifactFile() {
                return platformFile;
            }

            @Override
            public Artifact getArtifact() {
                return new Artifact(framework);
            }

            @Override
            public Manifest getManifest() {
                return new Manifest();
            }
        };
        d.getCapabilities().addAll(capabilities);
        d.getExportedPackages().addAll(pcks);
        d.lock();
        return d;
    }

    private List<Capability> calculateSystemCapabilities(final KeyValueMap fwkProps) throws IOException
    {
         Map<String, String> mf = new HashMap<>();
         mf.put(Constants.PROVIDE_CAPABILITY,
                Stream.of(
                    fwkProps.get(Constants.FRAMEWORK_SYSTEMCAPABILITIES),
                    fwkProps.get(Constants.FRAMEWORK_SYSTEMCAPABILITIES_EXTRA)
                )
                .filter(Objects::nonNull)
                .collect(Collectors.joining(",")));
         mf.put(Constants.EXPORT_PACKAGE, Stream.of(
             fwkProps.get(Constants.FRAMEWORK_SYSTEMPACKAGES),
             fwkProps.get(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA)
             ).filter(Objects::nonNull)
                 .collect(Collectors.joining(",")));
         mf.put(Constants.BUNDLE_SYMBOLICNAME, Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
         mf.put(Constants.BUNDLE_MANIFESTVERSION, "2");
         try
         {
             return ResourceBuilder.build(null, mf).getCapabilities(null);
         }
         catch (Exception ex) {
             throw new IOException(ex);
         }
    }

    private Set<PackageInfo> calculateSystemPackages(final KeyValueMap fwkProps) {
        return
            Stream.of(
                Parser.parseHeader(
                    Stream.of(
                        fwkProps.get(Constants.FRAMEWORK_SYSTEMPACKAGES),
                        fwkProps.get(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA)
                    ).filter(Objects::nonNull)
                    .collect(Collectors.joining(","))
                )
            ).map(
                clause -> new PackageInfo(clause.getName(), clause.getAttribute("version") != null ? clause.getAttribute("version") : "0.0.0", false))
            .collect(Collectors.toSet());
    }

    private static final String DEFAULT_PROPERTIES = "default.properties";

    KeyValueMap getFrameworkProperties(final KeyValueMap appProps, final File framework)
    throws IOException {
        final Map<String, Properties> propsMap = new HashMap<>();
        try (final ZipInputStream zis = new ZipInputStream(new FileInputStream(framework)) ) {
            boolean done = false;
            while ( !done ) {
                final ZipEntry entry = zis.getNextEntry();
                if ( entry == null ) {
                    done = true;
                } else {
                    final String entryName = entry.getName();
                    if ( entryName.endsWith(".properties") ) {
                        final Properties props = new Properties();
                        props.load(zis);

                        propsMap.put(entryName, props);
                    }
                    zis.closeEntry();
                }
            }
        }

        final Properties defaultMap = propsMap.get(DEFAULT_PROPERTIES);
        if ( defaultMap == null ) {
            return null;
        }

        final KeyValueMap frameworkProps = new KeyValueMap();
        frameworkProps.putAll(appProps);

        // replace variables
        defaultMap.put("java.specification.version",
                System.getProperty("java.specification.version", "1.8"));

        StrSubstitutor ss = new StrSubstitutor(new StrLookup() {
            @Override
            public String lookup(String key) {
                // Normally if a variable cannot be found, StrSubstitutor will
                // leave the raw variable in place. We need to replace it with
                // nothing in that case.

                String val = defaultMap.getProperty(key);
                return val != null ? val : "";
            }
        });
        ss.setEnableSubstitutionInVariables(true);

        for(final Object name : defaultMap.keySet()) {
            if ( frameworkProps.get(name.toString()) == null ) {
                final String value = (String)defaultMap.get(name);
                final String substValue = ss.replace(value);
                frameworkProps.put(name.toString(), substValue);
            }
        }

        return frameworkProps;
    }
}
