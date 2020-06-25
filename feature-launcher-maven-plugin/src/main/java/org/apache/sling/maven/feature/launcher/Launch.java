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

import java.util.regex.Pattern;

import org.apache.maven.model.Dependency;

public class Launch {
    
    private static final Pattern ID_PATTERN = Pattern.compile("[a-zA-Z0-9_\\-\\.]+");

    private String id;
    private Dependency feature;
    private LauncherArguments launcherArguments;
    private int startTimeoutSeconds = 30;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Dependency getFeature() {
        return feature;
    }

    public void setFeature(Dependency feature) {
        this.feature = feature;
    }

    public LauncherArguments getLauncherArguments() {
        return launcherArguments;
    }

    public void setLauncherArguments(LauncherArguments launcherArguments) {
        this.launcherArguments = launcherArguments;
    }
    
    public int getStartTimeoutSeconds() {
        return startTimeoutSeconds;
    }
    
    public void setStartTimeoutSeconds(int startTimeoutSeconds) {
        this.startTimeoutSeconds = startTimeoutSeconds;
    }

    public void validate() {
        if ( id == null || id.trim().isEmpty() ) 
            throw new IllegalArgumentException("Missing id");
        
        if ( !ID_PATTERN.matcher(id).matches() )
            throw new IllegalArgumentException("Invalid id '" + id + "'. Allowed characters are digits, numbers, '-','_' and '.'.");
        
        if ( startTimeoutSeconds < 0 )
            throwInvalid("startTimeout value '" + startTimeoutSeconds + "' is negative" );
        
        if ( feature == null )
            throwInvalid("required field 'feature' is missing");
        
        if ( ! "slingosgifeature".equals(feature.getType()) )
            throwInvalid("type must be 'slingosgifeature' but is '" + feature.getType()+"'");
    }
    
    private void throwInvalid(String reason) {
        throw new IllegalArgumentException("Invalid launch '" + id + "': " + reason);
    }
}
