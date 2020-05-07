
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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import com.cedarsoftware.util.io.JsonWriter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;

import graphql.ExecutionResult;

public class GraphQLScriptEngine extends AbstractScriptEngine {

    private final GraphQLScriptEngineFactory factory;
    public static final int JSON_INDENT_SPACES = 2;

    public GraphQLScriptEngine(GraphQLScriptEngineFactory factory) {
        this.factory = factory;
    }

    @Override
    public Object eval(String script, ScriptContext context) throws ScriptException {
        return eval(new StringReader(script), context);
    }

    @Override
    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        try {
            final GraphQLResourceQuery q = new GraphQLResourceQuery();

            final Resource resource = (Resource) context.getBindings(ScriptContext.ENGINE_SCOPE)
                    .get(SlingBindings.RESOURCE);
            final ExecutionResult result = q.executeQuery(factory.getSchemaProvider(), factory.getFetcherManager(),
                    resource, IOUtils.toString(reader));
            final PrintWriter out = (PrintWriter) context.getBindings(ScriptContext.ENGINE_SCOPE).get(SlingBindings.OUT);
            sendJSON(out, result);
        } catch(Exception e) {
            throw new ScriptException(e);
        }
        return null;
    }

    public static void sendJSON(PrintWriter out, ExecutionResult result) throws ScriptException {
        final Object data = result.toSpecification();
        if (data == null) {
            throw new ScriptException("No data");
        }
        new JsonWriter(new WriterOutputStream(out)).write(data);
    }

    @Override
    public Bindings createBindings() {
        return null;
    }

    @Override
    public ScriptEngineFactory getFactory() {
        return factory;
    }
}
