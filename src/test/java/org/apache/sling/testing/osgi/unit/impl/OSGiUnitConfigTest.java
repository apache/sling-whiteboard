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
package org.apache.sling.testing.osgi.unit.impl;

import org.apache.sling.testing.osgi.unit.OSGiSupport;
import org.apache.sling.testing.osgi.unit.OSGiSupport.Enablement;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.osgi.util.converter.Converters;

import java.util.EnumSet;
import java.util.Map;
import java.util.function.Consumer;

import static org.apache.sling.testing.osgi.unit.impl.OSGiUnitConfig.merge;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OSGiUnitConfigTest {

    static final @NotNull OSGiUnitConfig DEFAULT_CONFIG = OSGiUnitConfig.withDefaults(String.class);

    @Test
    void defaultConfig() {
        assertSame(DEFAULT_CONFIG.isVerbose(), false);
        assertThat(DEFAULT_CONFIG.getLogServiceBundles()).isEmpty();
        assertThat(DEFAULT_CONFIG.getAdditionalBundles()).isEmpty();
        assertThat(DEFAULT_CONFIG.getClasses())
                .hasSize(1)
                .containsOnly(String.class);
    }

    @ParameterizedTest
    @EnumSource(Enablement.class)
    void verboseInheritance(Enablement parentVerbosity) {
        final OSGiUnitConfig parentConfig = merge(
                DEFAULT_CONFIG,
                osgiSupport(b -> b.verbose(parentVerbosity)));

        assertEquals(
                merge(parentConfig, osgiSupport(b -> {})).isVerbose(),
                parentConfig.isVerbose(),
                "verbose should be inherited by default");

        assertEquals(
                merge(parentConfig, osgiSupport(b -> b.verbose(Enablement.INHERIT))).isVerbose(),
                parentConfig.isVerbose(),
                "verbose should be inherited explicitly");

        assertTrue(
                merge(parentConfig, osgiSupport(b -> b.verbose(Enablement.ENABLED))).isVerbose(),
                "verbose should be overridden to be enabled");

        assertFalse(
                merge(parentConfig, osgiSupport(b -> b.verbose(Enablement.DISABLED))).isVerbose(),
                "verbose should be overridden to be disabled");
    }

    @ParameterizedTest
    @EnumSource(value = OSGiSupport.LogService.class, mode = EnumSource.Mode.MATCH_ALL)
    void logServiceInheritance(OSGiSupport.LogService parentLogService) {
        final OSGiUnitConfig parentConfig = merge(
                DEFAULT_CONFIG,
                osgiSupport(b -> b.logService(parentLogService)));

        final EnumSet<OSGiSupport.LogService> options =
                EnumSet.complementOf(EnumSet.of(OSGiSupport.LogService.INHERIT));

        assertThat(merge(parentConfig, osgiSupport(b -> {})).getLogServiceBundles())
                .containsExactlyInAnyOrderElementsOf(parentConfig.getLogServiceBundles());

        assertThat(merge(parentConfig, osgiSupport(b -> b.logService(OSGiSupport.LogService.INHERIT))).getLogServiceBundles())
                .containsExactlyInAnyOrderElementsOf(parentConfig.getLogServiceBundles());

        for (OSGiSupport.LogService option : options) {
            assertThat(merge(parentConfig, osgiSupport(b -> b.logService(option))).getLogServiceBundles())
                    .containsExactlyInAnyOrderElementsOf(option.getSymbolicNames());
        }
    }

    @Test
    void additionalBundlesMerging() {
        final OSGiUnitConfig parentConfig = merge(
                DEFAULT_CONFIG,
                osgiSupport(b -> b.additionalBundles("bar", "foo")));

        assertThat(merge(parentConfig, osgiSupport(b -> {})).getAdditionalBundles())
                .containsExactlyInAnyOrder("bar", "foo");

        assertThat(merge(parentConfig, osgiSupport(b -> b.additionalBundles("baz", "fuzz"))).getAdditionalBundles())
                .containsExactlyInAnyOrder("bar", "foo", "baz", "fuzz");
    }

    @Test
    void additionalClassesMerging() {
        final OSGiUnitConfig parentConfig = DEFAULT_CONFIG;

        assertThat(merge(parentConfig, osgiSupport(b -> {})).getClasses())
                .containsExactlyInAnyOrder(String.class);

        assertThat(merge(parentConfig, osgiSupport(b -> b.additionalClasses(CharSequence.class, Integer.class))).getClasses())
                .containsExactlyInAnyOrder(String.class, CharSequence.class, Integer.class);
    }

    private static @NotNull OSGiSupport osgiSupport(Consumer<OSGiSupportBuilder> builderCallback) {
        final OSGiSupportBuilder builder = new OSGiSupportBuilder();
        builderCallback.accept(builder);
        return builder.build();
    }

    private static class OSGiSupportBuilder {

        Enablement verbose;

        OSGiSupport.LogService logService;

        String[] additionalBundles;

        Class<?>[] additionalClasses;

        OSGiSupportBuilder() {
            final OSGiSupport defaultOsgiSupport = Converters.standardConverter()
                    .convert(Map.of())
                    .to(OSGiSupport.class);
            verbose = defaultOsgiSupport.verbose();
            logService = defaultOsgiSupport.logService();
            additionalBundles = defaultOsgiSupport.additionalBundles();
            additionalClasses = defaultOsgiSupport.additionalClasses();
        }

        OSGiSupportBuilder verbose(Enablement verbose) {
            this.verbose = verbose;
            return this;
        }

        OSGiSupportBuilder logService(OSGiSupport.LogService logService) {
            this.logService = logService;
            return this;
        }

        OSGiSupportBuilder additionalBundles(String... additionalBundles) {
            this.additionalBundles = additionalBundles;
            return this;
        }

        OSGiSupportBuilder additionalClasses(Class<?>... additionalClasses) {
            this.additionalClasses = additionalClasses;
            return this;
        }

        OSGiSupport build() {
            final Map<String, Object> map = Map.of(
                    "verbose", verbose,
                    "logService", logService,
                    "additionalBundles", additionalBundles,
                    "additionalClasses", additionalClasses
            );
            return Converters.standardConverter().convert(map).to(OSGiSupport.class);
        }
    }
}