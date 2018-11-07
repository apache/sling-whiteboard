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
package org.apache.sling.upgrade;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a configuration entry loaded from a Sling JAR. Contains the
 * configuration contents and installation requirements.
 */
public class ConfigEntry extends UpgradeEntry {

    private static final Logger log = LoggerFactory.getLogger(ConfigEntry.class);

    private final String pid;

    public ConfigEntry(JarEntry entry, InputStream is) throws IOException {
        super(entry, is);
        pid = entry.getName().replace("resources/config/", "");
        log.debug("Reading config {}", pid);

    }

    @Override
    public int compareTo(UpgradeEntry o) {
        if (o instanceof ConfigEntry) {
            ConfigEntry co = (ConfigEntry) o;
            return pid.compareTo(co.getPid());
        } else {
            return getClass().getName().compareTo(o.getClass().getName());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ConfigEntry other = (ConfigEntry) obj;
        if (pid == null) {
            if (other.pid != null) {
                return false;
            }
        } else if (!pid.equals(other.pid)) {
            return false;
        }
        return true;
    }

    /**
     * @return the pid
     */
    public String getPid() {
        return pid;
    }

    public String getPath() {
        String path = StringUtils.substringBeforeLast(pid, ".");
        path = path.replace('.', File.separatorChar).replace("-", "%007e");
        return path + ".config";
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((pid == null) ? 0 : pid.hashCode());
        return result;
    }

}
