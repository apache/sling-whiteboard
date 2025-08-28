/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.microsling.scripting.engines.freemarker;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.sling.microsling.api.exceptions.HttpStatusCodeException;
import org.apache.sling.microsling.api.exceptions.SlingException;
import org.apache.sling.microsling.scripting.ScriptEngine;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;

/**
 * A ScriptEngine that uses {@link http://freemarker.org/ FreeMarker}
 * templates to render a Resource in HTML.
 */
public class FreemarkerScriptEngine implements ScriptEngine {

    public final static String FREEMARKER_SCRIPT_EXTENSION = "ftl";
    private final Configuration configuration;

    public FreemarkerScriptEngine() throws SlingException {
        configuration = new Configuration();
    }

    public String[] getExtensions() {
        return new String[] { FREEMARKER_SCRIPT_EXTENSION };
    }

    public void eval(Reader script, Map<String, Object> props)
            throws SlingException, IOException {
        String scriptName = (String)props.get(FILENAME);                                                                    
        // ensure get method
        HttpServletRequest request = (HttpServletRequest) props.get(REQUEST);
        if (!"GET".equals(request.getMethod())) {
            throw new HttpStatusCodeException(
                HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "FreeMarker templates only support GET requests");
        }

        Template tmpl = new Template(scriptName, script, configuration);

        try {
            Writer w = ((HttpServletResponse) props.get(RESPONSE)).getWriter();
            tmpl.process(props, w);            
        } catch (IOException ioe) {
            throw ioe;
        } catch (Throwable t) {
            throw new SlingException("Failure running FreeMarker script " + scriptName, t);
        }
    }
}