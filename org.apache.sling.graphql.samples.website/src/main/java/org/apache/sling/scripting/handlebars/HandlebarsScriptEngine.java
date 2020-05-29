
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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Map;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import com.cedarsoftware.util.io.JsonReader;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.StringHelpers;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.apache.sling.servlethelpers.MockSlingHttpServletResponse;

/** Minimal script engine for server-side rendering
 *  using Handlebars templates. Might need some changes if
 *  we want to make it general-purpose, for now it's a quick
 *  implementation just for this demo.
 * 
 *  The JSON representation of the current Resource is retrieved
 *  using an internal request with the Resource path + ".json", and
 *  the result converted to a Handlebars-friendly Map of Maps.
 * 
 *  See https://github.com/jknack/handlebars.java for Handlebars docs
 */
public class HandlebarsScriptEngine extends AbstractScriptEngine {

    private final HandlebarsScriptEngineFactory factory;

    public HandlebarsScriptEngine(HandlebarsScriptEngineFactory factory) {
        this.factory = factory;
    }

    @Override
    public Object eval(String script, ScriptContext context) throws ScriptException {
        final Resource resource = (Resource) context.getBindings(ScriptContext.ENGINE_SCOPE).get(SlingBindings.RESOURCE);
        final PrintWriter out = (PrintWriter) context.getBindings(ScriptContext.ENGINE_SCOPE).get(SlingBindings.OUT);

        try {
            final Handlebars handlebars = setupHandlebars();
            final Template template = handlebars.compileInline(script);
            out.println(template.apply(getData(resource)));
        } catch(IOException ioe) {
            final ScriptException up = new ScriptException("IOException in eval");
            up.initCause(ioe);
            throw up;
        }
        return null;
    }

    private Handlebars setupHandlebars() {
        final Handlebars result = new Handlebars();
        result.registerHelpers(StringHelpers.class);
        return result;
    }

    /** We might do this with a BindingsValuesProvider? */
    private Map<?, ?> getData(Resource r) throws ScriptException {
        // Request resource.json and convert the result to Maps
        final String jsonString = internalRequest(r.getResourceResolver(), r.getPath() + ".json");
        return JsonReader.jsonToMaps(jsonString);
    }

    private String internalRequest(ResourceResolver resourceResolver, String path) throws ScriptException {
        final MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(resourceResolver);
        request.setPathInfo(path);
        final MockSlingHttpServletResponse response = new MockSlingHttpServletResponse();

        try {
            factory.getSlingRequestProcessor().processRequest(request, response, resourceResolver);
            final int status = response.getStatus();
            if(status != 200) {
                throw new ScriptException ("Request to " + path + " returns HTTP status " + status);
            }
                return response.getOutputAsString();
        } catch(Exception e) {
            final ScriptException up = new ScriptException("Internal request failed");
            up.initCause(e);
            throw up;
        }
    }

    @Override
    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        try {
            return eval(IOUtils.toString(reader), context);
        } catch(IOException ioe) {
            final ScriptException up = new ScriptException("IOException in eval");
            up.initCause(ioe);
            throw up;
        }
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
