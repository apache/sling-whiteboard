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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.scripting.ScriptEvaluationException;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.scripting.core.ScriptHelper;

class BundledScriptServlet extends GenericServlet {


    private final BundledScriptFinder m_bundledScriptFinder;
    private final ScriptContextProvider m_scriptContextProvider;
    private final LinkedHashSet<TypeProvider> m_wiredTypeProviders;
    private final boolean m_precompiledScripts;

    private Map<String, Executable> scriptsMap = new HashMap<>();
    private ReadWriteLock lock = new ReentrantReadWriteLock();



    BundledScriptServlet(BundledScriptFinder bundledScriptFinder, ScriptContextProvider scriptContextProvider,
                         LinkedHashSet<TypeProvider> wiredTypeProviders, boolean precompiledScripts) {
        m_bundledScriptFinder = bundledScriptFinder;
        m_scriptContextProvider = scriptContextProvider;
        m_wiredTypeProviders = wiredTypeProviders;
        m_precompiledScripts = precompiledScripts;
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        if ((req instanceof SlingHttpServletRequest) && (res instanceof SlingHttpServletResponse)) {
            SlingHttpServletRequest request = (SlingHttpServletRequest) req;
            SlingHttpServletResponse response = (SlingHttpServletResponse) res;

            if (request.getAttribute(SlingConstants.ATTR_INCLUDE_SERVLET_PATH) == null) {
                final String contentType = request.getResponseContentType();
                if (contentType != null) {
                    response.setContentType(contentType);
                    if (contentType.startsWith("text/")) {
                        response.setCharacterEncoding("UTF-8");
                    }
                }
            }

            String scriptsMapKey = getScriptsMapKey(request);
            Executable executable;
            lock.readLock().lock();
            try {
                executable = scriptsMap.get(scriptsMapKey);
                if (executable == null) {
                    lock.readLock().unlock();
                    lock.writeLock().lock();
                    try {
                        executable = scriptsMap.get(scriptsMapKey);
                        if (executable == null) {
                            executable = m_bundledScriptFinder.getScript(request, m_wiredTypeProviders, m_precompiledScripts);
                            if (executable != null) {
                                scriptsMap.put(scriptsMapKey, executable);
                            }
                        }
                        lock.readLock().lock();
                    } finally {
                        lock.writeLock().unlock();
                    }
                }
            } finally {
                lock.readLock().unlock();
            }
            if (executable != null) {
                Set<String> wiredResourceTypes = new HashSet<>();
                for (TypeProvider typeProvider : m_wiredTypeProviders) {
                    wiredResourceTypes.add(typeProvider.getType());
                }
                RequestWrapper requestWrapper = new RequestWrapper(request, wiredResourceTypes);
                ScriptContext scriptContext = m_scriptContextProvider.prepareScriptContext(requestWrapper, response, executable);
                try {
                    executable.eval(scriptContext);
                } catch (ScriptException se) {
                    Throwable cause = (se.getCause() == null) ? se : se.getCause();
                    throw new ScriptEvaluationException(executable.getName(), se.getMessage(), cause);
                } finally {
                    Bindings engineBindings = scriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
                    if (engineBindings != null && engineBindings.containsKey(SlingBindings.SLING)) {
                        Object scriptHelper = engineBindings.get(SlingBindings.SLING);
                        if (scriptHelper instanceof ScriptHelper) {
                            ((ScriptHelper) scriptHelper).cleanup();
                        }
                    }
                    executable.releaseDependencies();
                }
            } else {
                throw new ServletException("Unable to locate a " + (m_precompiledScripts ? "class" : "script") + " for rendering.");
            }
        } else {
            throw new ServletException("Not a Sling HTTP request/response");
        }
    }

    private String getScriptsMapKey(SlingHttpServletRequest request) {
        RequestPathInfo requestPathInfo = request.getRequestPathInfo();
        String selectorString = requestPathInfo.getSelectorString();
        String requestExtension = requestPathInfo.getExtension();
        return request.getMethod() + ":" + request.getResource().getResourceType() +
                (StringUtils.isNotEmpty(selectorString) ? ":" + selectorString : "") +
                (StringUtils.isNotEmpty(requestExtension) ? ":" + requestExtension : "");
    }
}
