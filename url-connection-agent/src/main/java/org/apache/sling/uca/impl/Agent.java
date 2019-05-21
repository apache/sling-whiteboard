package org.apache.sling.uca.impl;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.HashMap;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public class Agent {

    public static void premain(String args, Instrumentation inst) {

        System.out.println("Loading agent...");
        inst.addTransformer(new HashMapTransformer(), true);
        try {
            inst.retransformClasses(HashMap.class);
        } catch (UnmodifiableClassException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Loaded agent!");
    }
    
    public static void agentmain(String args, Instrumentation inst) {
        premain(args, inst);
    }

    static class HashMapTransformer implements ClassFileTransformer {

        private final Class<?> klazz = HashMap.class;
        
        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            try {
                if ( classBeingRedefined == klazz) {
                    System.out.println("Asked to transform " + className);
                    CtClass cc = ClassPool.getDefault().get(klazz.getName());
                    CtMethod putMethod = cc.getDeclaredMethod("put");
                    putMethod.insertAfter("System.out.println(\"[AGENT] Adding key \" + key );");
                    classfileBuffer = cc.toBytecode();
                    cc.detach();
                    System.err.println("Transformation complete!");
                }
                return classfileBuffer;
            } catch (NotFoundException | CannotCompileException | IOException e) {
                throw new RuntimeException("Transformation failed", e);
            }
        }
    }
}
    
