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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OSGiUnitConfig {

    private final boolean verbose;

    private final OSGiSupport.LogService logService;

    private final Collection<String> additionalBundles;

    private final @NotNull Collection<Class<?>> classes;

    @VisibleForTesting
    OSGiUnitConfig(boolean verbose, OSGiSupport.LogService logService, Collection<String> additionalBundles, Collection<Class<?>> classes) {
        this.verbose = verbose;
        this.logService = logService;
        this.additionalBundles = additionalBundles;
        this.classes = classes;
    }

    public static @NotNull OSGiUnitConfig withDefaults(@NotNull Class<?> clazz) {
        return new OSGiUnitConfig(false, OSGiSupport.LogService.INHERIT, List.of(), List.of(clazz));
    }

    public boolean isVerbose() {
        return verbose;
    }

    public @NotNull Collection<@NotNull String> getLogServiceBundles() {
        return logService.getSymbolicNames();
    }

    public @NotNull Collection<@NotNull String> getAdditionalBundles() {
        return additionalBundles;
    }

    public @NotNull Collection<@NotNull Class<?>> getClasses() {
        return classes;
    }

    public static OSGiUnitConfig merge(OSGiUnitConfig cfg, OSGiSupport annotation) {
        return new OSGiUnitConfig(
                annotation.verbose() == OSGiSupport.Enablement.INHERIT ? cfg.verbose : annotation.verbose().isEnabled(),
                annotation.logService() == OSGiSupport.LogService.INHERIT ? cfg.logService : annotation.logService(),
                merge(cfg.additionalBundles, annotation.additionalBundles()),
                merge(cfg.classes, annotation.additionalClasses()));
    }

    private static <T> Collection<T> merge(Collection<T> collection, T[] array) {
        return Stream.concat(collection.stream(), Stream.of(array))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toUnmodifiableList());
    }
}
