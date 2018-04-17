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
package org.apache.sling.feature.scanner.impl;

import java.io.IOException;
import java.util.Collections;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.OSGiRequirement;
import org.apache.sling.feature.analyser.ContainerDescriptor;
import org.apache.sling.feature.scanner.ExtensionScanner;
import org.apache.sling.feature.support.artifact.ArtifactManager;
import org.osgi.resource.Requirement;

public class RepoInitScanner implements ExtensionScanner {

    @Override
    public String getId() {
        return "repoinit";
    }

    @Override
    public String getName() {
        return "Apache Sling Repoinit Scanner";
    }

    @Override
    public ContainerDescriptor scan(final Extension extension,
            final ArtifactManager artifactManager)
    throws IOException {
        if (!Extension.NAME_REPOINIT.equals(extension.getName()) ) {
            return null;
        }
        if ( extension.getType() != ExtensionType.TEXT ) {
            return null;
        }

        final ContainerDescriptor cd = new ContainerDescriptor() {};

        final Requirement req = new OSGiRequirement("osgi.implementation", Collections.emptyMap(),
            Collections.singletonMap("filter", "(&(osgi.implementation=org.apache.sling.jcr.repoinit)(version>=1.0)(!(version>=2.0)))"));
        cd.getRequirements().add(req);

        cd.lock();

        return cd;
    }
}
