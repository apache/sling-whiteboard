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

import org.osgi.framework.Version;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Thread Safe
public class Feature {
    private final String groupId;
    private final String artifactId;
    private final Version version;
    private final String title;
    private final String description;
    private final String vendor;
    private final String license;
    private final String location;
    private final boolean complete;
    private final boolean isFinal;

    private final List<Bundle> bundles;
    private final Map<String, String> variables;

    private Feature(String gid, String aid, Version ver, String aTitle, String desc, String vnd, String lic, String loc,
            boolean comp, boolean fin, List<Bundle> bs, Map<String,String> vars) {
        groupId = gid;
        artifactId = aid;
        version = ver;
        title = aTitle;
        description = desc;
        vendor = vnd;
        license = lic;
        location = loc;
        complete = comp;
        isFinal = fin;

        bundles = Collections.unmodifiableList(bs);
        variables = Collections.unmodifiableMap(vars);

        // add prototype
        // add requirements
        // add capabilities
        // add bundles
        // add configurations
        // add framework properties
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public Version getVersion() {
        return version;
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

    public Map<String, String> getVariables() {
        return variables;
    }

    // Not Thread Safe
    public class Builder {
        private final String groupId;
        private final String artifactId;
        private final Version version;

        private String title;
        private String description;
        private String vendor;
        private String license;
        private String location;
        private boolean complete;
        private boolean isFinal;

        private final List<Bundle> bundles = new ArrayList<>();
        private final Map<String,String> variables = new HashMap<>();

        public Builder(String groupId, String artifactId, Version version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
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

        public Builder addVariables(Map<String, String> variables) {
            this.variables.putAll(variables);
            return this;
        }

        public Feature build() {
            return new Feature(groupId, artifactId, version, title,
                    description, vendor, license, location, complete, isFinal,
                    bundles, variables);
        }
    }
}
