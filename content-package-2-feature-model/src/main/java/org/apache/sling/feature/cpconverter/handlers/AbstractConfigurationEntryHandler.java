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
package org.apache.sling.feature.cpconverter.handlers;

import java.io.InputStream;
import java.util.Dictionary;
import java.util.regex.Matcher;

import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.fs.io.Archive.Entry;
import org.apache.sling.feature.cpconverter.ContentPackage2FeatureModelConverter;

abstract class AbstractConfigurationEntryHandler extends AbstractRegexEntryHandler {

    public AbstractConfigurationEntryHandler(String extension) {
        super("(jcr_root)?/apps/[^/]+/config(\\.([^/]+))?/.+\\." + extension);
    }

    @Override
    public final void handle(String path, Archive archive, Entry entry, ContentPackage2FeatureModelConverter converter) throws Exception {
        String pid = entry.getName().substring(0, entry.getName().lastIndexOf('.'));

        logger.info("Processing configuration '{}'.", pid);

        Dictionary<String, Object> configurationProperties;
        try (InputStream input = archive.openInputStream(entry)) {
            configurationProperties = parseConfiguration(pid, input);
        }

        if (configurationProperties.isEmpty()) {
            logger.info("No configuration properties found for configuration {}", path);
            return;
        }

        Matcher matcher = getPattern().matcher(path);
        String runMode = null;
        // we are pretty sure it matches, here
        if (matcher.matches()) {
            // there is a specified RunMode
            runMode = matcher.group(3);
        } else {
            throw new IllegalStateException("Something went terribly wrong: pattern '"
                                            + getPattern().pattern()
                                            + "' should have matched already with path '"
                                            + path
                                            + "' but it does not, currently");
        }

        converter.addConfiguration(runMode, pid, configurationProperties);
    }

    protected abstract Dictionary<String, Object> parseConfiguration(String name, InputStream input) throws Exception;

}
