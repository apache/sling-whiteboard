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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.util.Dictionary;

import org.apache.commons.io.IOUtils;
import org.apache.felix.configurator.impl.json.JSONUtil;
import org.apache.felix.configurator.impl.json.TypeConverter;
import org.apache.felix.configurator.impl.model.ConfigurationFile;

public final class JsonConfigurationEntryHandler extends AbstractConfigurationEntryHandler {

    public JsonConfigurationEntryHandler() {
        super("[^/]+\\.cfg\\.json");
    }

    @Override
    protected Dictionary<String, Object> parseConfiguration(String name, InputStream input) throws Exception {
        StringBuilder content = new StringBuilder()
                                .append("{ \"")
                                .append(name)
                                .append("\" : ");
        try (Reader reader = new InputStreamReader(input); StringWriter writer = new StringWriter()) {
            IOUtils.copy(reader, writer);
            content.append(writer.toString());
        }
        content.append("}");

        JSONUtil.Report report = new JSONUtil.Report();
        ConfigurationFile configuration = JSONUtil.readJSON(new TypeConverter(null),
                                                            name,
                                                            new URL("file://content-package/" + name),
                                                            0,
                                                            content.toString(),
                                                            report);

        if (!report.errors.isEmpty() || !report.warnings.isEmpty()) {
            final StringBuilder builder = new StringBuilder();
            builder.append("Errors in configuration:");
            for (final String w : report.warnings) {
                builder.append("\n");
                builder.append(w);
            }
            for (final String e : report.errors) {
                builder.append("\n");
                builder.append(e);
            }
            throw new IOException(builder.toString());
        }

        return configuration.getConfigurations().get(0).getProperties();
    }

}
