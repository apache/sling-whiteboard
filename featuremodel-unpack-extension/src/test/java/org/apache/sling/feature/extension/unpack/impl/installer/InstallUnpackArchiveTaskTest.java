/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.feature.extension.unpack.impl.installer;

import org.apache.sling.feature.extension.unpack.Unpack;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;

public class InstallUnpackArchiveTaskTest {
    @Test
    public void testTaskIgnoresNoContext() {
        TaskResource tr = Mockito.mock(TaskResource.class);
        Mockito.when(tr.getEntityId()).thenReturn("1234");

        TaskResourceGroup trg = Mockito.mock(TaskResourceGroup.class);
        Mockito.when(trg.getActiveResource()).thenReturn(tr);

        Unpack unpack = Mockito.mock(Unpack.class);

        InstallUnpackArchiveTask iuat = new InstallUnpackArchiveTask(trg, unpack, null);

        Mockito.verifyZeroInteractions(unpack); // precondition
        iuat.execute(null);

        // Should have done nothing
        Mockito.verifyZeroInteractions(unpack);

        assertNotNull(iuat.getSortKey());
    }

    @Test
    public void testTask() throws Exception {
        Map<String,Object> context = new HashMap<>();
        InputStream bais = new ByteArrayInputStream("".getBytes());

        TaskResource tr = Mockito.mock(TaskResource.class);
        Mockito.when(tr.getAttribute("context")).thenReturn(context);
        Mockito.when(tr.getInputStream()).thenReturn(bais);

        TaskResourceGroup trg = Mockito.mock(TaskResourceGroup.class);
        Mockito.when(trg.getActiveResource()).thenReturn(tr);

        Unpack unpack = Mockito.mock(Unpack.class);

        InstallUnpackArchiveTask iuat = new InstallUnpackArchiveTask(trg, unpack, null);

        Mockito.verifyZeroInteractions(unpack); // precondition
        iuat.execute(null);

        Mockito.verify(unpack).unpack(bais, context);
    }
}
