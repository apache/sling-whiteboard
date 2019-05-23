package org.apache.sling.uca.impl;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public class Agent {

    public static void premain(String args, Instrumentation inst) {

        System.out.println("Loading agent...");
        inst.addTransformer(new URLTimeoutTransformer(), true);
        System.out.println("Loaded agent!");
    }
    
    public static void agentmain(String args, Instrumentation inst) {
        premain(args, inst);
    }

    static class URLTimeoutTransformer implements ClassFileTransformer {
        
        private static final Set<String> CLASSES_TO_TRANSFORM = new HashSet<>();
        
        static {
            CLASSES_TO_TRANSFORM.add("sun.net.www.protocol.http.HttpURLConnection".replace('.', '/'));
            CLASSES_TO_TRANSFORM.add("sun.net.www.protocol.https.HttpsURLConnectionImpl".replace('.', '/'));
        }
        
        private final Class<?> klazz = HashMap.class;
        
        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            try {
                if ( CLASSES_TO_TRANSFORM.contains(className)) {
                    System.out.println("Asked to transform " + className);
                    CtClass cc = ClassPool.getDefault().get(klazz.getName());
                    CtMethod connectMethod = cc.getDeclaredMethod("connect");
                    connectMethod.insertBefore("if ( getConnectTimeout() == 0 ) { setConnectTimeout(60); }");
                    connectMethod.insertBefore("if ( getReadTimeout() == 0 ) { setReadTimeout(60); }");
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
    
