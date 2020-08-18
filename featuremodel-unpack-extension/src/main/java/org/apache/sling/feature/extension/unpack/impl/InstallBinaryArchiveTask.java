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

import java.util.Map;

public class InstallBinaryArchiveTask extends InstallTask {

    public InstallBinaryArchiveTask(TaskResourceGroup erl) {
        super(erl);
    }

    @Override
    public void execute(InstallationContext ctx) {
        Unpack unpack = (Unpack) getResource().getAttribute("__unpack__");
        if (unpack == null)
            return;

        Map<String,String> context = (Map<String, String>) getResource().getAttribute("context");
        unpack.unpack(null, context);
//        unpack.unpack(url, context); // TODO I only have an Inputstream

        // TODO handle 'Font-Archive-Contents' (or 'Binary-Archive-Contents')?

    }

    @Override
    public String getSortKey() {
        // TODO Auto-generated method stub
        return "999";
    }

}
