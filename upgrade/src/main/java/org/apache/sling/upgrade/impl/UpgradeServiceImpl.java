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
package org.apache.sling.upgrade.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.sling.upgrade.EntryHandlerFactory;
import org.apache.sling.upgrade.UpgradeRequest;
import org.apache.sling.upgrade.UpgradeService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = UpgradeService.class)
public class UpgradeServiceImpl implements UpgradeService {

    private static final Logger log = LoggerFactory.getLogger(UpgradeServiceImpl.class);

    @Reference
    private List<EntryHandlerFactory<?>> entryFactories;

    @Override
    public UpgradeRequest readSlingJar(InputStream jar) throws IOException {
        log.trace("readSlingJar");

        UpgradeRequest request = null;
        try (JarInputStream jarIs = new JarInputStream(jar)) {
            Manifest manifest = jarIs.getManifest();

            request = new UpgradeRequest(manifest);

            JarEntry entry = jarIs.getNextJarEntry();
            while (entry != null) {
                log.debug("Reading entry {}", entry.getName());

                for (EntryHandlerFactory<?> factory : entryFactories) {
                    if (factory.matches(entry)) {
                        log.debug("Loading with {}", factory.getClass().getName());
                        request.getEntries().add(factory.loadEntry(entry, jarIs));
                        break;
                    }
                }
                entry = jarIs.getNextJarEntry();
            }

            Collections.sort(request.getEntries());
        }
        return request;
    }

}
