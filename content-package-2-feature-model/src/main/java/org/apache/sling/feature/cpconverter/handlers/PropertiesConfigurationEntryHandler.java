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

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

public final class PropertiesConfigurationEntryHandler extends AbstractConfigurationEntryHandler {

    public PropertiesConfigurationEntryHandler() {
        super("(cfg|properties)");
    }

    @Override
    protected Dictionary<String, Object> parseConfiguration(String name, InputStream input) throws Exception {
        final Properties properties = new Properties();

        try (final BufferedInputStream in = new BufferedInputStream(input)) {
            in.mark(1);

            boolean isXml = '<' == in.read();

            in.reset();

            if (isXml) {
                properties.loadFromXML(in);
            } else {
                properties.load(in);
            }
        }

        Dictionary<String, Object> configuration = new Hashtable<>();
        final Enumeration<Object> i = properties.keys();
        while (i.hasMoreElements()) {
            final Object key = i.nextElement();
            configuration.put(key.toString(), properties.get(key));
        }

        return configuration;
    }

}
