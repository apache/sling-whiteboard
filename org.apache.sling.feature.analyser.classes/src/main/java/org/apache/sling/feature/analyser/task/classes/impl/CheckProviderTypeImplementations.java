/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.feature.analyser.task.classes.impl;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckProviderTypeImplementations implements AnalyserTask {

    private static final String PROVIDER_TYPE_ANNOTATION = "Lorg/osgi/annotation/versioning/ProviderType;";
    private static final String MESSAGE = "Type %s %s provider type %s. This is not allowed!";

    static final Logger LOG = LoggerFactory.getLogger(CheckProviderTypeImplementations.class);

    @Override
    public String getId() {
        return "prevent-provider-type-impls";
    }

    @Override
    public String getName() {
        return "Prevents implementation or extension of types marked as Provider";
    }

    @Override
    public void execute(AnalyserTaskContext ctx) throws Exception {
        Set<String> providerTypes = collectProviderTypes(ctx);
        LOG.debug("Provider types found: {}", providerTypes);
        forEachClass(ctx, 
                new ProhibitingSuperTypeAndImplementedInterfacesClassVisitor(ctx, providerTypes),
                ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }

    private Set<String> collectProviderTypes(AnalyserTaskContext ctx) throws IOException {
        AnnotatedTypeCollectorClassVisitor providerTypeCollector = new AnnotatedTypeCollectorClassVisitor(PROVIDER_TYPE_ANNOTATION);
        forEachClass(ctx, providerTypeCollector, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return providerTypeCollector.getProviderTypes();
    }

    private static final class AnnotatedTypeCollectorClassVisitor extends ClassVisitor {

        private final String annotationType;
        private final Set<String> providerTypes;
        private String currentClassName;

        protected AnnotatedTypeCollectorClassVisitor(String annotationType) {
            super(Opcodes.ASM9);
            this.annotationType = annotationType;
            this.providerTypes = new HashSet<>();
        }

        
        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            currentClassName = name;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (descriptor.equals(annotationType)) {
                providerTypes.add(currentClassName);
            }
            return super.visitAnnotation(descriptor, visible);
        }

        public Set<String> getProviderTypes() {
            return providerTypes;
        }
    }

    static String internalToClassName(String internalName) {
        return Type.getObjectType(internalName).getClassName();
    }

    private void forEachClass(AnalyserTaskContext ctx, ClassVisitor visitor, int parsingOptions) throws IOException {
        for (BundleDescriptor bundleDescriptor : ctx.getFeatureDescriptor().getBundleDescriptors()) {
            URL url = bundleDescriptor.getArtifactFile();
            LOG.debug("Checking classes in bundle {}", url);
            try (final JarInputStream jis = new JarInputStream(url.openStream())) {
                if (visitor instanceof ArtifactContextAwareClassVisitor) {
                    ArtifactContextAwareClassVisitor acacv = (ArtifactContextAwareClassVisitor)visitor;
                    acacv.visitArtifact(bundleDescriptor.getArtifact().getId());
                }
                forEachClass(jis, visitor, parsingOptions);
            }
        }
    }

    private void forEachClass(JarInputStream jarInputStream, ClassVisitor visitor, int parsingOptions) throws IOException {
        JarEntry jarEntry;
        while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
            if (jarEntry.getName().endsWith(".class")) {
                ClassReader classReader = new ClassReader(jarInputStream);
                classReader.accept(visitor, parsingOptions);
            }
            jarInputStream.closeEntry();
        }
    }

    private static final class ProhibitingSuperTypeAndImplementedInterfacesClassVisitor extends ArtifactContextAwareClassVisitor {

        private final Set<String> prohibitedTypes;

        protected ProhibitingSuperTypeAndImplementedInterfacesClassVisitor(AnalyserTaskContext ctx, Set<String> prohibitedTypes) {
            super(ctx);
            this.prohibitedTypes = prohibitedTypes;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            if (prohibitedTypes.contains(superName)) {
                reportError(String.format(MESSAGE, internalToClassName(name), "implements", internalToClassName(superName)));
            }
            Arrays.stream(interfaces).filter(prohibitedTypes::contains).forEach(s -> reportError(String.format(MESSAGE, internalToClassName(name), "extends", internalToClassName(s))));
        }
    }
}
