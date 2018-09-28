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
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.io.IOUtils;
import org.apache.sling.scripting.core.ScriptNameAwareReader;
import org.osgi.framework.Bundle;

class Script extends AbstractBundledRenderUnit {

    private final URL url;
    private final ScriptEngine scriptEngine;
    private String sourceCode;
    private CompiledScript compiledScript = null;
    private Lock compilationLock = new ReentrantLock();
    private Lock readLock = new ReentrantLock();


    Script(Bundle bundle, URL url, ScriptEngine scriptEngine) {
        super(bundle);
        this.url = url;
        this.scriptEngine = scriptEngine;
    }

    private String getSourceCode() throws IOException {
        if (sourceCode == null) {
            readLock.lock();
            try {
                if (sourceCode == null) {
                    sourceCode = IOUtils.toString(url.openStream(), StandardCharsets.UTF_8);
                }
            } finally {
                readLock.unlock();
            }
        }
        return sourceCode;
    }

    @Override
    public String getName() {
        return url.getPath();
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return scriptEngine;
    }

    @Override
    public void eval(ScriptContext context) throws ScriptException {
        try {
            if (scriptEngine instanceof Compilable && compiledScript == null) {
                compilationLock.lock();
                try {
                    if (compiledScript == null) {
                        compiledScript =
                                ((Compilable) scriptEngine)
                                        .compile(new ScriptNameAwareReader(new StringReader(getSourceCode()), getName()));
                    }
                } finally {
                    compilationLock.unlock();
                }
            }
            if (compiledScript != null) {
                compiledScript.eval(context);
            } else {
                scriptEngine.eval(getSourceCode(), context);
            }
        } catch (IOException e) {
            throw new ScriptException(e);
        }
    }
}
