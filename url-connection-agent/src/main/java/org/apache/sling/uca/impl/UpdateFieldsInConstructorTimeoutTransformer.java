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
import javassist.CtConstructor;
import javassist.CtField;
import javassist.bytecode.Descriptor;

/**
 * Support class for transformers that update the timeout fields in the default constructor
 */
public abstract class UpdateFieldsInConstructorTimeoutTransformer implements ClassFileTransformer {

    private final String className;
    private final String connectTimeoutFieldName;
    private final String readTimeoutFieldName;
    private final long connectTimeoutMillis;
    private final long readTimeoutMillis;

    public UpdateFieldsInConstructorTimeoutTransformer(String className, String connectTimeoutFieldName,
            String readTimeoutFieldName, long connectTimeoutMillis, long readTimeoutMillis) {

        this.className = className;
        this.connectTimeoutFieldName = connectTimeoutFieldName;
        this.readTimeoutFieldName = readTimeoutFieldName;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        try {
            if ( this.className.equals(className) ) {
                Log.get().log("%s asked to transform %s", getClass().getSimpleName(), className);
                
                ClassPool defaultPool = ClassPool.getDefault();
                CtClass cc = defaultPool.get(Descriptor.toJavaName(className));
                
                CtConstructor noArgCtor = cc.getConstructor(Descriptor.ofConstructor(new CtClass[0]));
                CtField connectTimeout = cc.getDeclaredField(connectTimeoutFieldName);
                CtField readTimeout = cc.getDeclaredField(readTimeoutFieldName);
                noArgCtor.insertAfter("this." + connectTimeout.getName() + " = " + connectTimeoutMillis + ";");
                noArgCtor.insertAfter("this." + readTimeout.getName() + " = " + readTimeoutMillis + ";");
                
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
