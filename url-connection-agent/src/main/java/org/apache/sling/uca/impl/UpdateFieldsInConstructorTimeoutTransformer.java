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
import javassist.CtConstructor;
import javassist.CtField;
import javassist.bytecode.Descriptor;

/**
 * Support class for transformers that update the timeout fields in the default constructor
 */
public abstract class UpdateFieldsInConstructorTimeoutTransformer extends MBeanAwareTimeoutTransformer {

    private final String connectTimeoutFieldName;
    private final String readTimeoutFieldName;
    private final long connectTimeoutMillis;
    private final long readTimeoutMillis;

    public UpdateFieldsInConstructorTimeoutTransformer(String className, String connectTimeoutFieldName,
            String readTimeoutFieldName, long connectTimeoutMillis, long readTimeoutMillis, AgentInfo agentInfo) {

        super(agentInfo, Collections.singleton(className));
        
        this.connectTimeoutFieldName = connectTimeoutFieldName;
        this.readTimeoutFieldName = readTimeoutFieldName;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
    }
    
    @Override
    protected byte[] doTransformClass(CtClass cc) throws Exception {
        
        CtConstructor noArgCtor = cc.getConstructor(Descriptor.ofConstructor(new CtClass[0]));
        CtField connectTimeout = cc.getDeclaredField(connectTimeoutFieldName);
        CtField readTimeout = cc.getDeclaredField(readTimeoutFieldName);
        noArgCtor.insertAfter("this." + connectTimeout.getName() + " = " + connectTimeoutMillis + ";");
        noArgCtor.insertAfter("this." + readTimeout.getName() + " = " + readTimeoutMillis + ";");
        
        byte[] classfileBuffer = cc.toBytecode();
        cc.detach();
        
        return classfileBuffer;
    }

}
