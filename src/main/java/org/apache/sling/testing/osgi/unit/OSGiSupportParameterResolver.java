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
package org.apache.sling.testing.osgi.unit;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.reflect.Parameter;
import java.util.Objects;
import java.util.Optional;

class OSGiSupportParameterResolver implements ParameterResolver {

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return Objects.nonNull(OSGiSupportFrameworkHandler.getStore(extensionContext).get(parameterContext.getParameter().getType()))
                || parameterContext.findAnnotation(Service.class).isPresent();
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        final ExtensionContext.Store store = OSGiSupportFrameworkHandler.getStore(extensionContext);
        final Parameter parameter = parameterContext.getParameter();
        final Class<?> parameterType = parameter.getType();
        final Service[] serviceAnnotations = store.getOrComputeIfAbsent(Service.class,
                k -> new Service[parameterContext.getDeclaringExecutable().getParameterCount()],
                Service[].class);
        final Optional<Service> serviceAnnotation = parameterContext.findAnnotation(Service.class);
        if (serviceAnnotation.isPresent()) {
            // services need to be resolved late, because they may be loaded by different class loaders in OSGi
            serviceAnnotations[parameterContext.getIndex()] = serviceAnnotation.get();
            return null;
        }
        return store.get(parameterType);
    }
}
