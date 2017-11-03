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
package org.apache.sling.feature.scanner;

import java.io.IOException;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.analyser.ContainerDescriptor;
import org.apache.sling.feature.support.ArtifactManager;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * The extension scanner scans an extension.
 */
@ConsumerType
public interface ExtensionScanner  {

    /** A unique (short) id. */
    String getId();

    /** A human readable name to identify the scanner. */
    String getName();

    /**
     * Try to scan the extension and return a descriptor
     *
     * @param extension The extension
     * @param manager Artifact manager
     * @return The descriptor or {@code null} if the scanner does not know the extension
     * @throws IOException If an error occurs while scanning the extension or the extension is invalid
     */
    ContainerDescriptor scan(Extension extension,
            ArtifactManager manager) throws IOException;
}