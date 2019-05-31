/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.feature.launcher.extensions.connect.impl;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.sling.feature.launcher.impl.launchers.FrameworkRunner;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.launch.Framework;

public class PojoSRRunner extends FrameworkRunner
{
    public PojoSRRunner(Map<String, String> frameworkProperties, Map<Integer, List<File>> bundlesMap, List<Object[]> configurations, List<File> installables) throws Exception
    {
        super(frameworkProperties, bundlesMap, configurations, installables);
    }

    @Override
    protected void setupFramework(Framework framework, Map<Integer, List<File>> bundlesMap) throws BundleException
    {
        try
        {
            FrameworkUtil.BUNDLE = framework;
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
        super.setupFramework(framework, framework.getSymbolicName().equals("org.apache.felix.connect") ? Collections.emptyMap() : bundlesMap);
    }
}
