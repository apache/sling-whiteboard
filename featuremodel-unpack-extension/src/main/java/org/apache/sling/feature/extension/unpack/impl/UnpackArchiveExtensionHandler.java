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
package org.apache.sling.feature.extension.unpack.impl;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.extension.unpack.Unpack;
import org.apache.sling.feature.spi.context.ExtensionHandler;
import org.apache.sling.feature.spi.context.ExtensionHandlerContext;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

@Component
public class UnpackArchiveExtensionHandler implements ExtensionHandler {
    static final String UNPACK_EXTENSIONS_PROP = "org.apache.sling.feature.unpack.extensions";

    private final Unpack unpack;

    @Activate
    public UnpackArchiveExtensionHandler(BundleContext bc) {
        unpack = Unpack.fromMapping(bc.getProperty(UNPACK_EXTENSIONS_PROP));
    }

    @Override
    public boolean handle(ExtensionHandlerContext context, Extension extension, Feature feature) throws Exception {
        return unpack.handle(extension, context.getArtifactProvider(),
                (u,m) -> context.addInstallableArtifact((ArtifactId) m.get("artifact.id"), u, m));
    }
}

