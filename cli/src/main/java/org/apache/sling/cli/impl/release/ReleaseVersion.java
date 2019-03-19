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

public final class ReleaseVersion {
    
    public static ReleaseVersion fromRepositoryDescription(String repositoryDescription) {
        
        ReleaseVersion rel = new ReleaseVersion();
        
        rel.fullName = repositoryDescription
            .replaceAll(" RC[0-9]*$", ""); // 'release candidate' suffix
        rel.name = rel.fullName
            .replace("Apache Sling ", ""); // Apache Sling prefix
        rel.version = rel.fullName.substring(rel.fullName.lastIndexOf(' ') + 1);
        rel.component = rel.name.substring(0, rel.name.lastIndexOf(' '));
        
        return rel;
    }
    
    private String fullName;
    private String name;
    private String component;
    private String version;

    private ReleaseVersion() {
        
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
}
