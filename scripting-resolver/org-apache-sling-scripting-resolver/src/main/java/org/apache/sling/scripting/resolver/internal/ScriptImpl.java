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

import java.io.Reader;
import java.io.StringReader;

import javax.annotation.Nonnull;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.sling.scripting.resolver.Script;


public class ScriptImpl implements Script {

    private ScriptEngine scriptEngine;
    private String sourceCode;

    public ScriptImpl(ScriptEngine scriptEngine, String sourceCode) {
        this.scriptEngine = scriptEngine;
        this.sourceCode = sourceCode;
    }

    @Override
    public Reader getSourceCodeReader() {
        return new StringReader(sourceCode);
    }

    @Override
    public String getSourceCode() {
        return sourceCode;
    }

    @Override
    public Object eval(@Nonnull Bindings props) throws ScriptException {
        return scriptEngine.eval(sourceCode, props);
    }
}
