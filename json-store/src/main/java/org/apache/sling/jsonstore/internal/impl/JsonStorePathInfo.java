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

package org.apache.sling.jsonstore.internal.impl;

import static org.apache.sling.jsonstore.internal.api.JsonStoreConstants.STORE_ROOT_PATH;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class JsonStorePathInfo {
    public final String site;
    public final String dataType;
    public final String dataPath;

    private final static Pattern PARSE_REGEXP = Pattern.compile(STORE_ROOT_PATH + "/([^/]+)/([^/]+)/(.*)");

    JsonStorePathInfo(String path) {
        final Matcher m = PARSE_REGEXP.matcher(path);
        if(!m.matches()) {
            throw new IllegalArgumentException("Path does not match " + PARSE_REGEXP + ": " + path);
        }
        site = m.group(1);
        dataType = m.group(2);
        dataPath = m.group(3);
    }

    @Override
    public String toString() {
        return String.format("%s: site=%s, dataType=%s, dataPath=%s", getClass().getSimpleName(), site, dataType, dataPath);
    }
}