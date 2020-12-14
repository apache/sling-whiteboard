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
package org.apache.sling.jcr.repositorymaintenance.internal;

import org.apache.jackrabbit.oak.api.jmx.RepositoryManagementMBean;
import org.apache.sling.jcr.repositorymaintenance.RepositoryManagementUtil;
import org.apache.sling.jcr.repositorymaintenance.RevisionCleanupConfig;
import org.apache.sling.jcr.repositorymaintenance.RunnableJob;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for running the Jackrabbit OAK Segment Store cleanup on a schedule.
 */
@Component(service = { Runnable.class }, configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true)
@Designate(ocd = RevisionCleanupConfig.class)
public class RevisionCleanupScheduler implements RunnableJob {

    private static final Logger log = LoggerFactory.getLogger(RevisionCleanupScheduler.class);

    private final RepositoryManagementMBean repositoryManager;

    private final String schedulerExpression;

    @Activate
    public RevisionCleanupScheduler(final RevisionCleanupConfig config,
            @Reference final RepositoryManagementMBean repositoryManager) {
        this.repositoryManager = repositoryManager;
        this.schedulerExpression = config.scheduler_expression();
    }

    public void run() {
        if (!RepositoryManagementUtil.isRunning(repositoryManager.getRevisionGCStatus())) {
            log.info("Starting Revision Garbage Collection");
            repositoryManager.startRevisionGC();
        } else {
            log.warn("Revision Garbage Collection already running!");
        }
    }

    /**
     * @return the schedulerExpression
     */
    public String getSchedulerExpression() {
        return schedulerExpression;
    }
}
