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
package org.apache.sling.maven.feature.launcher;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// TODO - refactor to a Mojo Component
public class Processes {

    private static final Map<String, Process> processes = new HashMap<>();
    
    public static void addProcess(String id, Process process) {
        // TODO - check for duplicates
        processes.put(id, process);
    }
    
    public static Optional<Process> get(String id) {
        return Optional.ofNullable(processes.get(id));
    }
}