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

/** Define a Property of a Resource: name, type, required etc */
public class ResourceProperty {
    
    private final String name;
    private final String label;
    private final String type;
    private final boolean required;
    
    public ResourceProperty(String name, String label, boolean required, String type) {
        this.name = name;
        this.label = label;
        this.required = required;
        this.type = type;
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append(":");
        sb.append("Name=").append(name);
        sb.append(",Label=").append(label);
        sb.append(",Type,=").append(type);
        sb.append(",Required=").append(required);
        return sb.toString();
    }
    
    /** @return the technical name of this Property */
    public String getName() {
        return name;
    }
    
    /** @return the label of this Property, for humans */
    public String getLabel() {
        return label;
    }
    
    /** @return true if this Property is required */
    public boolean isRequired() {
        return required;
    }
    
    /** @return the type of this Property: a String that points
     *  to the PropertyTypesRegistry */
    public String getType() {
        return type;
    }
}