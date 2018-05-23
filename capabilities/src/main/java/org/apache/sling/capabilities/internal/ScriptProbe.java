/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Licensed to the Apache Software Foundation (ASF) under one
 ~ or more contributor license agreements.  See the NOTICE file
 ~ distributed with this work for additional information
 ~ regarding copyright ownership.  The ASF licenses this file
 ~ to you under the Apache License, Version 2.0 (the
 ~ "License"); you may not use this file except in compliance
 ~ with the License.  You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package org.apache.sling.capabilities.internal;

/** A Probe that executes a script to find out if 
 *  specific capabilities are present.
 *  The capability value is the output of the script.
 */
class ScriptProbe implements Probe {
    
    public static final String ID = "script";
    
    private final String name;
    private final String script;
    
    ScriptProbe(String name, String script) {
        this.name = name;
        this.script = script;
    }

    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getValue() {
        // TODO implement this
        if(script.contains("EX")) {
            throw new IllegalArgumentException("foobar");
        }
        return script;
    }
}
