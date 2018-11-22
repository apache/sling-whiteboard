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

import org.apache.sling.launchpad.api.StartupListener;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Hashtable;

public class Activator implements BundleActivator {

    private static final Logger log = LoggerFactory.getLogger(Activator.class);

    private ServiceRegistration<?> mbeanServerReg;

    private ServiceRegistration<StartupListener> mbeanStartupListenerReg;

    private StartupListenerTracker startupListenerTracker;

    @Override
    public void start(final BundleContext bundleContext) {
        startupListenerTracker = new StartupListenerTracker(bundleContext);
        mbeanServerReg = registerMBeanServer(bundleContext);
        try {
            mbeanStartupListenerReg = bundleContext.registerService(StartupListener.class, new MBeanStartupListener(), new Hashtable<String, Object>());
        } catch (MalformedObjectNameException e) {
            log.error("Can't instantiate MBeanStartupListener");
        }
    }

    @Override
    public void stop(final BundleContext context) {
        if (mbeanStartupListenerReg != null) {
            mbeanStartupListenerReg.unregister();
            mbeanStartupListenerReg = null;
        }
        if (mbeanServerReg != null) {
            mbeanServerReg.unregister();
            mbeanServerReg = null;
        }
        if (startupListenerTracker != null) {
            startupListenerTracker.close();
            startupListenerTracker = null;
        }
    }

    private ServiceRegistration<?> registerMBeanServer(final BundleContext bundleContext) {
        // register the platform MBeanServer
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        Hashtable<String, Object> mbeanProps = new Hashtable<String, Object>();
        try {
            ObjectName beanName = ObjectName.getInstance("JMImplementation:type=MBeanServerDelegate");
            AttributeList attrs = platformMBeanServer.getAttributes(beanName,
                    new String[] { "MBeanServerId", "SpecificationName",
                            "SpecificationVersion", "SpecificationVendor",
                            "ImplementationName", "ImplementationVersion",
                            "ImplementationVendor" });
            for (Object object : attrs) {
                Attribute attr = (Attribute) object;
                if (attr.getValue() != null) {
                    mbeanProps.put(attr.getName(), attr.getValue().toString());
                }
            }
        } catch (Exception je) {
            log.error("start: Cannot set service properties of Platform MBeanServer service, registering without", je);
        }
        return bundleContext.registerService(MBeanServer.class.getName(), platformMBeanServer, mbeanProps);
    }
}