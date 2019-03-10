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
package org.apache.sling.cp2fm.handlers;

import java.io.InputStream;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.regex.Matcher;

import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.fs.io.Archive.Entry;
import org.apache.sling.cp2fm.ContentPackage2FeatureModelConverter;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Feature;

abstract class AbstractConfigurationEntryHandler extends AbstractRegexEntryHandler {

    public AbstractConfigurationEntryHandler(String extension) {
        super("(jcr_root)?/apps/[^/]+/config(\\.([^/]+))?/.+\\." + extension);
    }

    @Override
    public final void handle(String path, Archive archive, Entry entry, ContentPackage2FeatureModelConverter converter) throws Exception {
        String name = entry.getName().substring(0, entry.getName().lastIndexOf('.'));

        logger.info("Processing configuration '{}'.", name);

        Dictionary<String, Object> configurationProperties;
        try (InputStream input = archive.openInputStream(entry)) {
            configurationProperties = parseConfiguration(name, input);
        }

        if (configurationProperties.isEmpty()) {
            logger.info("No configuration properties found for configuration {}", path);
            return;
        }

        Feature feature;

        Matcher matcher = getPattern().matcher(path);
        String runMode = null;
        // we are pretty sure it matches, here
        if (matcher.matches() && (runMode = matcher.group(3)) != null) {
            // there is a specified RunMode
            feature = converter.getRunMode(runMode);
        } else {
            feature = converter.getTargetFeature();
        }

        Configuration configuration = feature.getConfigurations().getConfiguration(name);

        if (configuration == null) {
            configuration = new Configuration(name);
            feature.getConfigurations().add(configuration);
        }

        Enumeration<String> keys = configurationProperties.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            Object value = configurationProperties.get(key);
            configuration.getProperties().put(key, value);
        }
    }

    protected abstract Dictionary<String, Object> parseConfiguration(String name, InputStream input) throws Exception;

}
