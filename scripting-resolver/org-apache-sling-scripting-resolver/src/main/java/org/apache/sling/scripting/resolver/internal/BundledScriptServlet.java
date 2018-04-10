/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Licensed to the Apache Software Foundation (ASF) under one
 ~ or more contributor license agreements.  See the NOTICE file
 ~ distributed with this work for additional information
 ~ regarding copyright ownership.  The ASF licenses this file
 ~ to you under the Apache License, Version 2.0 (the
 ~ "License"); you may not use this file except in compliance
 ~ with the License.  You may obtain a copy of the License at
 ~
 ~   http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package org.apache.sling.scripting.resolver.internal;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.scripting.ScriptEvaluationException;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.scripting.resolver.BundledScriptFinder;
import org.apache.sling.scripting.resolver.Script;
import org.osgi.framework.Bundle;

class BundledScriptServlet extends SlingAllMethodsServlet {

    private final Bundle m_bundle;
    private final BundledScriptFinder m_bundledScriptFinder;
    private final ScriptContextProvider m_scriptContextProvider;

    private Map<URI, CompiledScript> compiledScriptsMap = new ConcurrentHashMap<>();

    BundledScriptServlet(BundledScriptFinder bundledScriptFinder, Bundle bundle, ScriptContextProvider scriptContextProvider) {
        m_bundle = bundle;
        m_bundledScriptFinder = bundledScriptFinder;
        m_scriptContextProvider = scriptContextProvider;
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        Script script = m_bundledScriptFinder.getScript(request, m_bundle);
        if (script != null) {
            if (request.getAttribute(SlingConstants.ATTR_INCLUDE_SERVLET_PATH) == null) {
                final String contentType = request.getResponseContentType();
                if (contentType != null) {
                    response.setContentType(contentType);

                    // only set the character encoding for text/ content types
                    // see SLING-679
                    if (contentType.startsWith("text/")) {
                        response.setCharacterEncoding("UTF-8");
                    }
                }
            }
            ScriptContext scriptContext = m_scriptContextProvider.prepareScriptContext(request, response, script);
            try {
                script.getScriptEngine().eval(script.getSourceCode(), scriptContext);
            } catch (ScriptException e) {
                Throwable cause = (e.getCause() == null) ? e : e.getCause();
                throw new ScriptEvaluationException(script.getName(), e.getMessage(), cause);
            }
        } else {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
