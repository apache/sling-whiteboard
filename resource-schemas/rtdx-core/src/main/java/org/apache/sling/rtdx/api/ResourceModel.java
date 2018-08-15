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

package org.apache.sling.rtdx.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/** Define the Model of a Resource: its description,
 *  properties, links to other Resource types etc.
 *  Objects of this class are immutable.
 */
public class ResourceModel {
    
    private final Builder b;
    
    public static class Builder {
        
        String name;
        String description;
        List<String> postHereResourceTypes = new ArrayList<>();
        List<ResourceProperty> properties = new ArrayList<>(); 
        
        public Builder withName(String n) {
            name = n;
            return this;
        }
        
        public Builder withDescription(String d) {
            description = d;
            return this;
        }
        
        public Builder withPostHereResourceType(String p) {
            postHereResourceTypes.add(p);
            return this;
        }
        
        public Builder withProperty(ResourceProperty p) {
            properties.add(p);
            return this;
        }
        
        public ResourceModel build() {
            return new ResourceModel(this);
        }
    }
    
    private ResourceModel(Builder b) {
        this.b = b;
    }
    
    public static Builder BUILDER() {
        return new Builder();
    }
    
    @Override
    public String toString() {
       final StringBuilder sb = new StringBuilder();
       sb.append(getClass().getSimpleName()).append(":");
       sb.append(b.name).append(":").append(b.description);
       sb.append("\nproperties=").append(b.properties);
       sb.append("\npost-here=").append(b.postHereResourceTypes);
       return sb.toString();
    }
    
    /** @return the name of this model */
    public String getName() {
        return b.name;
    }
    
    /** @return a textual description of this model */
    public String getDescription() {
        return b.description;
    }
    
    /** @return an Iterator over resource types that
     *  can be POSTed to this Resource.
     */
    public Collection<String> getPostHereResourceTypes() {
        return Collections.unmodifiableCollection(b.postHereResourceTypes);
    }
    
    /** @return an Iterator over the Resource Properties of this model */
    public Collection<ResourceProperty> getProperties() {
        return Collections.unmodifiableCollection(b.properties);
    }
}