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

import static org.mockito.Mockito.never;

import javax.management.openmbean.CompositeData;

import org.apache.jackrabbit.oak.api.jmx.RepositoryManagementMBean;
import org.apache.jackrabbit.oak.api.jmx.RepositoryManagementMBean.StatusCode;
import org.apache.sling.repositorymaintainance.CompositeDataMock;
import org.junit.Test;
import org.mockito.Mockito;

public class RevisionCleanupSchedulerTest {

    @Test
    public void testRunnable() {
        final RevisionCleanupScheduler rcs = new RevisionCleanupScheduler();

        Integer id = 1;
        final RepositoryManagementMBean repositoryManager = Mockito.mock(RepositoryManagementMBean.class);
        CompositeData startingCd = CompositeDataMock.init().put("id", id).build();
        Mockito.when(repositoryManager.startDataStoreGC(false)).thenReturn(startingCd);
        CompositeData doneCd = CompositeDataMock.init().put("id", (Integer) id + 1)
                .put("code", StatusCode.SUCCEEDED.ordinal()).build();
        Mockito.when(repositoryManager.getRevisionGCStatus()).thenReturn(doneCd);
        rcs.setRepositoryManager(repositoryManager);

        rcs.run();

        Mockito.verify(repositoryManager).startRevisionGC();
    }

    @Test
    public void testRunning() {
        final RevisionCleanupScheduler rcs = new RevisionCleanupScheduler();

        Integer id = 1;
        final RepositoryManagementMBean repositoryManager = Mockito.mock(RepositoryManagementMBean.class);
        CompositeData runningCd = CompositeDataMock.init().put("id", id).put("code", StatusCode.RUNNING.ordinal())
                .build();
        Mockito.when(repositoryManager.getRevisionGCStatus()).thenReturn(runningCd);
        rcs.setRepositoryManager(repositoryManager);

        rcs.run();

        Mockito.verify(repositoryManager, never()).startRevisionGC();
    }

}