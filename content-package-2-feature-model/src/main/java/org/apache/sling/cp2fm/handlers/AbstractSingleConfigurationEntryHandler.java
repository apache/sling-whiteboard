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

import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Configurations;

abstract class AbstractSingleConfigurationEntryHandler extends AbstractConfigurationEntryHandler {

    public AbstractSingleConfigurationEntryHandler(String regex) {
        super(regex);
    }

    @Override
    protected final Configurations parseConfigurations(String name, InputStream input) throws Exception {
        Configurations configurations = new Configurations();

        Dictionary<String, Object> configurationProperties = parseConfiguration(input);

        if (!configurationProperties.isEmpty()) {
            Configuration configuration = new Configuration(name);
            Enumeration<String> keys = configurationProperties.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                Object value = configurationProperties.get(key);
                configuration.getProperties().put(key, value);
            }

            configurations.add(configuration);
        }

        return configurations;
    }

    protected abstract Dictionary<String, Object> parseConfiguration(InputStream input) throws Exception;

}
