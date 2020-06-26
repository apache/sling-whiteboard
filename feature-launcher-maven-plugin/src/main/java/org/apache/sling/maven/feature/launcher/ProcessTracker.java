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
import java.util.concurrent.TimeUnit;

import org.codehaus.plexus.component.annotations.Component;

@Component(role = ProcessTracker.class)
public class ProcessTracker {

    static void stop(Process process) throws InterruptedException {
        process.destroy();
        boolean stopped = process.waitFor(30, TimeUnit.SECONDS);
        if ( !stopped )
            process.destroyForcibly();
    }
    
    private final Object sync = new Object();
    
    private boolean hookAdded = false;
    private final Map<String, Process> processes = new HashMap<>();
    
    public void startTracking(String launchId, Process process) {
        synchronized (sync) {
            if ( processes.containsKey(launchId) )
                throw new IllegalArgumentException("Launch id " + launchId + " already associated with a process");
            processes.put(launchId, process);
            if ( ! hookAdded ) {
                Runtime.getRuntime().addShutdownHook(new Thread("process-tracker-shutdown") {
                    @Override
                    public void run() {
                        for ( Map.Entry<String, Process> entry : processes.entrySet() ) {
                            System.err.println("Launch " + entry.getKey() + " was not shut down! Destroying forcibly from shutdown hook..");
                            process.destroyForcibly();
                        }
                    }
                    
                });
                hookAdded = true;
            }
        }
    }
    
    public void stop(String id) throws InterruptedException {
        Process process;
        synchronized (sync) {
            process = processes.remove(id);            
        }
        if ( process == null )
            return;
        
        stop(process);
    }
}
