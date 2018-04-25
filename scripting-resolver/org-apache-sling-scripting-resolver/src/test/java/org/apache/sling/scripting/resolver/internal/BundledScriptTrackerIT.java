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
package org.apache.sling.scripting.resolver.internal;

import javax.servlet.Servlet;

import org.apache.sling.junit.rules.TeleporterRule;
import org.apache.sling.testing.clients.util.poller.Polling;
import org.junit.Rule;
import org.junit.Test;

public class BundledScriptTrackerIT {

    @Rule
    public TeleporterRule teleporter = TeleporterRule.forClass(getClass(), "IT");

    @Test
    public void testSlingServletForResourceTypeProvided() throws Exception {
        waitForService(Servlet.class, String.format("(%s=%s)", "sling.servlet.resourceTypes", "org.apache.sling.scripting" +
                ".examplebundle.hello/1.0.0"), 20000, 1000);
        waitForService(Servlet.class, String.format("(%s=%s)", "sling.servlet.resourceTypes", "org.apache.sling.scripting" +
                ".examplebundle.hello/2.0.0"), 20000, 1000);
        waitForService(Servlet.class, String.format("(%s=%s)", "sling.servlet.resourceTypes", "org.apache.sling.scripting" +
                ".examplebundle.hello"), 20000, 1000);
        waitForService(Servlet.class, String.format("(%s=%s)", "sling.servlet.resourceTypes", "org.apache.sling.scripting" +
                ".examplebundle.hi/1.0.0"), 20000, 1000);
        waitForService(Servlet.class, String.format("(%s=%s)", "sling.servlet.resourceTypes", "org.apache.sling.scripting" +
                ".examplebundle.hi"), 20000, 1000);
        waitForService(Servlet.class, String.format("(%s=%s)", "sling.servlet.resourceTypes", "org.apache.sling.scripting" +
                ".examplebundle.scriptmatching/1.0.0"), 20000, 1000);
        waitForService(Servlet.class, String.format("(%s=%s)", "sling.servlet.resourceTypes", "org.apache.sling.scripting" +
                ".examplebundle.scriptmatching"), 20000, 1000);
    }

    private void waitForService(Class serviceClass, String filter, long waitTime, long retryAfter)
            throws Exception {
        Polling p = new Polling() {
            @Override
            public Boolean call() {
                return teleporter.getService(serviceClass, filter) != null;
            }

            @Override
            protected String message() {
                return "Cannot obtain a reference to service " + serviceClass.getName() + " with filter " + filter + " after %1$d ms";
            }
        };
        p.poll(waitTime, retryAfter);
    }
}
