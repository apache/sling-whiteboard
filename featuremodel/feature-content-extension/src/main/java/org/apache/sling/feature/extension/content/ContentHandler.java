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
package org.apache.sling.feature.extension.content;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.SubPackageHandling;
import org.apache.jackrabbit.vault.packaging.registry.ExecutionPlanBuilder;
import org.apache.jackrabbit.vault.packaging.registry.PackageTask.Type;
import org.apache.jackrabbit.vault.packaging.registry.impl.FSPackageRegistry;
import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.FeatureConstants;
import org.apache.sling.feature.launcher.spi.LauncherPrepareContext;
import org.apache.sling.feature.launcher.spi.extensions.ExtensionHandler;
import org.apache.sling.feature.launcher.spi.extensions.ExtensionInstallationContext;

public class ContentHandler implements ExtensionHandler {
    
    public static final String PACKAGEREGISTRY_HOME = "packageregistry.home";

    private static final String REPOSITORY_HOME = "repository.home";

    private static final String REGISTRY_FOLDER = "packageregistry";

    private static ExecutionPlanBuilder buildExecutionPlan(Collection<Artifact> artifacts, LauncherPrepareContext prepareContext, File registryHome) throws Exception {

        List<File> packageReferences = new ArrayList<File>();

        for (final Artifact a : artifacts) {
            final File file = prepareContext.getArtifactFile(a.getId());
            if (file.exists()) {
                packageReferences.add(file);
            }

        }
        
        if(!registryHome.exists()) {
            registryHome.mkdirs();
        }

        FSPackageRegistry registry = new FSPackageRegistry(registryHome);

        ExecutionPlanBuilder builder = registry.createExecutionPlan();

        for (File pkgFile : packageReferences) {
            PackageId pid = registry.registerExternal(pkgFile, true);
            Map<PackageId, SubPackageHandling.Option> subPkgs = registry.getInstallState(pid).getSubPackages();
            if (!subPkgs.isEmpty()) {
                for (PackageId subId : subPkgs.keySet()) {
                    SubPackageHandling.Option opt = subPkgs.get(subId);
                    if (opt != SubPackageHandling.Option.IGNORE) {
                        builder.addTask().with(subId).with(Type.EXTRACT);
                    }
                }
            }

            builder.addTask().with(pid).with(Type.EXTRACT);
        }
        builder.validate();
        return builder;

    }

    @Override
    public boolean handle(Extension extension, LauncherPrepareContext prepareContext,
            ExtensionInstallationContext installationContext) throws Exception {
        File registryHome = getRegistryHomeDir(installationContext);
        if (extension.getType() == ExtensionType.ARTIFACTS
                && extension.getName().equals(FeatureConstants.EXTENSION_NAME_CONTENT_PACKAGES)) {
            MultiValueMap orderedArtifacts = MultiValueMap.decorate(new LinkedHashMap<Integer, Collection<Artifact>>());
            for (final Artifact a : extension.getArtifacts()) {
                int order;
                // content-packages without explicit start-order to be installed last
                if (a.getMetadata().get(Artifact.KEY_START_ORDER) != null) {
                    order = a.getStartOrder();
                } else {
                    order = Integer.MAX_VALUE;
                }
                orderedArtifacts.put(order, a);
            }
            List<String> executionPlans = new ArrayList<String>();
            for (Object key : orderedArtifacts.keySet()) {
                @SuppressWarnings("unchecked")
                Collection<Artifact> artifacts = orderedArtifacts.getCollection(key);
                ExecutionPlanBuilder builder = buildExecutionPlan(artifacts, prepareContext, registryHome);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                builder.save(baos);
                executionPlans.add(baos.toString("UTF-8"));
            }
            final Configuration initcfg = new Configuration("org.apache.sling.jcr.packageinit.impl.ExecutionPlanRepoInitializer");
            initcfg.getProperties().put("executionplans", executionPlans.toArray(new String[executionPlans.size()]));
            installationContext.addConfiguration(initcfg.getPid(), initcfg.getFactoryPid(), initcfg.getProperties());
            
            final Configuration registrycfg = new Configuration("org.apache.jackrabbit.vault.packaging.registry.impl.FSPackageRegistry");
            registrycfg.getProperties().put("homePath", REGISTRY_FOLDER);
            installationContext.addConfiguration(registrycfg.getPid(), registrycfg.getFactoryPid(), registrycfg.getProperties());;

            return true;
        }
        else {
            return false;
        }
    }

    private File getRegistryHomeDir(ExtensionInstallationContext installationContext) {
        //read repository- home from framework properties (throw exception if repo.home not set)
        String registryPath = System.getProperty(PACKAGEREGISTRY_HOME);
        File registryHome;
        if (registryPath != null) {
            registryHome = Paths.get(registryPath).toFile();

        } else {
            String repoHome = installationContext.getFrameworkProperties().get(REPOSITORY_HOME);
            if (repoHome == null) {
                throw new IllegalStateException("Neither registry.home set nor repository.home configured.");
            } 
            registryHome = Paths.get(repoHome, REGISTRY_FOLDER).toFile();
        }
        if (!registryHome.exists()) {
            registryHome.mkdirs();
        }
        if (!registryHome.isDirectory()) {
            throw new IllegalStateException("Registry but points to file - must be directory");
        }
        return registryHome;
    }
}
