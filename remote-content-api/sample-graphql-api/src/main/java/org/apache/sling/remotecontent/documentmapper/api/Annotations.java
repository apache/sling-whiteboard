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

package org.apache.sling.remotecontent.documentmapper.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.osgi.annotation.versioning.ProviderType;

/** Document mapping annotations for a given Resource Type */
@ProviderType
public class Annotations {
    public final String resourceType;
    private boolean navigable;
    private boolean visitContent;
    private boolean documentRoot;
    private String childSubstitutePath;
    private Pattern visitContentResourceNamePattern;
    private Pattern includePropertyPattern;
    private Pattern excludePropertyPattern;
    private List<String> resolveByPathProperties;
    private List<String> excludeNodeNames;

    private Annotations(String resourceType) {
        this.resourceType = resourceType;
    }

    @Override
    public String toString() {
        return String.format(
            "RT=%s N=%b VC=%b DR=%b VCRN=%s IP=%s EP=%s DR=%s ENN=%s",
            resourceType,
            navigable,
            visitContent,
            documentRoot,
            visitContentResourceNamePattern,
            includePropertyPattern,
            excludePropertyPattern,
            resolveByPathProperties,
            excludeNodeNames
        );
    }

    // TODO equals + hashcode

    public String getResourceType() {
        return resourceType;
    }
    
    public boolean isNavigable() {
        return navigable;
    }
    public boolean isDocumentRoot() {
        return documentRoot;
    }

    public boolean visitContent() {
        return visitContent;
    }

    public boolean visitChildResource(String resourceName) {
        return visitContentResourceNamePattern == null ? true : visitContentResourceNamePattern.matcher(resourceName).matches();
    }

    // TODO should accept a parent Annotations - other methods as well?
    public boolean includeProperty(String name) {
        // include has priority over exclude
        boolean result = includePropertyPattern == null ? true : includePropertyPattern.matcher(name).matches();
        if(!result) {
            result = excludePropertyPattern == null ? true : !excludePropertyPattern.matcher(name).matches();
        }
        return result;
    }

    public Collection<String> resolveByPathPropertyNames() {
        if(resolveByPathProperties != null) {
            return resolveByPathProperties;
        } else {
            return Collections.emptyList();
        }
    }

    public Collection<String> excludeNodeNames() {
        if(excludeNodeNames != null) {
            return excludeNodeNames;
        } else {
            return Collections.emptyList();
        }
    }

    public String childSubstitutePath() {
        return childSubstitutePath;
    }

    public static Builder forResourceType(String resourceType) {
        return new Builder(resourceType);
    }

    public static class Builder {
        private final Annotations target;
        Builder(String resourceType) {
            target = new Annotations(resourceType);
        }

        public Builder withNavigable(boolean b) {
            target.navigable = b;
            return this;
        }

        public Builder withDocumentRoot(boolean b) {
            target.documentRoot = b;
            return this;
        }

        public Builder withVisitContent(boolean b) {
            target.visitContent = b;
            return this;
        }

        public Builder withVisitContentChildResourceNamePattern(String p) {
            target.visitContentResourceNamePattern = Pattern.compile(p);
            return this;
        }

        public Builder withIncludePropertyPattern(String p) {
            target.includePropertyPattern = Pattern.compile(p);
            return this;
        }

        public Builder withExcludePropertyPattern(String p) {
            target.excludePropertyPattern = Pattern.compile(p);
            return this;
        }

        public Builder withResolveByPathPropertyNames(String ... names) {
            if(target.resolveByPathProperties == null) {
                target.resolveByPathProperties = new ArrayList<String>();
            }
            for(String name : names) {
                target.resolveByPathProperties.add(name);
            }
            return this;
        }

        public Builder withExcludeNodeNames(String ... names) {
            if(target.excludeNodeNames == null) {
                target.excludeNodeNames = new ArrayList<String>();
            }
            for(String name : names) {
                target.excludeNodeNames.add(name);
            }
            return this;
        }

        public Builder withChildSubstituePath(String path) {
            target.childSubstitutePath = path;
            return this;
        }

        public Annotations build() {
            return target;
        }
    }
}