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

public interface Constants {
    String SLING_RESOURCE_SUPER_TYPE_PROPERTY = "sling:resourceSuperType";
    String DYNAMIC_COMPONENTS_SERVICE_USER = "ddr-serviceuser";
    String REFERENCE_PROPERTY_NAME = "dc:ref";
    String JCR_PRIMARY_TYPE = "jcr:primaryType";
    String JCR_TITLE = "jcr:title";
    String SLING_FOLDER = "sling:Folder";
    String NT_UNSTRUCTURED = "nt:unstructured";
    String COMPONENT_GROUP = "componentGroup";
    String LABEL = "label";
    String REP_POLICY = "rep:policy";

    String DYNAMIC_COMPONENT_FOLDER_NAME = "dynamic";

    String SLASH = "/";
    String EQUALS = "=";
    String VERTICAL_LINE = "|";
    char OPENING_MULTIPLE = '{';
    char CLOSING_MULTIPLE = '}';
    String MULTI_SEPARATOR = ";";

    String DDR_NODE_TYPE = "sling:DDR";
    String DDR_TARGET_PROPERTY_NAME = "sling:ddrTarget";
    String DDR_REF_PROPERTY_NAME = "sling:ddrRef";
    String CONFIGURATION_ROOT_PATH = "/conf";
    String DDR_INDICATOR = "sling:ddrActive";
}