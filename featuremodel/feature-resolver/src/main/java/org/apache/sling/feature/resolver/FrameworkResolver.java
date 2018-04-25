/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.feature.resolver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.io.ArtifactManager;
import org.apache.sling.feature.resolver.impl.BundleResourceImpl;
import org.apache.sling.feature.resolver.impl.FeatureResourceImpl;
import org.apache.sling.feature.resolver.impl.ResolveContextImpl;
import org.apache.sling.feature.scanner.BundleDescriptor;
import org.apache.sling.feature.scanner.impl.BundleDescriptorImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.Resolver;

public class FrameworkResolver implements FeatureResolver {
    private final ArtifactManager artifactManager;
    private final Resolver resolver;
    private final FeatureResource frameworkResource;
    private final Framework framework;
    private String tempDirToBeDeleted = null;

    public FrameworkResolver(ArtifactManager am) {
        this(am, getTempDirProps());

        // Since we create the temp dir, the close() method needs to delete it.
        tempDirToBeDeleted = framework.getBundleContext().getProperty(Constants.FRAMEWORK_STORAGE);
    }

    private static Map<String, String> getTempDirProps() {
        try {
            String temp = Files.createTempDirectory("frameworkresolver").toFile().getAbsolutePath();
            return Collections.singletonMap(Constants.FRAMEWORK_STORAGE, temp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public FrameworkResolver(ArtifactManager am, Map<String, String> frameworkProperties) {
        artifactManager = am;

        Resolver r = null;
        // Launch an OSGi framework and obtain its resolver
        try {
            FrameworkFactory fwf = ServiceLoader.load(FrameworkFactory.class).iterator().next();
            framework = fwf.newFramework(frameworkProperties);
            framework.init();
            framework.start();
            BundleContext ctx = framework.getBundleContext();

            // Create a resource representing the framework
            Map<String, List<Capability>> capabilities = new HashMap<>();
            BundleRevision br = framework.adapt(BundleRevision.class);
            capabilities.put(PackageNamespace.PACKAGE_NAMESPACE, br.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE));
            capabilities.put(BundleNamespace.BUNDLE_NAMESPACE, br.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE));
            capabilities.put(HostNamespace.HOST_NAMESPACE, br.getCapabilities(HostNamespace.HOST_NAMESPACE));
            capabilities.put(IdentityNamespace.IDENTITY_NAMESPACE, br.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE));
            frameworkResource = new BundleResourceImpl(framework.getSymbolicName(), framework.getVersion(), null, null,
                    capabilities, Collections.emptyMap());

            int i=0;
            while (i < 20) {
                ServiceReference<Resolver> ref = ctx.getServiceReference(Resolver.class);
                if (ref != null) {
                    r = ctx.getService(ref);
                    break;
                }

                // The service isn't there yet, let's wait a little and try again
                Thread.sleep(500);
                i++;
            }
        } catch (BundleException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        resolver = r;
    }

    @Override
    public void close() throws Exception {
        framework.stop();

        if (tempDirToBeDeleted != null) {
            Files.walk(Paths.get(tempDirToBeDeleted))
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }

    @Override
    public List<FeatureResource> orderResources(List<Feature> features) {
        try {
            return internalOrderResources(features);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<FeatureResource> internalOrderResources(List<Feature> features) throws IOException {
        Map<Feature, FeatureResource> featureMap = new HashMap<>();
        Map<FeatureResource, Feature> resourceMap = new HashMap<>();
        for (Feature f : features) {
            FeatureResourceImpl fr = new FeatureResourceImpl(f);
            resourceMap.put(fr, f);
            featureMap.put(f, fr);

            for (Artifact b : f.getBundles()) {
                BundleDescriptor bd = getBundleDescriptor(artifactManager, b);
                FeatureResource r = new BundleResourceImpl(bd, f);
                resourceMap.put(r, f);
            }
        }

        Map<String, FeatureResource> idVerMap = new HashMap<>();
        for (FeatureResource fr : resourceMap.keySet()) {
            idVerMap.put(fr.getId() + ":" + fr.getVersion(), fr);
        }

        // Add these too
        Artifact lpa = new Artifact(ArtifactId.parse("org.apache.sling/org.apache.sling.launchpad.api/1.2.0"));
        idVerMap.put("org.apache.sling.launchpad.api:1.2.0", new BundleResourceImpl(getBundleDescriptor(artifactManager, lpa), null));
        idVerMap.put(framework.getSymbolicName() + ":" + framework.getVersion(), frameworkResource);

        List<FeatureResource> orderedResources = new LinkedList<>();
        try {
            for (FeatureResource resource : resourceMap.keySet()) {
                if (orderedResources.contains(resource)) {
                    // Already handled
                    continue;
                }
                Map<Resource, List<Wire>> deps = resolver.resolve(new ResolveContextImpl(resource, idVerMap.values()));

                for (Map.Entry<Resource, List<Wire>> entry : deps.entrySet()) {
                    if (resource.equals(entry.getKey()))
                        continue;

                    Resource depResource = entry.getKey();
                    FeatureResource curResource = getFeatureResource(depResource, idVerMap);
                    if (curResource == null)
                        continue;

                    if (!orderedResources.contains(curResource)) {
                        orderedResources.add(curResource);
                    }

                    for (Wire w : entry.getValue()) {
                        FeatureResource provBundle = getFeatureResource(w.getProvider(), idVerMap);
                        if (provBundle == null)
                            continue;

                        int curBundleIdx = orderedResources.indexOf(curResource);
                        int newBundleIdx = orderedResources.indexOf(provBundle);
                        if (newBundleIdx >= 0) {
                            if (curBundleIdx < newBundleIdx) {
                                // If the list already contains the providing but after the current bundle, remove it there to move it before the current bundle
                                orderedResources.remove(provBundle);
                            } else {
                                // If the providing bundle is already before the current bundle, then no need to change anything
                                continue;
                            }
                        }
                        orderedResources.add(curBundleIdx, provBundle);
                    }
                }

                // All of the dependencies of the resource have been added, now add the resource itself
                if (!orderedResources.contains(resource)) {
                    Feature associatedFeature = resource.getFeature();
                    if (resource.equals(featureMap.get(associatedFeature))) {
                        // The resource is a feature resource, don't add this one by itself.
                    }
                    else {
                        orderedResources.add(resource);
                    }
                }
            }
        } catch (ResolutionException e) {
            throw new RuntimeException(e);
        }

        // Sort the fragments so that fragments are started before the host bundle
        for (int i=0; i<orderedResources.size(); i++) {
            Resource r = orderedResources.get(i);
            List<Requirement> reqs = r.getRequirements(HostNamespace.HOST_NAMESPACE);
            if (reqs.size() > 0) {
                // This is a fragment
                Requirement req = reqs.iterator().next(); // TODO handle more host requirements
                String bsn = req.getAttributes().get(HostNamespace.HOST_NAMESPACE).toString(); // TODO this is not valid, should obtain from filter
                // system bundle is already started, no need to reorder here
                if ( Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(bsn)) {
                    continue;
                }
                int idx = getBundleIndex(orderedResources, bsn); // TODO check for filter too
                if (idx < i) {
                    // the fragment is after the host, and should be moved to be before the host
                    FeatureResource frag = orderedResources.remove(i);
                    orderedResources.add(idx, frag);
                }
            }
        }

        // Add the features at the appropriate place to the ordered resources list
        for (int i=0; i<orderedResources.size(); i++) {
            FeatureResource r = orderedResources.get(i);
            FeatureResource associatedFeature = featureMap.get(r.getFeature());
            if (associatedFeature == null)
                continue;

            int idx = orderedResources.indexOf(associatedFeature);
            if (idx > i) {
                orderedResources.remove(idx);
                orderedResources.add(i, associatedFeature);
            } else if (idx == -1) {
                orderedResources.add(i, associatedFeature);
            }
        }

        // If the framework shows up as a dependency, remove it as it's always there
        orderedResources.remove(frameworkResource);

        return orderedResources;
    }

    private FeatureResource getFeatureResource(Resource res, Map<String, FeatureResource> idVerMap) {
        if (res instanceof FeatureResource)
            return (FeatureResource) res;

        // Obtain the identity from the resource and look up in the resource
        List<Capability> caps = res.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
        if (caps.size() == 0) {
            return null;
        }
        Capability cap = caps.get(0);
        Map<String, Object> attrs = cap.getAttributes();
        Object id = attrs.get(IdentityNamespace.IDENTITY_NAMESPACE);
        Object ver = attrs.get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
        String idVer = "" + id + ":" + ver;
        return idVerMap.get(idVer);
    }

    private static int getBundleIndex(List<FeatureResource> bundles, String bundleSymbolicName) {
        for (int i=0; i<bundles.size(); i++) {
            Resource b = bundles.get(i);
            if (bundleSymbolicName.equals(getBundleSymbolicName(b))) {
                return i;
            }
        }
        return -1;
    }

    private static String getBundleSymbolicName(Resource b) {
        for (Capability cap : b.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE)) {
            return cap.getAttributes().get(BundleNamespace.BUNDLE_NAMESPACE).toString();
        }
        return null;
    }

    private static BundleDescriptor getBundleDescriptor(ArtifactManager artifactManager, Artifact b) throws IOException {
        final File file = artifactManager.getArtifactHandler(b.getId().toMvnUrl()).getFile();
        if ( file == null ) {
            throw new IOException("Unable to find file for " + b.getId());
        }

        return new BundleDescriptorImpl(b, file, -1);
    }
}
