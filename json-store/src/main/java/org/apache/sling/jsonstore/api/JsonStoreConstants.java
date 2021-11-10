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

 package org.apache.sling.jsonstore.api;

public class JsonStoreConstants {
    public static final String STORE_ROOT_PATH = "/content/sites";
    public static final String JSON_BLOB_RESOURCE_TYPE = "sling/jsonstore/json";
    public static final String JSON_FOLDER_RESOURCE_TYPE = "sling/jsonstore/folder";
    public static final String JSON_PROP_NAME = "json";

    public static final String SCHEMA_DATA_TYPE = "schema";
    public static final String ELEMENTS_DATA_TYPE = "elements";
    public static final String CONTENT_DATA_TYPE = "content";

    public static final String JSON_SCHEMA_FIELD = "$schema";
    
    private JsonStoreConstants() {}
}
