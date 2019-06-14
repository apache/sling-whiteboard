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

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.Descriptor;

/**
 * Sets timeouts for HTTP calls done using <em>Apache Commons HttpClient 3.x</em>
 * 
 * <p>It inserts two calls in <tt>org.apache.commons.httpclient.params.DefaultHttpParamsFactory.createParams</tt> that set
 * default values for <tt>http.connection.timeout</tt> and <tt>http.socket.timeout</tt>.</p>
 */
public class HttpClient3TimeoutTransformer implements ClassFileTransformer {
    
    private static final String DEFAULT_HTTP_PARAMS_FACTORY_CLASS_NAME = Descriptor.toJvmName("org.apache.commons.httpclient.params.DefaultHttpParamsFactory");
    
    private final long connectTimeoutMillis;
    private final long readTimeoutMillis;
    
    public HttpClient3TimeoutTransformer(long connectTimeoutMillis, long readTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        try {
            if ( DEFAULT_HTTP_PARAMS_FACTORY_CLASS_NAME.equals(className) ) {
                Log.get().log("%s asked to transform %s", getClass().getSimpleName(), className);
                
                ClassPool defaultPool = ClassPool.getDefault();
                CtClass cc = defaultPool.get(Descriptor.toJavaName(className));
                
                CtMethod getSoTimeout =  cc.getDeclaredMethod("createParams");
                // javassist seems unable to resolve the constant values, so just inline them
                // also, unable to resolve calls to setParameter with int values (no boxing?)
                // HttpConnectionParams.CONNECTION_TIMEOUT
                getSoTimeout.insertAfter("$_.setParameter(\"http.connection.timeout\", Integer.valueOf(" + connectTimeoutMillis + "));");
                // HttpMethodParams.SO_TIMEOUT
                getSoTimeout.insertAfter("$_.setParameter(\"http.socket.timeout\", Integer.valueOf(" + readTimeoutMillis + "));");
                
                classfileBuffer = cc.toBytecode();
                cc.detach();
                Log.get().log("Transformation complete.");
            }
            return classfileBuffer;
        } catch (Exception e) {
            Log.get().fatal("Transformation failed", e);
            return null;
        }
    }
}
