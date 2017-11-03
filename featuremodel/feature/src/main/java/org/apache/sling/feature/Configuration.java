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

import java.util.Dictionary;
import java.util.Hashtable;


/**
 * A configuration has either
 * <ul>
 *   <li>a pid
 *   <li>or a factory pid and a name
 * </ul>
 * and properties.
 */
public class Configuration
    implements Comparable<Configuration> {

    public static final String PROP_ARTIFACT = "service.bundleLocation";

    /** The pid or name for factory pids. */
    private final String pid;

    /** The factory pid. */
    private final String factoryPid;

    /** The properties. */
    private final Dictionary<String, Object> properties = new Hashtable<>();

    /**
     * Create a new configuration
     * @param pid The pid
     * @throws IllegalArgumentException If pid is {@code null}
     */
    public Configuration(final String pid) {
        if ( pid == null ) {
            throw new IllegalArgumentException("pid must not be null");
        }
        this.pid = pid;
        this.factoryPid = null;
    }

    /**
     * Create a new factor configuration
     * @param factoryPid The factory pid
     * @param name The name of the factory pid
     * @throws IllegalArgumentException If factoryPid or name is {@code null}
     */
    public Configuration(final String factoryPid, final String name) {
        if ( factoryPid == null || name == null ) {
            throw new IllegalArgumentException("factoryPid and/or name must not be null");
        }
        this.pid = name;
        this.factoryPid = factoryPid;
    }

    private int compareString(final String a, final String b) {
        if ( a == null ) {
            if ( b == null ) {
                return 0;
            }
            return -1;
        }
        if ( b == null ) {
            return 1;
        }
        return a.compareTo(b);
    }

    @Override
    public int compareTo(final Configuration o) {
        int result = compareString(this.factoryPid, o.factoryPid);
        if ( result == 0 ) {
            result = compareString(this.pid, o.pid);
        }
        return result;
    }


    /**
     * Get the pid.
     * If this is a factory configuration, it returns {@code null}
     * @return The pid or {@code null}
     */
    public String getPid() {
        if ( this.isFactoryConfiguration() ) {
            return null;
        }
        return this.pid;
    }

    /**
     * Return the factory pid
     * @return The factory pid or {@code null}.
     */
    public String getFactoryPid() {
        return this.factoryPid;
    }

    /**
     * Return the name for a factory configuration.
     * @return The name or {@code null}.
     */
    public String getName() {
        if ( this.isFactoryConfiguration() ) {
            return this.pid;
        }
        return null;
    }

    /**
     * Check whether this is a factory configuration
     * @return {@code true} if it is a factory configuration
     */
    public boolean isFactoryConfiguration() {
        return this.factoryPid != null;
    }

    /**
     * Get all properties of the configuration.
     * @return The properties
     */
    public Dictionary<String, Object> getProperties() {
        return this.properties;
    }

    @Override
    public String toString() {
        if ( this.isFactoryConfiguration() ) {
            return "Factory Configuration [factoryPid=" + factoryPid
                    + ", name=" + pid
                    + ", properties=" + properties
                    + "]";
        }
        return "Configuration [pid=" + pid
                + ", properties=" + properties
                + "]";
    }
}
