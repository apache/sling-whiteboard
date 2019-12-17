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
package org.osgi.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Thread Safe
// Or do we use an interface?
public class Feature extends Artifact {
    private final String title;
    private final String description;
    private final String vendor;
    private final String license;
    private final String location;
    private final boolean complete;
    private final boolean isFinal;

    private final List<Bundle> bundles;
    private final List<Configuration> configurations;
    private final Map<String, String> variables;

    private Feature(ArtifactID id, String aTitle, String desc, String vnd, String lic, String loc,
            boolean comp, boolean fin, List<Bundle> bs, List<Configuration> cs, Map<String,String> vars) {
        super(id);

        title = aTitle;
        description = desc;
        vendor = vnd;
        license = lic;
        location = loc;
        complete = comp;
        isFinal = fin;

        bundles = Collections.unmodifiableList(bs);
        configurations = Collections.unmodifiableList(cs);
        variables = Collections.unmodifiableMap(vars);

        // add prototype
        // add requirements
        // add capabilities
        // add framework properties
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getVendor() {
        return vendor;
    }

    public String getLicense() {
        return license;
    }

    public String getLocation() {
        return location;
    }

    public boolean isComplete() {
        return complete;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public List<Bundle> getBundles() {
        return bundles;
    }

    public List<Configuration> getConfigurations() {
        return configurations;
    }

    public Map<String, String> getVariables() {
        return variables;
    }

    @Override
    public String toString() {
        return "Feature [title=" + title + ", description=" + description + ", vendor=" + vendor + ", license=" + license
                + ", location=" + location + ", complete=" + complete + ", isFinal=" + isFinal + ", bundles=" + bundles
                + ", configurations=" + configurations + ", variables=" + variables + ", getID()=" + getID() + "]";
    }

    // Not Thread Safe
    public static class Builder {
        private final ArtifactID id;

        private String title;
        private String description;
        private String vendor;
        private String license;
        private String location;
        private boolean complete;
        private boolean isFinal;

        private final List<Bundle> bundles = new ArrayList<>();
        private final List<Configuration> configurations = new ArrayList<>();
        private final Map<String,String> variables = new HashMap<>();

        public Builder(ArtifactID id) {
            this.id = id;
        }

        public Builder(String groupId, String artifactId, String version) {
            this(new ArtifactID(groupId, artifactId, version, null, null));
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setVendor(String vendor) {
            this.vendor = vendor;
            return this;
        }

        public Builder setLicense(String license) {
            this.license = license;
            return this;
        }

        public Builder setLocation(String location) {
            this.location = location;
            return this;
        }

        public Builder setComplete(boolean complete) {
            this.complete = complete;
            return this;
        }

        public Builder setFinal(boolean isFinal) {
            this.isFinal = isFinal;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder addBundles(Bundle ... bundles) {
            this.bundles.addAll(Arrays.asList(bundles));
            return this;
        }

        public Builder addConfigurations(Configuration ... configs) {
            this.configurations.addAll(Arrays.asList(configs));
            return this;
        }

        public Builder addVariable(String key, String value) {
            this.variables.put(key, value);
            return this;
        }

        public Builder addVariables(Map<String, String> variables) {
            this.variables.putAll(variables);
            return this;
        }

        public Feature build() {
            return new Feature(id, title,
                    description, vendor, license, location, complete, isFinal,
                    bundles, configurations, variables);
        }
    }
}
