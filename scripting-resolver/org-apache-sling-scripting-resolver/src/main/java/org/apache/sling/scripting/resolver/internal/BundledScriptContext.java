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
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import org.apache.sling.api.scripting.SlingScriptConstants;

class BundledScriptContext extends SimpleScriptContext {

    private static final Integer[] SCOPES = {SlingScriptConstants.SLING_SCOPE, GLOBAL_SCOPE, ENGINE_SCOPE};

    private Bindings globalScope;
    private Bindings engineScope;

    private Bindings slingScope = new SimpleBindings();

    @Override
    public void setBindings(final Bindings bindings, final int scope) {
        switch (scope) {
            case SlingScriptConstants.SLING_SCOPE : this.slingScope = bindings;
                break;
            case 100: if (bindings == null) throw new NullPointerException("Bindings for ENGINE scope is null");
                this.engineScope = bindings;
                break;
            case 200: this.globalScope = bindings;
                break;
            default: throw new IllegalArgumentException("Invalid scope");
        }
    }

    @Override
    public Bindings getBindings(final int scope) {
        switch (scope) {
            case SlingScriptConstants.SLING_SCOPE : return slingScope;
            case 100: return this.engineScope;
            case 200: return this.globalScope;
        }
        throw new IllegalArgumentException("Invalid scope");
    }

    @Override
    public void setAttribute(final String name, final Object value, final int scope) {
        if (name == null) throw new IllegalArgumentException("Name is null");
        final Bindings bindings = getBindings(scope);
        if (bindings != null) {
            bindings.put(name, value);
        }
    }

    @Override
    public Object getAttribute(final String name, final int scope) {
        if (name == null) throw new IllegalArgumentException("Name is null");
        final Bindings bindings = getBindings(scope);
        if (bindings != null) {
            return bindings.get(name);
        }
        return null;
    }

    @Override
    public Object removeAttribute(final String name, final int scope) {
        if (name == null) throw new IllegalArgumentException("Name is null");
        final Bindings bindings = getBindings(scope);
        if (bindings != null) {
            return bindings.remove(name);
        }
        return null;
    }

    @Override
    public Object getAttribute(String name) {
        if (name == null) throw new IllegalArgumentException("Name is null");
        for (final int scope : SCOPES) {
            final Bindings bindings = getBindings(scope);
            if ( bindings != null ) {
                final Object o = bindings.get(name);
                if ( o != null ) {
                    return o;
                }
            }
        }
        return null;
    }

    @Override
    public int getAttributesScope(String name) {
        if (name == null) throw new IllegalArgumentException("Name is null");
        for (final int scope : SCOPES) {
            if ((getBindings(scope) != null) && (getBindings(scope).containsKey(name))) {
                return scope;
            }
        }
        return -1;
    }

    @Override
    public List<Integer> getScopes() {
        return Arrays.asList(SCOPES);
    }

    @Override
    public Writer getWriter() {
        return writer;
    }

    @Override
    public Writer getErrorWriter() {
        return errorWriter;
    }

    @Override
    public void setWriter(Writer writer) {
        this.writer = writer;
    }

    @Override
    public void setErrorWriter(Writer writer) {
        this.errorWriter = writer;
    }

    @Override
    public Reader getReader() {
        return reader;
    }

    @Override
    public void setReader(Reader reader) {
        this.reader = reader;
    }


}
