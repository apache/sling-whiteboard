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
package org.apache.sling.jcr.maintenance.internal;

import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.sling.jcr.maintenance.VersionCleanupPathConfig;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = VersionCleanupPath.class, immediate = true)
@Designate(ocd = VersionCleanupPathConfig.class, factory = true)
public class VersionCleanupPath implements Comparable<VersionCleanupPath> {

    private static final Logger log = LoggerFactory.getLogger(VersionCleanupPath.class);

    private final boolean keepVersions;
    private final int limit;
    private final String path;

    @Activate
    public VersionCleanupPath(VersionCleanupPathConfig config) {
        this.keepVersions = config.keepVersions();
        this.limit = config.limit();
        this.path = config.path();
    }

    @Override
    public int compareTo(VersionCleanupPath o) {
        return path.compareTo(o.path) * -1;
    }

    /**
     * @return the keepVersions
     */
    public boolean isKeepVersions() {
        return keepVersions;
    }

    /**
     * @return the limit
     */
    public int getLimit() {
        return limit;
    }

    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }

    public static final VersionCleanupPath getMatchingConfiguration(
            final List<VersionCleanupPath> versionCleanupConfigs, final String path) throws RepositoryException {
        log.trace("Evaluating configurations {} for path {}", versionCleanupConfigs, path);
        return versionCleanupConfigs.stream().filter(c -> path.startsWith(c.getPath())).findFirst()
                .orElseThrow(() -> new RepositoryException("Failed to find version cleanup configuration for " + path));
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */

    @Override
    public String toString() {
        return "VersionCleanupPath [keepVersions=" + keepVersions + ", limit=" + limit + ", path=" + path + "]";
    }

}
