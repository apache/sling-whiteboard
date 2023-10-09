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

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public abstract class ArtifactContextAwareClassVisitor extends ClassVisitor {

    private final AnalyserTaskContext ctx;
    private ArtifactId currentArtifactId;

    ArtifactContextAwareClassVisitor(AnalyserTaskContext ctx) {
        super(Opcodes.ASM9);
        this.ctx = ctx;
    }

    /**
     * Called for each visited artifact prior to all other method calls from {@link ClassVisitors}
     * @param artifactId the artifact id of the currently visited class
     */
    public void visitArtifact(ArtifactId artifactId) {
        this.currentArtifactId = artifactId;
    }

    /**
     * Report an error in the context of the of the given {@link AnalyserTaskContext} 
     * and the artifact given to {@link #visitArtifact(ArtifactId)}
     * @param message the error message to emit
     */
    public void reportError(String message) {
        ctx.reportArtifactError(currentArtifactId, message);
    }
}
