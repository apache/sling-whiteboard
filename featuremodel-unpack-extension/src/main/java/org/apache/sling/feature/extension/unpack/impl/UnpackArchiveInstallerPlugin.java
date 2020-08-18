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
import org.apache.sling.feature.extension.unpack.Unpack;
import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallTaskFactory;
import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.ResourceTransformer;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.apache.sling.installer.api.tasks.TransformationResult;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component(service = { InstallTaskFactory.class, ResourceTransformer.class })
public class UnpackArchiveInstallerPlugin implements InstallTaskFactory, ResourceTransformer {
    public static final String TYPE_UNPACK_ARCHIVE = "unpackarchive";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    final Unpack unpack;

    @Activate
    public UnpackArchiveInstallerPlugin(BundleContext bc) {
        this(Unpack.fromMapping(bc.getProperty(UnpackArchiveExtensionHandler.UNPACK_EXTENSIONS_PROP)));
    }

    UnpackArchiveInstallerPlugin(Unpack unpack) {
        this.unpack = unpack;
    }

    @Override
    public TransformationResult[] transform(RegisteredResource resource) {
        if (!InstallableResource.TYPE_FILE.equals(resource.getType())) {
            return null;
        }

        Dictionary<String, Object> dict = resource.getDictionary();
        if (dict == null) {
            dict = new Hashtable<>();
        }

        Map<String,Object> context = Collections.list(dict.keys()).stream()
                .collect(Collectors.toMap(Function.identity(), dict::get));

        try {
            if (!unpack.handles(resource.getInputStream(), context)) {
                return null;
            }
        } catch (IOException e) {
            logger.warn("Unable to read stream from {}", resource.getURL(), e);
            return null;
        }
        try {
            ArtifactId aid = (ArtifactId) dict.get("artifact.id");
            if (aid == null) {
                // If aid is not set, the archive doesn't come from a feature model, and we'd have
                // to generate some sort of ID for it...
                String u = resource.getURL();
                int idx = u.lastIndexOf('/');
                String name = u.substring(idx + 1);
                int idx2 = name.lastIndexOf('.');
                if (idx2 >= 0) {
                    name = name.substring(0, idx2);
                }
                aid = new ArtifactId("unpack.packages", name, resource.getDigest(), null, null);
            }

            TransformationResult tr = new TransformationResult();
            tr.setResourceType(TYPE_UNPACK_ARCHIVE);
            tr.setId(aid.getGroupId() + ":" + aid.getArtifactId());
            tr.setInputStream(resource.getInputStream());
            tr.setAttributes(context);
            tr.getAttributes().put("context", context);

            return new TransformationResult [] {tr};
        } catch (IOException e) {
            logger.warn("Problem processing {}", resource.getURL(), e);
            return null;
        }
    }

    @Override
    public InstallTask createTask(TaskResourceGroup group) {
        TaskResource tr = group.getActiveResource();
        if (!TYPE_UNPACK_ARCHIVE.equals(tr.getType())) {
            return null;
        }
        if (tr.getState() != ResourceState.INSTALL) {
            return null;
        }

        return new InstallUnpackArchiveTask(group, unpack, logger);
    }

}
