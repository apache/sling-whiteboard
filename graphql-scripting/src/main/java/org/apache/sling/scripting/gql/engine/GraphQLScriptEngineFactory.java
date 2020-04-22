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

package org.apache.sling.scripting.gql.engine;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.apache.sling.scripting.api.AbstractScriptEngineFactory;
import org.apache.sling.scripting.gql.schema.DataFetcherSelector;
import org.apache.sling.scripting.gql.schema.GraphQLSchemaProvider;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

@Component(
    service = ScriptEngineFactory.class,
    immediate = true,
    property = {
        Constants.SERVICE_DESCRIPTION + "=Apache Sling Scripting GraphQL ScriptEngineFactory",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
    }
)

@Designate(
    ocd = GraphQLScriptEngineFactoryConfiguration.class
)

public class GraphQLScriptEngineFactory extends AbstractScriptEngineFactory {

    public static final String LANGUAGE_NAME = "GraphQL";
    public static final String LANGUAGE_VERSION = "Sling:GraphQL:0.1";

    @Reference
    private GraphQLSchemaProvider schemaProvider;

    @Reference
    private DataFetcherSelector fetcherManager;

    @Activate
    private void activate(final GraphQLScriptEngineFactoryConfiguration config) {
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
        return new GraphQLScriptEngine(this);
    }

    GraphQLSchemaProvider getSchemaProvider() {
        return schemaProvider;
    }

    DataFetcherSelector getFetcherManager() {
        return fetcherManager;
    }

}
