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
package org.apache.sling.ddr.api;

/**
 * The Declarative Dynamic Resource Manager is responsible
 * to create the Resource Provider for a given Source Folder
 */
public interface DeclarativeDynamicResourceManager {

    /**
     * Whenever a Declarative Dynamic Provider Folder is ready to be handled
     * this method is called to create the Declarative Dynamic Resources
     *
     * @param declarativeDynamicProviderPath Path to the Folder where the declarative dynamic resources are located in
     */
    void update(String declarativeDynamicProviderPath);

    /**
     * Add a given Path as Reference to listen for changes in references.
     * If a parent path of that ref is already register it is ignored.
     *
     * @param sourcePath Path of the Source of the Reference
     * @param targetPath Path of the Target of the Reference
     */
    void addReference(String sourcePath, String targetPath);
}
