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
package org.apache.sling.capabilities.oakdescriptors;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.sling.capabilities.CapabilitiesSource;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CapabilitiesSource that provides information on the Oak
 * by exposing (some) repository descriptor values */
@Component(service = CapabilitiesSource.class)
@Designate(ocd = OakDescriptorSource.Config.class)
public class OakDescriptorSource implements CapabilitiesSource {

    public static final String NAMESPACE = "org.apache.sling.oak.descriptor";

    private final Logger log = LoggerFactory.getLogger(getClass().getName());

    @Reference
    private SlingRepository repository;

    private int cacheLifetimeSeconds;

    private long cacheExpires;

    private Map<String, Object> cachedReadOnlyMap;

    private Pattern[] keyPatterns;

    @ObjectClassDefinition(name = "Apache Sling Capabilities - Oak Descriptors Source", description = "Provides information about Oak by exposing Oak Repository Descriptors")
    public static @interface Config {
        @AttributeDefinition(name = "Key Whitelist", description = "List of Oak Repository Descriptor keys that are exposed.")
        String[] keyWhitelist() default { "" };

        @AttributeDefinition(name = "Cache time-to-live in seconds", description = "The repository descriptor values are cached for this amount of time, "
                + "accounting for potentially expensive value calculations")
        int cacheLifetimeSeconds() default 60;
    }

    @Activate
    protected void activate(Config cfg, ComponentContext ctx) {
        cacheLifetimeSeconds = cfg.cacheLifetimeSeconds();
        if (cfg.keyWhitelist() == null || cfg.keyWhitelist().length == 0) {
            keyPatterns = null;
        } else {
            keyPatterns = new Pattern[cfg.keyWhitelist().length];
            int i = 0;
            for (String regex : cfg.keyWhitelist()) {
                keyPatterns[i++] = Pattern.compile(regex);
            }
        }
    }

    @Override
    public Map<String, Object> getCapabilities() throws Exception {
        final SlingRepository localRepo = repository;
        if (localRepo == null) {
            return Collections.emptyMap();
        }

        refreshCachedValues();
        return cachedReadOnlyMap;
    }

    private void refreshCachedValues() {
        if (System.currentTimeMillis() < cacheExpires) {
            log.debug("refreshCachedValues: Using cached oak repository descriptor value");
            return;
        }

        cacheExpires = System.currentTimeMillis() + (cacheLifetimeSeconds * 1000L);

        String[] keys = repository.getDescriptorKeys();
        List<String> filteredKeys = filterKeys(keys);
        if (filteredKeys == null || filteredKeys.isEmpty()) {
            cachedReadOnlyMap = Collections.emptyMap();
        }

        final HashMap<String, Object> result = new HashMap<>();
        for (String key : filteredKeys) {
            try {
                result.put(key, repository.getDescriptorValue(key).getString());
            } catch (ValueFormatException e) {
                log.debug("refreshCachedValues: ValueFormatException ({}) reading key ({})", e.getMessage(), key);
            } catch (IllegalStateException e) {
                log.debug("refreshCachedValues: IllegalStateException ({}) reading key ({})", e.getMessage(), key);
            } catch (RepositoryException e) {
                log.debug("refreshCachedValues: RepositoryException ({}) reading key ({})", e.getMessage(), key);
            }
        }
        cachedReadOnlyMap = Collections.unmodifiableMap(result);

        log.debug("refreshCachedValues: refreshed oak repository descriptor values");

    }

    private List<String> filterKeys(String[] keys) {
        final List<String> result = new LinkedList<>();
        for (String key : keys) {
            if (whitelisted(key)) {
                result.add(key);
            }
        }
        return result;
    }

    private boolean whitelisted(String key) {
        if (keyPatterns == null || keyPatterns.length == 0) {
            // nothing white-listed
            return false;
        }
        if (key == null || key.length() == 0) {
            // empty key is not allowed
            return false;
        }
        for (Pattern keyPattern : keyPatterns) {
            if (matches(key, keyPattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean matches(String key, Pattern keyPattern) {
        if (keyPattern == null) {
            return false;
        }
        return keyPattern.matcher(key).matches();
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

}
