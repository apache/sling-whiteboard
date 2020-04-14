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
package org.osgi.util.features.impl;

import org.osgi.util.features.ArtifactID;
import org.osgi.util.features.Bundle;
import org.osgi.util.features.Configuration;
import org.osgi.util.features.Extension;
import org.osgi.util.features.Feature;
import org.osgi.util.features.FeatureBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class FeatureBuilderImpl implements FeatureBuilder {
    private final ArtifactID id;

    private String title;
    private String description;
    private String vendor;
    private String license;
    private String location;
    private boolean complete;
    private boolean isFinal;

    private final List<Bundle> bundles = new ArrayList<>();
    private final Map<String,Configuration> configurations = new HashMap<>();
    private final Map<String,Extension> extensions = new HashMap<>();
    private final Map<String,String> variables = new HashMap<>();

    FeatureBuilderImpl(ArtifactID id) {
        this.id = id;
    }

    @Override
    public FeatureBuilder setTitle(String title) {
        this.title = title;
        return this;
    }

    @Override
    public FeatureBuilder setVendor(String vendor) {
        this.vendor = vendor;
        return this;
    }

    @Override
    public FeatureBuilder setLicense(String license) {
        this.license = license;
        return this;
    }

    @Override
    public FeatureBuilder setLocation(String location) {
        this.location = location;
        return this;
    }

    @Override
    public FeatureBuilder setComplete(boolean complete) {
        this.complete = complete;
        return this;
    }

    @Override
    public FeatureBuilder setFinal(boolean isFinal) {
        this.isFinal = isFinal;
        return this;
    }

    @Override
    public FeatureBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public FeatureBuilder addBundles(Bundle ... bundles) {
        this.bundles.addAll(Arrays.asList(bundles));
        return this;
    }

    @Override
    public FeatureBuilder addConfigurations(Configuration ... configs) {
        for (Configuration cfg : configs) {
            this.configurations.put(cfg.getPid(), cfg);
        }
        return this;
    }

    @Override
    public FeatureBuilder addExtensions(Extension ... extensions) {
        for (Extension ex : extensions) {
            this.extensions.put(ex.getName(), ex);
        }
        return this;
    }

    @Override
    public FeatureBuilder addVariable(String key, String value) {
        this.variables.put(key, value);
        return this;
    }

    @Override
    public FeatureBuilder addVariables(Map<String,String> variables) {
        this.variables.putAll(variables);
        return this;
    }

    @Override
    public Feature build() {
        return new FeatureImpl(id, title,
                description, vendor, license, location, complete, isFinal,
                bundles, configurations, extensions, variables);
    }

    private static class FeatureImpl extends ArtifactImpl implements Feature {
        private final String title;
        private final String description;
        private final String vendor;
        private final String license;
        private final String location;
        private final boolean complete;
        private final boolean isFinal;

        private final List<Bundle> bundles;
        private final Map<String,Configuration> configurations;
        private final Map<String,Extension> extensions;
        private final Map<String,String> variables;

        private FeatureImpl(ArtifactID id, String aTitle, String desc, String vnd, String lic, String loc,
                boolean comp, boolean fin, List<Bundle> bs, Map<String,Configuration> cs,
                Map<String,Extension> es, Map<String,String> vars) {
            super(id);

            title = aTitle;
            description = desc;
            vendor = vnd;
            license = lic;
            location = loc;
            complete = comp;
            isFinal = fin;

            bundles = Collections.unmodifiableList(bs);
            configurations = Collections.unmodifiableMap(cs);
            extensions = Collections.unmodifiableMap(es);
            variables = Collections.unmodifiableMap(vars);
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public String getVendor() {
            return vendor;
        }

        @Override
        public String getLicense() {
            return license;
        }

        @Override
        public String getLocation() {
            return location;
        }

        @Override
        public boolean isComplete() {
            return complete;
        }

        @Override
        public boolean isFinal() {
            return isFinal;
        }

        @Override
        public List<Bundle> getBundles() {
            return bundles;
        }

        @Override
        public Map<String,Configuration> getConfigurations() {
            return configurations;
        }

        @Override
        public Map<String,Extension> getExtensions() {
            return extensions;
        }

        @Override
        public Map<String,String> getVariables() {
            return variables;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + Objects.hash(bundles, complete, configurations, description, isFinal, license, location,
                    title, variables, vendor);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!super.equals(obj))
                return false;
            if (!(obj instanceof FeatureImpl))
                return false;
            FeatureImpl other = (FeatureImpl) obj;
            return Objects.equals(bundles, other.bundles) && complete == other.complete
                    && Objects.equals(configurations, other.configurations) && Objects.equals(description, other.description)
                    && isFinal == other.isFinal && Objects.equals(license, other.license)
                    && Objects.equals(location, other.location) && Objects.equals(title, other.title)
                    && Objects.equals(variables, other.variables) && Objects.equals(vendor, other.vendor);
        }

        @Override
        public String toString() {
            return "FeatureImpl [getID()=" + getID() + "]";
        }
    }
}
