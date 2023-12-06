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
package org.apache.sling.feature.launcher.atomos.weaver.impl;

import java.io.InputStream;
import java.net.URL;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.commons.Method;

public class AtomosWeaverVisitor  extends ClassVisitor implements Opcodes {
    public static byte[] weave(byte[] bytes, String targetClass, String targetMethodClassLoader, String targetMethodResource, String targetMethodStream, ClassLoader cl) {
        ClassReader cr = new ClassReader(bytes);
        ClassWriter cw = new StaticToolClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES, cl);
        AtomosWeaverVisitor cv = new AtomosWeaverVisitor(cw,
            Type.getType("L" + targetClass.replace('.', '/') + ";"),
            new Method(targetMethodClassLoader,
                    Type.getType(ClassLoader.class),
                    new Type[] {Type.getType(Class.class)}
            ),new Method(targetMethodResource,
                Type.getType(URL.class),
                new Type[] {Type.getType(Class.class),Type.getType(String.class)}
            ),new Method(targetMethodStream,
                Type.getType(InputStream.class),
                new Type[] {Type.getType(Class.class), Type.getType(String.class)}
            ),new Method("getEntry",
                Type.getType(URL.class),
                new Type[] {Type.getType(String.class)}        )
        );
        cr.accept(cv, ClassReader.SKIP_FRAMES);
        if (cv.isWoven()) {
            return cw.toByteArray();
        } else {
            return bytes;
        }
    }

    private volatile boolean m_woven = false;
    private final Type target;
    private final Method targetMethodClassLoader;
    private final Method targetMethodResource;
    private final Method targetMethodStream;
    private final Method targetMethodBundleResource;

    AtomosWeaverVisitor(ClassWriter cv, Type target, Method targetMethodClassLoader, Method targetMethodResource, Method targetMethodStream, Method targetMethodBundleResource) {
        super(Opcodes.ASM9, cv);
        this.target = target;
        this.targetMethodClassLoader = targetMethodClassLoader;
        this.targetMethodResource = targetMethodResource;
        this.targetMethodStream = targetMethodStream;
        this.targetMethodBundleResource = targetMethodBundleResource;
    }

    boolean isWoven() {
        return m_woven;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        mv = new MethodWeaverVisitor(mv, access, name, desc);
        mv = new JSRInlinerAdapter(mv, access, name, desc, signature, exceptions);

        return mv;
    }

    private class MethodWeaverVisitor extends GeneratorAdapter {
        public MethodWeaverVisitor(MethodVisitor mv, int access, String name, String descriptor) {
            super(Opcodes.ASM7, mv, access, name, descriptor);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (opcode == INVOKEVIRTUAL && owner.replace('/', '.').equals("java.lang.Class")) {
                Method targetMethod;
                if (name.equals("getClassLoader")) {
                    targetMethod = targetMethodClassLoader;
                } else if (name.equals("getResource")) {
                    targetMethod = targetMethodResource;
                } else if (name.equals("getResourceAsStream")) {
                    targetMethod = targetMethodStream;
                } else {
                    targetMethod = null;
                }
                if (targetMethod != null) {
                    invokeStatic(target, targetMethod);
                    m_woven = true;
                    return;
                }
            }

            if (opcode == INVOKEINTERFACE && owner.replace('/', '.').equals("org.osgi.framework.Bundle")) {
                if (name.equals("getResource")) {
                    System.out.println("@@@ getResource");
                    invokeInterface(Type.getType("L" + owner.replace('.', '/') + ";"), targetMethodBundleResource);
                    m_woven = true;
                    return;
                }
            }
            /*
            if (opcode == INVOKEVIRTUAL && owner.replace('/', '.').equals("org.osgi.framework.Bundle")) {
                // Method targetMethod;
                if (name.equals("getResource")) {
                    name = "getEntry";
                    m_woven = true;
                    System.out.println("@@@ getResource");
                }
                // } else if (name.equals("getResources")) {
                //     targetMethod = targetMethodStream;
                // } else {
                //     targetMethod = null;
                // }
                // if (targetMethod != null) {
                //     invokeStatic(target, targetMethod);
                //     m_woven = true;
                //     return;
                // }
            }
            */
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }
}
