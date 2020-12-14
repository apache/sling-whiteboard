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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.management.openmbean.CompositeData;

import org.apache.felix.hc.api.Result;
import org.apache.jackrabbit.oak.api.jmx.RepositoryManagementMBean;
import org.apache.sling.jcr.repositorymaintenance.CompositeDataMock;
import org.apache.sling.jcr.repositorymaintenance.internal.DataStoreCleanupScheduler;
import org.apache.sling.jcr.repositorymaintenance.internal.RepositoryMaintenanceHealthCheck;
import org.apache.sling.jcr.repositorymaintenance.internal.RevisionCleanupScheduler;
import org.apache.sling.jcr.repositorymaintenance.internal.VersionCleanupMBean;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class RepositoryMaintenanceHealthCheckTest {

    private CompositeData successCompositeData;
    private CompositeData failedCompositeData;

    @Before
    public void init() {
        successCompositeData = CompositeDataMock.init()
                .put("code", RepositoryManagementMBean.StatusCode.SUCCEEDED.ordinal()).build();
        failedCompositeData = CompositeDataMock.init()
                .put("code", RepositoryManagementMBean.StatusCode.FAILED.ordinal()).build();
    }

    @Test
    public void testNothingRegistered() {
        RepositoryMaintenanceHealthCheck repositoryHealthCheck = new RepositoryMaintenanceHealthCheck();

        RepositoryManagementMBean repositoryManagementMBean = Mockito.mock(RepositoryManagementMBean.class);
        Mockito.when(repositoryManagementMBean.getRevisionGCStatus()).thenReturn(successCompositeData);
        Mockito.when(repositoryManagementMBean.getDataStoreGCStatus()).thenReturn(successCompositeData);
        repositoryHealthCheck.setRepositoryManagementMBean(repositoryManagementMBean);

        Result result = repositoryHealthCheck.execute();
        assertFalse(result.isOk());
    }

    @Test
    public void testAllSuccessful() {
        RepositoryMaintenanceHealthCheck repositoryHealthCheck = new RepositoryMaintenanceHealthCheck();

        DataStoreCleanupScheduler dataStoreCleanupScheduler = Mockito.mock(DataStoreCleanupScheduler.class);
        repositoryHealthCheck.setDataStoreCleanupScheduler(dataStoreCleanupScheduler);

        RepositoryManagementMBean repositoryManagementMBean = Mockito.mock(RepositoryManagementMBean.class);
        Mockito.when(repositoryManagementMBean.getRevisionGCStatus()).thenReturn(successCompositeData);
        Mockito.when(repositoryManagementMBean.getDataStoreGCStatus()).thenReturn(successCompositeData);

        repositoryHealthCheck.setRepositoryManagementMBean(repositoryManagementMBean);

        RevisionCleanupScheduler revisionCleanupScheduler = Mockito.mock(RevisionCleanupScheduler.class);
        repositoryHealthCheck.setRevisionCleanupScheduler(revisionCleanupScheduler);

        VersionCleanupMBean versionCleanupMBean = Mockito.mock(VersionCleanupMBean.class);
        Mockito.when(versionCleanupMBean.isFailed()).thenReturn(false);
        repositoryHealthCheck.setVersionCleanup(versionCleanupMBean);

        Result result = repositoryHealthCheck.execute();
        assertTrue(result.isOk());
    }

    @Test
    public void testDataStoreFailure() {
        RepositoryMaintenanceHealthCheck repositoryHealthCheck = new RepositoryMaintenanceHealthCheck();

        RepositoryManagementMBean repositoryManagementMBean = Mockito.mock(RepositoryManagementMBean.class);
        Mockito.when(repositoryManagementMBean.getRevisionGCStatus()).thenReturn(successCompositeData);
        Mockito.when(repositoryManagementMBean.getDataStoreGCStatus()).thenReturn(successCompositeData);

        repositoryHealthCheck.setRepositoryManagementMBean(repositoryManagementMBean);

        RevisionCleanupScheduler revisionCleanupScheduler = Mockito.mock(RevisionCleanupScheduler.class);
        repositoryHealthCheck.setRevisionCleanupScheduler(revisionCleanupScheduler);

        VersionCleanupMBean versionCleanupMBean = Mockito.mock(VersionCleanupMBean.class);
        Mockito.when(versionCleanupMBean.isFailed()).thenReturn(false);
        repositoryHealthCheck.setVersionCleanup(versionCleanupMBean);

        Result result = repositoryHealthCheck.execute();
        assertFalse(result.isOk());
        assertEquals(Result.Status.WARN, result.getStatus());

        DataStoreCleanupScheduler dataStoreCleanupScheduler = Mockito.mock(DataStoreCleanupScheduler.class);
        repositoryHealthCheck.setDataStoreCleanupScheduler(dataStoreCleanupScheduler);
        Mockito.when(repositoryManagementMBean.getDataStoreGCStatus()).thenReturn(failedCompositeData);
        result = repositoryHealthCheck.execute();
        assertFalse(result.isOk());
        assertEquals(Result.Status.CRITICAL, result.getStatus());
    }

    @Test
    public void testRevisionFailure() {
        RepositoryMaintenanceHealthCheck repositoryHealthCheck = new RepositoryMaintenanceHealthCheck();

        DataStoreCleanupScheduler dataStoreCleanupScheduler = Mockito.mock(DataStoreCleanupScheduler.class);
        repositoryHealthCheck.setDataStoreCleanupScheduler(dataStoreCleanupScheduler);

        RepositoryManagementMBean repositoryManagementMBean = Mockito.mock(RepositoryManagementMBean.class);
        Mockito.when(repositoryManagementMBean.getRevisionGCStatus()).thenReturn(successCompositeData);
        Mockito.when(repositoryManagementMBean.getDataStoreGCStatus()).thenReturn(successCompositeData);

        repositoryHealthCheck.setRepositoryManagementMBean(repositoryManagementMBean);

        VersionCleanupMBean versionCleanupMBean = Mockito.mock(VersionCleanupMBean.class);
        Mockito.when(versionCleanupMBean.isFailed()).thenReturn(false);
        repositoryHealthCheck.setVersionCleanup(versionCleanupMBean);

        Result result = repositoryHealthCheck.execute();
        assertFalse(result.isOk());
        assertEquals(Result.Status.WARN, result.getStatus());

        Mockito.when(repositoryManagementMBean.getRevisionGCStatus()).thenReturn(failedCompositeData);
        RevisionCleanupScheduler revisionCleanupScheduler = Mockito.mock(RevisionCleanupScheduler.class);
        repositoryHealthCheck.setRevisionCleanupScheduler(revisionCleanupScheduler);
        result = repositoryHealthCheck.execute();
        assertFalse(result.isOk());
        assertEquals(Result.Status.CRITICAL, result.getStatus());
    }

    @Test
    public void testVersionFailure() {
        RepositoryMaintenanceHealthCheck repositoryHealthCheck = new RepositoryMaintenanceHealthCheck();

        DataStoreCleanupScheduler dataStoreCleanupScheduler = Mockito.mock(DataStoreCleanupScheduler.class);
        repositoryHealthCheck.setDataStoreCleanupScheduler(dataStoreCleanupScheduler);

        RepositoryManagementMBean repositoryManagementMBean = Mockito.mock(RepositoryManagementMBean.class);
        Mockito.when(repositoryManagementMBean.getRevisionGCStatus()).thenReturn(successCompositeData);
        Mockito.when(repositoryManagementMBean.getDataStoreGCStatus()).thenReturn(successCompositeData);

        repositoryHealthCheck.setRepositoryManagementMBean(repositoryManagementMBean);

        RevisionCleanupScheduler revisionCleanupScheduler = Mockito.mock(RevisionCleanupScheduler.class);
        repositoryHealthCheck.setRevisionCleanupScheduler(revisionCleanupScheduler);

        Result result = repositoryHealthCheck.execute();
        assertFalse(result.isOk());
        assertEquals(Result.Status.WARN, result.getStatus());

        VersionCleanupMBean versionCleanupMBean = Mockito.mock(VersionCleanupMBean.class);
        Mockito.when(versionCleanupMBean.isFailed()).thenReturn(true);
        repositoryHealthCheck.setVersionCleanup(versionCleanupMBean);
        result = repositoryHealthCheck.execute();
        assertFalse(result.isOk());
        assertEquals(Result.Status.CRITICAL, result.getStatus());
    }

}
