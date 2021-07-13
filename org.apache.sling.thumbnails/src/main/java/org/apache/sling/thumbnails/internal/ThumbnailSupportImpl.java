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
package org.apache.sling.thumbnails.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.thumbnails.ThumbnailSupport;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = { ThumbnailSupport.class })
@Designate(ocd = ThumbnailSupportConfig.class)
public class ThumbnailSupportImpl implements ThumbnailSupport {

    private static final Logger log = LoggerFactory.getLogger(ThumbnailSupportImpl.class);

    private final Map<String, String> persistableTypes = new HashMap<>();
    private final Map<String, String> supportedTypes = new HashMap<>();

    private final ThumbnailSupportConfig config;

    @Activate
    public ThumbnailSupportImpl(ThumbnailSupportConfig config) {
        Arrays.stream(config.supportedTypes()).forEach(nt -> {
            String[] cfg = nt.split("\\=");
            if (cfg.length != 2 || StringUtils.isEmpty(cfg[0]) || StringUtils.isEmpty(cfg[1])) {
                log.warn("Could not parse supported resource type from {}", nt);
            } else if (supportedTypes.containsKey(cfg[0])) {
                log.warn("Ignoring duplicate supported resource type: {}", cfg[0]);
            } else {
                supportedTypes.put(cfg[0], cfg[1]);
            }
        });

        Arrays.stream(config.persistableTypes()).forEach(nt -> {
            String[] cfg = nt.split("\\=");
            if (cfg.length != 2 || StringUtils.isEmpty(cfg[0]) || StringUtils.isEmpty(cfg[1])) {
                log.warn("Could not parse persisted resource type from {}", nt);
            } else if (!supportedTypes.containsKey(cfg[0])) {
                log.warn("Ignoring unsupported persistable resource type: {}", cfg[0]);
            } else if (persistableTypes.containsKey(cfg[0])) {
                log.warn("Ignoring duplicate persistable resource type: {}", cfg[0]);
            } else {
                persistableTypes.put(cfg[0], cfg[1]);
            }
        });
        this.config = config;
    }

    @Override
    public @NotNull String getMetaTypePropertyPath(@NotNull String resourceType) {
        if (!supportedTypes.containsKey(resourceType)) {
            throw new IllegalArgumentException("Supplied unsupported resource type " + resourceType);
        } else {
            return supportedTypes.get(resourceType);
        }
    }

    @Override
    public @NotNull Set<String> getPersistableTypes() {
        return persistableTypes.keySet();
    }

    @Override
    public @NotNull String getRenditionPath(@NotNull String resourceType) {
        if (!persistableTypes.containsKey(resourceType)) {
            throw new IllegalArgumentException("Supplied non-persistable resource type " + resourceType);
        } else {
            return persistableTypes.get(resourceType);
        }
    }

    @Override
    public @NotNull String getServletErrorSuffix() {
        return config.errorSuffix();
    }

    @Override
    public @NotNull Set<String> getSupportedTypes() {
        return supportedTypes.keySet();
    }

    @Override
    public @NotNull String getServletErrorResourcePath() {
        return config.errorResourcePath();
    }

}
