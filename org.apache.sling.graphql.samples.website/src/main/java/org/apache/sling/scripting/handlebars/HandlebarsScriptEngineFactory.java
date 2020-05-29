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

package org.apache.sling.scripting.handlebars;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.apache.sling.engine.SlingRequestProcessor;
import org.apache.sling.scripting.api.AbstractScriptEngineFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

@Component(
    service = ScriptEngineFactory.class,
    immediate = true,
    property = {
        Constants.SERVICE_DESCRIPTION + "=Apache Sling Handlebars ScriptEngineFactory",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
    }
)

@Designate(
    ocd = HandlebarsScriptEngineFactoryConfig.class
)

public class HandlebarsScriptEngineFactory extends AbstractScriptEngineFactory {

    public static final String LANGUAGE_NAME = "Handlebars";
    public static final String LANGUAGE_VERSION = "Sling:Handlebars:0.1";

    @Reference
    private SlingRequestProcessor requestProcessor;

    @Activate
    private void activate(final HandlebarsScriptEngineFactoryConfig config, final BundleContext ctx) {
        setExtensions(config.extensions());
        setMimeTypes(config.mimeTypes());
        setNames(config.names());
    }

    @Override
    public String getLanguageName() {
        return LANGUAGE_NAME;
    }

    @Override
    public String getLanguageVersion() {
        return LANGUAGE_VERSION;
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return new HandlebarsScriptEngine(this);
    }

    SlingRequestProcessor getSlingRequestProcessor() {
        return requestProcessor;
    }
}