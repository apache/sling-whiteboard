/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.feature.support;

import org.apache.sling.feature.Configurations;
import org.apache.sling.feature.support.json.ConfigurationJSONWriter;
import org.osgi.framework.Constants;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;


public class ConfigurationUtil {

    public static final String REQUIRE_CONFIGURATOR_CAPABILITY =
            "osgi.extender;filter:=\"(&(osgi.extender=osgi.configurator)(version>=1.0)(!(version>=2.0)))\"";

    public static final String REQUIRE_REPOINIT_CAPABILITY =
            "osgi.implementation;filter:=\"(&(osgi.implementation=org.apache.sling.jcr.repoinit)(version>=1.0)(!(version>=2.0)))\"";

    /**
     * Create a bundle containing the configurations to be processed the
     * OSGi configurator
     *
     * @param os The output stream. The stream is not closed
     * @param configurations The list of configurations
     * @param symbolicName The symbolic name for the generated bundle
     * @param version The version for the generated bundle
     * @param additionalAttributes Optional additional attributes for the Manifest.
     * @throws IOException If something goes wrong
     */
    public static void createConfiguratorBundle(final OutputStream os,
            final Configurations configurations,
            final String symbolicName,
            final String version,
            final Map<String, String> additionalAttributes)
    throws IOException {

        final Manifest mf = new Manifest();
        mf.getMainAttributes().putValue("Manifest-Version", "1.0");
        mf.getMainAttributes().putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
        mf.getMainAttributes().putValue(Constants.BUNDLE_SYMBOLICNAME, symbolicName);
        mf.getMainAttributes().putValue(Constants.BUNDLE_VERSION, version);
        mf.getMainAttributes().putValue(Constants.BUNDLE_VENDOR, "The Apache Software Foundation");
        mf.getMainAttributes().putValue(Constants.REQUIRE_CAPABILITY, REQUIRE_CONFIGURATOR_CAPABILITY);

        if ( additionalAttributes != null ) {
            for(final Map.Entry<String, String> entry : additionalAttributes.entrySet()) {
                if ( Constants.REQUIRE_CAPABILITY.equals(entry.getKey())
                    && !entry.getValue().contains("osgi.extender=osgi.configurator")) {
                    mf.getMainAttributes().putValue(entry.getKey(), entry.getValue() + "," + REQUIRE_CONFIGURATOR_CAPABILITY);
                } else {
                    mf.getMainAttributes().putValue(entry.getKey(), entry.getValue());
                }
            }
        }

        final JarOutputStream jos = new JarOutputStream(os, mf);

        final ZipEntry ze = new ZipEntry("OSGI-INF/configurator/configurations.json");
        jos.putNextEntry(ze);
        final Writer w = new OutputStreamWriter(jos, "UTF-8");
        ConfigurationJSONWriter.write(w, configurations);
        w.flush();
        jos.closeEntry();

        jos.flush();
        jos.finish();
    }
}
