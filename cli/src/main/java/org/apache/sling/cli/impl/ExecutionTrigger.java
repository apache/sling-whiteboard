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
package org.apache.sling.cli.impl;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class ExecutionTrigger {

    @Reference
    private CommandProcessor processor;

    protected void activate(BundleContext ctx) {
        ctx.addFrameworkListener(evt -> {
            if (evt.getType() == FrameworkEvent.STARTED)
                new Thread(() -> processor.runCommand(), getClass().getSimpleName() + "Thread").start();
        });
        // never removed but not important - it's one-shot anyway
    }
}
