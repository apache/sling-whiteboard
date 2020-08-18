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
package org.apache.sling.feature.extension.unpack.impl;

import org.apache.sling.feature.extension.unpack.Unpack;
import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallationContext;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Map;

public class InstallUnpackArchiveTask extends InstallTask {

    private final Logger logger;
    final Unpack unpack;

    public InstallUnpackArchiveTask(TaskResourceGroup erl, Unpack unpack, Logger logger) {
        super(erl);

        this.logger = logger;
        this.unpack = unpack;
    }

    @Override
    public void execute(InstallationContext ctx) {
        @SuppressWarnings("unchecked")
        Map<String,Object> context = (Map<String, Object>) getResource().getAttribute("context");
        if (context == null)
            return;

        try {
            unpack.unpack(getResource().getInputStream(), context);
        } catch (IOException e) {
            logger.error("Problem unpacking {}", getResource().getURL(), e);
        }
    }

    @Override
    public String getSortKey() {
        return getResource().getEntityId();
    }
}
