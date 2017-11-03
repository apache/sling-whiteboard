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
package org.apache.sling.feature;

import java.util.ArrayList;

/**
 * A container for configurations.
 */
public class Configurations extends ArrayList<Configuration> {

    private static final long serialVersionUID = -7243822886707856704L;

    /**
     * Get the configuration
     * @param pid The pid of the configuration
     * @return The configuration or {@code null}
     */
    public Configuration getConfiguration(final String pid) {
        for(final Configuration cfg : this) {
            if ( !cfg.isFactoryConfiguration() && pid.equals(cfg.getPid())) {
                return cfg;
            }
        }
        return null;
    }

    /**
     * Get the factory configuration
     * @param factoryPid The factoryPid of the configuration
     * @param name The name of the configuration
     * @return The factory configuration or {@code null}
     */
    public Configuration getFactoryConfiguration(final String factoryPid, final String name) {
        for(final Configuration cfg : this) {
            if ( cfg.isFactoryConfiguration()
                    && factoryPid.equals(cfg.getFactoryPid())
                    && name.equals(cfg.getName())) {
                return cfg;
            }
        }
        return null;
    }
}
