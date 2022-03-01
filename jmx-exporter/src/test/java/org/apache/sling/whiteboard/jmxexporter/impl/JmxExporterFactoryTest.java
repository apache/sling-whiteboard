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
package org.apache.sling.whiteboard.jmxexporter.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.management.ManagementFactory;
import java.util.function.Supplier;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.sling.commons.metrics.MetricsService;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class JmxExporterFactoryTest {

    
    @Rule
    public OsgiContext context = new OsgiContext();
    
    @Captor
    ArgumentCaptor<Supplier<Integer>> intSupplierCaptor;
    
    JmxExporterImplFactory exporter;
    
    private static final String OBJECT_NAME = "org.apache.sling.whiteboard.jmxexporter.impl:type=sample";
    
    private static final String EXPECTED_INT_NAME = "org.apache.sling.whiteboard.jmxexporter.impl.sample.Int";
    
    MetricsService metrics;
    
    
    @Before
    public void setup() throws MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        ObjectName mbeanName = new ObjectName(OBJECT_NAME);
        SimpleBean mbean = new SimpleBean();
        server.registerMBean(mbean, mbeanName);
        
        exporter = new JmxExporterImplFactory(); 
        metrics = Mockito.mock(MetricsService.class);
        context.registerService(MetricsService.class, metrics);
    }
    
    @Test
    public void test() {
        context.registerInjectActivateService(exporter, "objectname",OBJECT_NAME);
        Mockito.verify(metrics).gauge(Mockito.eq(EXPECTED_INT_NAME), intSupplierCaptor.capture());
        assertEquals(new Integer(1),intSupplierCaptor.getValue().get());
        
    }
    
    static class SimpleBean implements SimpleBeanMBean {

        
        int internalInt = 1;
        
        @Override
        public int getInt() {
            return internalInt;
        }
        
        public void setInt(int value) {
            internalInt = value;
        }
        
        
    }
    
    
    static public interface SimpleBeanMBean {
        
        public int getInt();
        
    }
    
}
