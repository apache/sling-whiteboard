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

import org.apache.commons.io.IOUtils;
import org.apache.sling.scripting.core.ScriptNameAwareReader;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Script {

    private URL url;
    private ScriptEngine scriptEngine;
    private String sourceCode;
    private CompiledScript compiledScript = null;
    private Lock lock = new ReentrantLock();

    Script(URL url, ScriptEngine scriptEngine) {
        this.url = url;
        this.scriptEngine = scriptEngine;
    }

    private synchronized String getSourceCode() throws IOException {
        if (sourceCode == null) {
            sourceCode = IOUtils.toString(url.openStream(), StandardCharsets.UTF_8);
        }
        return sourceCode;
    }

    String getName() {
        return url.getPath();
    }

    ScriptEngine getScriptEngine() {
        return scriptEngine;
    }

    void eval(ScriptContext context) throws ScriptException, IOException {
        if (scriptEngine instanceof Compilable && compiledScript == null) {
            lock.lock();
            try {
                if (scriptEngine instanceof Compilable && compiledScript == null) {
                    compiledScript =
                            ((Compilable) scriptEngine).compile(new ScriptNameAwareReader(new StringReader(getSourceCode()), getName()));
                }
            } finally {
                lock.unlock();
            }
        }
        if (compiledScript != null) {
            compiledScript.eval(context);
        } else {
            scriptEngine.eval(getSourceCode(), context);
        }

    }
}
