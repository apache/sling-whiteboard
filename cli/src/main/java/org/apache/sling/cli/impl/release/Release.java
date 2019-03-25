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
package org.apache.sling.cli.impl.release;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Release {

    /*
        Group 1: Apache Sling and any trailing whitespace (optional)
        Group 2: Release component
        Group 3: Release version
        Group 4: RC status (optional)
     */
    private static final Pattern RELEASE_PATTERN = Pattern.compile("^\\h*(Apache Sling\\h*)?([()a-zA-Z0-9\\-.\\h]+)\\h([0-9\\-.]+)" +
            "\\h?(RC[0-9.]+)?\\h*$");
    
    public static Release fromString(String repositoryDescription) {
        
        Release rel = new Release();
        Matcher matcher = RELEASE_PATTERN.matcher(repositoryDescription);
        if (matcher.matches()) {
            rel.component = matcher.group(2).trim();
            rel.version = matcher.group(3);
            rel.name = rel.component + " " + rel.version;
            StringBuilder fullName = new StringBuilder();
            if (matcher.group(1) != null) {
                fullName.append(matcher.group(1).trim()).append(" ");
            }
            fullName.append(rel.name);
            rel.fullName = fullName.toString();


        }
        return rel;
    }
    
    private String fullName;
    private String name;
    private String component;
    private String version;

    private Release() {
        
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public String getName() {
        return name;
    }
    
    public String getVersion() {
        return version;
    }

    public String getComponent() {
        return component;
    }

    @Override
    public int hashCode() {
        return fullName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Release)) {
            return false;
        }
        Release other = (Release) obj;
        return Objects.equals(name, other.name);
    }

    @Override
    public String toString() {
        return fullName;
    }
}
