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
package org.apache.sling.microsling.services;

import junit.framework.TestCase;

import org.apache.sling.microsling.api.ResourceResolver;
import org.apache.sling.microsling.api.ServiceLocator;

public class MicroslingServiceLocatorTest extends TestCase {
    private ServiceLocator serviceLocator;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        serviceLocator = new MicroslingServiceLocator(); 
    }

    public void testServiceFound() {
        try {
            final Object svc = serviceLocator.getService(ResourceResolver.class.getName());
            assertNotNull(svc);
            assertTrue(svc instanceof ResourceResolver);
        } catch(Exception e) {
            fail("Unexpected Exception in getService call " + e);
        }
    }
    
    public void testServiceNotFound() {
        try {
            final Object svc = serviceLocator.getService("some stuff");
            assertNull(svc);
        } catch(Exception e) {
            fail("Unexpected Exception in getService call " + e);
        }
    }
    
    public void testRequiredServiceFound() {
        try {
            final Object svc = serviceLocator.getRequiredService(ResourceResolver.class.getName());
            assertNotNull(svc);
            assertTrue(svc instanceof ResourceResolver);
        } catch(Exception e) {
            fail("Unexpected Exception in getService call " + e);
        }
    }
    
    public void testRequiredServiceNotFound() {
        try {
            serviceLocator.getRequiredService("some stuff");
            fail("Expected Exception when service is not found");
            
        } catch(ServiceLocator.ServiceNotAvailableException sna) {
            //fine - as expected
            
        } catch(Exception e) {
            fail("Unexpected Exception in getRequiredService call " + e);
        }
    }
    
    public void testGetServices() {
        try {
            final Object svc = serviceLocator.getServices(ResourceResolver.class.getName(),null);
            assertNull("getServices must return null",svc);
        } catch(Exception e) {
            fail("Unexpected Exception in getServices call " + e);
        }
    }
}
