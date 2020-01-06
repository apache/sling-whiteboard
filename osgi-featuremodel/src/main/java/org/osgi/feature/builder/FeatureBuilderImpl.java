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
package org.osgi.feature.builder;

import org.osgi.feature.ArtifactID;
import org.osgi.feature.Bundle;
import org.osgi.feature.Configuration;
import org.osgi.feature.Extension;
import org.osgi.feature.Feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FeatureBuilderImpl {
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

    public FeatureBuilderImpl(ArtifactID id) {
        this.id = id;
    }

    public FeatureBuilderImpl setTitle(String title) {
        this.title = title;
        return this;
    }

    public FeatureBuilderImpl setVendor(String vendor) {
        this.vendor = vendor;
        return this;
    }

    public FeatureBuilderImpl setLicense(String license) {
        this.license = license;
        return this;
    }

    public FeatureBuilderImpl setLocation(String location) {
        this.location = location;
        return this;
    }

    public FeatureBuilderImpl setComplete(boolean complete) {
        this.complete = complete;
        return this;
    }

    public FeatureBuilderImpl setFinal(boolean isFinal) {
        this.isFinal = isFinal;
        return this;
    }

    public FeatureBuilderImpl setDescription(String description) {
        this.description = description;
        return this;
    }

    public FeatureBuilderImpl addBundles(Bundle ... bundles) {
        this.bundles.addAll(Arrays.asList(bundles));
        return this;
    }

    public FeatureBuilderImpl addConfigurations(Configuration ... configs) {
        for (Configuration cfg : configs) {
            this.configurations.put(cfg.getPid(), cfg);
        }
        return this;
    }

    public FeatureBuilderImpl addExtensions(Extension ... extensions) {
        for (Extension ex : extensions) {
            this.extensions.put(ex.getName(), ex);
        }
        return this;
    }

    public FeatureBuilderImpl addVariable(String key, String value) {
        this.variables.put(key, value);
        return this;
    }

    public FeatureBuilderImpl addVariables(Map<String,String> variables) {
        this.variables.putAll(variables);
        return this;
    }

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
