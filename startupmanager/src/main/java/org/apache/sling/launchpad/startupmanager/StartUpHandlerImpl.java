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
package org.apache.sling.launchpad.startupmanager;

import org.apache.sling.launchpad.api.StartupHandler;
import org.apache.sling.launchpad.api.StartupMode;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;

public class StartUpHandlerImpl implements StartupHandler
{
    private final Bundle framework;

    public StartUpHandlerImpl(Bundle bundle)
    {
        framework = bundle;
    }

    @Override
    public StartupMode getMode()
    {
        return StartupMode.INSTALL;
    }

    @Override
    public boolean isFinished()
    {
        return framework.getState() == Bundle.ACTIVE;
    }

    @Override
    public void waitWithStartup(boolean b)
    {
        // Ignore
    }
}
