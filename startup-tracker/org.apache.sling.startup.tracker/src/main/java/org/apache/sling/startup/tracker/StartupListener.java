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
package org.apache.sling.startup.tracker;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleEvent;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.component.annotations.Component;

/**
 * 
 */
@Component(service = { ServiceListener.class, SynchronousBundleListener.class, FrameworkListener.class })
public class StartupListener implements ServiceListener, SynchronousBundleListener, FrameworkListener {

    private Map<String, AbstractStartupItem> startupRecords = new HashMap<>();

    private String currentStartLevel = "0";

    @Override
    public void serviceChanged(ServiceEvent event) {
        event.
    }

    @Override
    public void bundleChanged(BundleEvent event) {
        String bundlePid = event.getBundle().getSymbolicName();
        if (event.getType() == BundleEvent.INSTALLED) {
            BundleStartup bundle = new BundleStartup(currentStartLevel, bundlePid);
            startupRecords.put(bundle.getId(), bundle);
        } else if (event.getType() == BundleEvent.STARTED || event.getType() == BundleEvent.LAZY_ACTIVATION) {
            startupRecords.get(ItemType.getId(ItemType.BUNDLE, bundlePid)).setComplete(System.currentTimeMillis());
        } else {
            BundleEventItem evt = new BundleEventItem(event);
            startupRecords.put(evt.getId(), evt);
        }
    }

    @Override
    public void frameworkEvent(FrameworkEvent event) {
        // TODO Auto-generated method stub

    }

}
