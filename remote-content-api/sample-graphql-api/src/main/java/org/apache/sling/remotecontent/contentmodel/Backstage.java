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

package org.apache.sling.remotecontent.contentmodel;

import java.util.HashMap;

/** Backstage information is meant to provide hints and rules
 *  to authoring user interfaces and publishing services.
 */
public class Backstage extends HashMap<String, Object> {
    private Etc etc;
    
    public Backstage() {
        put("authoring", "authoring/editor information can come here, free-form");
        put("publishing", "publishing hints can come here, free-form");
    }

    public Object getEtc() {
        if(etc == null) {
            etc = new Etc();
            etc.put("info", "...additional information for this Document");
        }
        return etc;
    }
}