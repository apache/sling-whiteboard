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
package org.apache.sling.feature.launcher.spi;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * This is the context for the launcher
 */
public interface LauncherRunContext {

    /**
     * Map of framework properties to be set when the framework is created.
     * @return The map with the framework properties.
     */
    Map<String, String> getFrameworkProperties();

    /**
     * Bundle map, key is the start level, value is a list of files.
     * @return The bundle map, might be empty
     */
    Map<Integer, List<File>> getBundleMap();

    /**
     * List of configurations.
     * The value in each is an object array with three values
     * <ol>
     *  <li>The PID
     *  <li>The factory PID or {@code null}
     *  <li>The dictionary with the properties
     * </ol>
     * We can't use a custom object due to class loading restrictions.
     * @return The list, might be empty
     */
    List<Object[]> getConfigurations();

    /**
     * List of installable artifacts.
     * @return The list of files. The list might be empty.
     */
    List<File> getInstallableArtifacts();
}
