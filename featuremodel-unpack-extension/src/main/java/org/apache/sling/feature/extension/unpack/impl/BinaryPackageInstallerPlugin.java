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

import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.function.Function;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

@Component(service = { InstallTaskFactory.class, ResourceTransformer.class })
//@Designate(ocd = BinaryPackageInstallerPlugin.Config.class)
public class BinaryPackageInstallerPlugin implements InstallTaskFactory, ResourceTransformer {
    public static final String BINARY_ARCHIVE_VERSION_HEADER = "Binary-Archive-Version";
    public static final String TYPE_BINARY_ARCHIVE = "binaryarchive";

    @Activate
    private BundleContext bundleContext;

    @Override
    public TransformationResult[] transform(RegisteredResource resource) {
        if (!InstallableResource.TYPE_FILE.equals(resource.getType())) {
//                || !handledExtension(resource.getURL())) {
            return null;
        }

        Dictionary<String, Object> dict = resource.getDictionary();
        if (dict == null) {
            dict = new Hashtable<>();
        }

        Unpack unpack = (Unpack) dict.get("__unpack__");
        if (unpack == null) {
            unpack = Unpack.fromMapping(bundleContext.getProperty(bundleContext.getProperty(
                BinaryArtifactExtensionHandler.BINARY_EXTENSIONS_PROP)));
            dict.put("__unpack__", unpack);
        }

        // Should be something like
//        if (!unpack.handles(resource.getInputStream())) {
//            return null;
//        }

        try (JarInputStream jis = new JarInputStream(resource.getInputStream())) {
            Manifest mf = jis.getManifest();
            if (!"1".equals(mf.getMainAttributes().getValue(BINARY_ARCHIVE_VERSION_HEADER))) {
                return null;
            }
        } catch (IOException e) {
            // Couldn't read the manifest, maybe not a Jar file
            return null;
        }

        try {
            ArtifactId aid = (ArtifactId) dict.get("artifact.id");
            if (aid == null) {
                String u = resource.getURL();
                int idx = u.lastIndexOf('/');
                String name = u.substring(idx + 1);
                int idx2 = name.lastIndexOf('.');
                if (idx2 >= 0) {
                    name = name.substring(0, idx2);
                }
                aid = new ArtifactId("binary.packages", name, resource.getDigest(), null, null);
            }

            TransformationResult tr = new TransformationResult();
            tr.setResourceType(TYPE_BINARY_ARCHIVE);
            tr.setId(aid.getGroupId() + ":" + aid.getArtifactId());
            tr.setInputStream(resource.getInputStream());
            Map<String,Object> attrs = Collections.list(dict.keys()).stream()
                       .collect(Collectors.toMap(Function.identity(), dict::get));
            tr.setAttributes(attrs);
            tr.getAttributes().put("context", attrs);

            return new TransformationResult [] {tr};
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public InstallTask createTask(TaskResourceGroup group) {
        TaskResource tr = group.getActiveResource();
        if (!TYPE_BINARY_ARCHIVE.equals(tr.getType())) {
            return null;
        }
        if (tr.getState() == ResourceState.UNINSTALL) {
            // TODO
            return null;
        }

        return new InstallBinaryArchiveTask(group);
    }

}
