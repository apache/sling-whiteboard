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
package org.osgi.util.features;

import java.util.Map;

/**
 * Represents an OSGi Configuration in the Feature Model.
 * @ThreadSafe
 */
public interface Configuration {
    /**
     * Get the PID from the configuration.
     * @return The PID.
     */
    String getPid();

    /**
     * Get the Factory PID from the configuration, if any.
     * @return The Factory PID, or {@code null} if there is none.
     */
    String getFactoryPid();

    /**
     * Get the configuration key-value map.
     * @return The key-value map.
     */
    Map<String, Object> getValues();
}
