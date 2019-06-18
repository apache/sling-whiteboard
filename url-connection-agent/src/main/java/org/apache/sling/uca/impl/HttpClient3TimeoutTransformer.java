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

import java.util.Collections;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.Descriptor;

/**
 * Sets timeouts for HTTP calls done using <em>Apache Commons HttpClient 3.x</em>
 * 
 * <p>It inserts two calls in <tt>org.apache.commons.httpclient.params.DefaultHttpParamsFactory.createParams</tt> that set
 * default values for <tt>http.connection.timeout</tt> and <tt>http.socket.timeout</tt>.</p>
 */
public class HttpClient3TimeoutTransformer extends MBeanAwareTimeoutTransformer {
    
    private static final String DEFAULT_HTTP_PARAMS_FACTORY_CLASS_NAME = Descriptor.toJvmName("org.apache.commons.httpclient.params.DefaultHttpParamsFactory");
    
    private final long connectTimeoutMillis;
    private final long readTimeoutMillis;
    
    public HttpClient3TimeoutTransformer(long connectTimeoutMillis, long readTimeoutMillis, AgentInfo agentInfoMBean) {
        super(agentInfoMBean, Collections.singleton(DEFAULT_HTTP_PARAMS_FACTORY_CLASS_NAME));
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    @Override
    protected byte[] doTransformClass(CtClass cc) throws Exception {
        
        CtMethod getSoTimeout =  cc.getDeclaredMethod("createParams");
        // javassist seems unable to resolve the constant values, so just inline them
        // also, unable to resolve calls to setParameter with int values (no boxing?)
        // HttpConnectionParams.CONNECTION_TIMEOUT
        getSoTimeout.insertAfter("$_.setParameter(\"http.connection.timeout\", Integer.valueOf(" + connectTimeoutMillis + "));");
        // HttpMethodParams.SO_TIMEOUT
        getSoTimeout.insertAfter("$_.setParameter(\"http.socket.timeout\", Integer.valueOf(" + readTimeoutMillis + "));");
        
        byte[] classfileBuffer = cc.toBytecode();
        cc.detach();
        
        return classfileBuffer;
    }

}
