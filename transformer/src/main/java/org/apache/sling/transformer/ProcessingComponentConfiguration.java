/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.transformer;

import org.apache.sling.api.resource.ValueMap;


/**
 * Configuration of a processing component.
 */
public interface ProcessingComponentConfiguration {

    /** Configuration for an optional component, only transformers support this option. */
    String CONFIGURATION_COMPONENT_OPTIONAL = "component-optional";

    /**
     * The name/type of the component.
     * @return A unique name for this component.
     */
    String getType();

    /**
     * Return the configuration for this component.
     * @return The configuration for this component or an empty map if there is none.
     */
    ValueMap getConfiguration();
}
