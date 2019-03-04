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

import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.fs.io.Archive.Entry;
import org.apache.sling.cp2fm.ContentPackage2FeatureModelConverter;
import org.apache.sling.feature.Configuration;

abstract class AbstractConfigurationEntryHandler extends AbstractRegexEntryHandler {

    public AbstractConfigurationEntryHandler(String regex) {
        super(regex);
    }

    @Override
    public final void handle(Archive archive, Entry entry, ContentPackage2FeatureModelConverter converter) throws Exception {
        String name = entry.getName().substring(0, entry.getName().lastIndexOf('.'));

        logger.info("Processing configuration '{}'.", name);

        Dictionary<Object, Object> parsedConfiguration;

        try (InputStream input = archive.openInputStream(entry)) {
            parsedConfiguration = parseConfiguration(input);
        }

        if (!parsedConfiguration.isEmpty()) {
            Configuration configuration = null;

            dance : for (Configuration addedConfiguration : converter.getTargetFeature().getConfigurations()) {
                if (name.equals(addedConfiguration.getPid())) {
                    configuration = addedConfiguration;
                    break dance;
                }
            }

            if (configuration == null) {
                configuration = new Configuration(name);
                converter.getTargetFeature().getConfigurations().add(configuration);
            }

            Enumeration<Object> keys = parsedConfiguration.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement().toString();
                Object value = parsedConfiguration.get(key);
                configuration.getProperties().put(key, value);
            }
        } else {
            logger.warn("{} configuration is empty, omitting it from the Feature File", name);
        }
    }

    protected abstract Dictionary<Object, Object> parseConfiguration(InputStream input) throws Exception;

}
