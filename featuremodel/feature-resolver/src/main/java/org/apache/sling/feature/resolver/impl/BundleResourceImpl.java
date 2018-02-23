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
package org.apache.sling.feature.resolver.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.sling.feature.OSGiCapability;
import org.apache.sling.feature.OSGiRequirement;
import org.apache.sling.feature.analyser.BundleDescriptor;
import org.apache.sling.feature.support.util.PackageInfo;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

/**
 * Implementation of the OSGi Resource interface.
 */
public class BundleResourceImpl implements Resource {
    final String hint;
    final Map<String, List<Capability>> capabilities;
    final Map<String, List<Requirement>> requirements;

    /**
     * Create a resource based on a BundleDescriptor.
     * @param bd The BundleDescriptor to represent.
     */
    public BundleResourceImpl(BundleDescriptor bd) {
        hint = bd.getBundleSymbolicName() + " " + bd.getBundleVersion();
        Map<String, List<Capability>> caps = new HashMap<>();
        for (Capability c : bd.getCapabilities()) {
            List<Capability> l = caps.get(c.getNamespace());
            if (l == null) {
                l = new ArrayList<>();
                caps.put(c.getNamespace(), l);
            }
            l.add(new OSGiCapability(this, c));
        }

        // Add the package capabilities (export package)
        List<Capability> pkgCaps = new ArrayList<>();
        for(PackageInfo exported : bd.getExportedPackages()) {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put(PackageNamespace.PACKAGE_NAMESPACE, exported.getName());
            attrs.put(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE, exported.getPackageVersion());
            attrs.put(PackageNamespace.CAPABILITY_BUNDLE_SYMBOLICNAME_ATTRIBUTE, bd.getBundleSymbolicName());
            attrs.put(PackageNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, new Version(bd.getBundleVersion()));
            pkgCaps.add(new OSGiCapability(this, PackageNamespace.PACKAGE_NAMESPACE, attrs, Collections.emptyMap()));
        }
        caps.put(PackageNamespace.PACKAGE_NAMESPACE, Collections.unmodifiableList(pkgCaps));

        // Add the bundle capability
        Map<String, Object> battrs = new HashMap<>();
        battrs.put(BundleNamespace.BUNDLE_NAMESPACE, bd.getBundleSymbolicName());
        battrs.put(BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, new Version(bd.getBundleVersion()));
        OSGiCapability bundleCap = new OSGiCapability(this, BundleNamespace.BUNDLE_NAMESPACE, battrs, Collections.emptyMap());
        caps.put(BundleNamespace.BUNDLE_NAMESPACE, Collections.singletonList(bundleCap));
        capabilities = Collections.unmodifiableMap(caps);

        Map<String, List<Requirement>> reqs = new HashMap<>();
        for (Requirement r : bd.getRequirements()) {
            List<Requirement> l = reqs.get(r.getNamespace());
            if (l == null) {
                l = new ArrayList<>();
                reqs.put(r.getNamespace(), l);
            }
            // Add the requirement and associate with this resource
            l.add(new OSGiRequirement(this, r));
        }

        // TODO What do we do with the execution environment?
        reqs.remove(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);

        // Add the package requirements (import package)
        List<Requirement> pkgReqs = new ArrayList<>();
        for(PackageInfo imported : bd.getImportedPackages()) {
            Map<String, String> dirs = new HashMap<>();
            VersionRange range = imported.getPackageVersionRange();
            String rangeFilter;
            if (range != null) {
                rangeFilter = range.toFilterString(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);
            } else {
                rangeFilter = "";
            }
            dirs.put(PackageNamespace.REQUIREMENT_FILTER_DIRECTIVE,
                "(&(" + PackageNamespace.PACKAGE_NAMESPACE + "=" + imported.getName() + ")" + rangeFilter + ")");
            if (imported.isOptional())
                dirs.put(PackageNamespace.REQUIREMENT_RESOLUTION_DIRECTIVE,
                    PackageNamespace.RESOLUTION_OPTIONAL);
            pkgReqs.add(new OSGiRequirement(this, PackageNamespace.PACKAGE_NAMESPACE, Collections.emptyMap(), dirs));
        }
        reqs.put(PackageNamespace.PACKAGE_NAMESPACE, Collections.unmodifiableList(pkgReqs));
        requirements = Collections.unmodifiableMap(reqs);
    }

    /**
     * Constructor. Create a resource based on capabilties and requirements.
     * @param hnt
     * @param caps The capabilities of the resource.
     * @param reqs The requirements of the resource.
     */
    public BundleResourceImpl(Map<String, List<Capability>> caps, Map<String, List<Requirement>> reqs) {
        hint = "" + System.identityHashCode(this);
        capabilities = caps;
        requirements = reqs;
    }

    @Override
    public List<Capability> getCapabilities(String namespace) {
        if (namespace == null) {
            return capabilities.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
        }

        List<Capability> caps = capabilities.get(namespace);
        if (caps == null)
            return Collections.emptyList();
        return caps;
    }

    @Override
    public List<Requirement> getRequirements(String namespace) {
        if (namespace == null) {
            return requirements.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
        }

        List<Requirement> reqs = requirements.get(namespace);
        if (reqs == null)
            return Collections.emptyList();
        return reqs;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((capabilities == null) ? 0 : capabilities.hashCode());
        result = prime * result + ((requirements == null) ? 0 : requirements.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BundleResourceImpl other = (BundleResourceImpl) obj;
        if (capabilities == null) {
            if (other.capabilities != null)
                return false;
        } else if (!capabilities.equals(other.capabilities))
            return false;
        if (requirements == null) {
            if (other.requirements != null)
                return false;
        } else if (!requirements.equals(other.requirements))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "BundleResourceImpl [" + hint + "]";
    }
}
