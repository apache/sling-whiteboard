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
package org.apache.sling.repositorymaintainance.internal;

import javax.management.openmbean.CompositeData;

import org.apache.felix.hc.api.FormattingResultLog;
import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.jackrabbit.oak.api.jmx.RepositoryManagementMBean;
import org.apache.sling.repositorymaintainance.RepositoryManagementUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;

@Component(service = HealthCheck.class, property = { HealthCheck.TAGS + "=oak", HealthCheck.TAGS + "=system-resource",
        HealthCheck.NAME + "=Apache Sling JCR Repository Maintaince" }, immediate = true)
public class RepositoryMaintainceHealthCheck implements HealthCheck {

    private DataStoreCleanupScheduler dataStoreCleanupScheduler;

    private RevisionCleanupScheduler revisionCleanupScheduler;

    private RepositoryManagementMBean repositoryManagementMBean;

    private VersionCleanupMBean versionCleanup;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY, service = Runnable.class, target = "(component.name=org.apache.sling.repositorymaintainance.internal.DataStoreCleanupScheduler)")
    public void setDataStoreCleanupScheduler(Runnable dataStoreCleanupScheduler) {
        this.dataStoreCleanupScheduler = (DataStoreCleanupScheduler) dataStoreCleanupScheduler;
    }

    @Reference
    public void setRepositoryManagementMBean(RepositoryManagementMBean repositoryManagementMBean) {
        this.repositoryManagementMBean = repositoryManagementMBean;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY, service = Runnable.class, target = "(component.name=org.apache.sling.repositorymaintainance.internal.RevisionCleanupScheduler)")
    public void setRevisionCleanupScheduler(Runnable revisionCleanupScheduler) {
        this.revisionCleanupScheduler = (RevisionCleanupScheduler) revisionCleanupScheduler;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
    public void setVersionCleanup(VersionCleanupMBean versionCleanup) {
        this.versionCleanup = versionCleanup;
    }

    @Override
    public Result execute() {
        FormattingResultLog log = new FormattingResultLog();

        if (dataStoreCleanupScheduler != null) {
            log.debug("DataStoreCleanup Schedule: {}", dataStoreCleanupScheduler.getSchedulerExpression());
        } else {
            log.warn("DataStoreCleanup not registered");
        }
        CompositeData status = repositoryManagementMBean.getDataStoreGCStatus();
        if (RepositoryManagementUtil.isValid(status)) {
            log.debug("DataStoreCleanup Last Status: {}", RepositoryManagementUtil.getStatusCode(status).name());
            log.debug("DataStoreCleanup Last Message: {}", RepositoryManagementUtil.getMessage(status));
        } else {
            log.critical("DataStoreCleanup Last Status: {}", RepositoryManagementUtil.getStatusCode(status).name());
            log.critical("DataStoreCleanup Last Message: {}", RepositoryManagementUtil.getMessage(status));
        }

        if (revisionCleanupScheduler != null) {
            log.debug("RevisionCleanup Schedule: {}", revisionCleanupScheduler.getSchedulerExpression());
        } else {
            log.warn("RevisionCleanup not registered");
        }
        status = repositoryManagementMBean.getRevisionGCStatus();
        if (RepositoryManagementUtil.isValid(status)) {
            log.debug("RevisionCleanup Last Status: {}", RepositoryManagementUtil.getStatusCode(status).name());
            log.debug("RevisionCleanup Last Message: {}", RepositoryManagementUtil.getMessage(status));
        } else {
            log.critical("RevisionCleanup Last Status: {}", RepositoryManagementUtil.getStatusCode(status).name());
            log.critical("RevisionCleanup Last Message: {}", RepositoryManagementUtil.getMessage(status));
        }

        if (versionCleanup != null) {
            if (versionCleanup.isFailed()) {
                log.critical("VersionCleanup Status: FAILED");
                log.critical("VersionCleanup Message: {}", versionCleanup.getLastMessage());
            } else {
                log.debug("VersionCleanup Status: SUCCEEDED");
            }
            log.debug("VersionCleanup Last Cleaned: {}", versionCleanup.getLastCleanedVersionsCount());
        } else {
            log.warn("VersionCleanup not registered");
        }

        return new Result(log);
    }

}