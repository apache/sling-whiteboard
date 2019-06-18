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
package org.apache.sling.uca.impl;

import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;

/**
 * Sets timeouts for HTTP calls done using <tt>java.net.URL</tt>/<tt>java.net.URLConnection</tt>.
 * 
 * <p>It transforms calls to <tt>connect</tt> methods of internal URL connection classes to set the
 * connect and read timeout in case they have the default value of <tt>0</tt>.</p>
 * 
 * @see URLConnection#getConnectTimeout()
 * @see URLConnection#getReadTimeout()
 *
 */
class JavaNetTimeoutTransformer extends MBeanAwareTimeoutTransformer {

    static final Set<String> CLASSES_TO_TRANSFORM = new HashSet<>();

    static {
        CLASSES_TO_TRANSFORM.add(Descriptor.toJvmName("sun.net.www.protocol.http.HttpURLConnection"));
        CLASSES_TO_TRANSFORM.add(Descriptor.toJvmName("sun.net.www.protocol.https.AbstractDelegateHttpsURLConnection"));
    }

    private final long readTimeoutMillis;
    private final long connectTimeoutMillis;
    public JavaNetTimeoutTransformer(long connectTimeout, long readTimeout, AgentInfo agentInfo) {
        
        super(agentInfo, CLASSES_TO_TRANSFORM);
        
        this.connectTimeoutMillis = connectTimeout;
        this.readTimeoutMillis = readTimeout;
    }

    protected byte[] doTransformClass(String className) throws Exception {
        CtMethod connectMethod = findConnectMethod(className);
        connectMethod.insertBefore("if ( getConnectTimeout() == 0 ) { setConnectTimeout(" + connectTimeoutMillis + "); }");
        connectMethod.insertBefore("if ( getReadTimeout() == 0 ) { setReadTimeout(" + readTimeoutMillis + "); }");
        byte[] classfileBuffer = connectMethod.getDeclaringClass().toBytecode();
        connectMethod.getDeclaringClass().detach();
        return classfileBuffer;
    }
    
    CtMethod findConnectMethod(String className) throws NotFoundException {
        
        ClassPool defaultPool = ClassPool.getDefault();
        CtClass cc = defaultPool.get(Descriptor.toJavaName(className));
        if (cc == null) {
            Log.get().log("No class found with name %s", className);
            return null;
        }
        return cc.getDeclaredMethod("connect");

    }

}