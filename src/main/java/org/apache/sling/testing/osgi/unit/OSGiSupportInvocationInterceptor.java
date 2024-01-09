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

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.platform.commons.support.ReflectionSupport;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;

import java.io.Closeable;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

class OSGiSupportInvocationInterceptor implements InvocationInterceptor {

    @Override
    public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
        invokeWithinContextOfOSGiFramework(invocationContext, extensionContext);
        invocation.skip();
    }

    @Override
    public void interceptTestTemplateMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
        invokeWithinContextOfOSGiFramework(invocationContext, extensionContext);
        invocation.skip();
    }

    private static void invokeWithinContextOfOSGiFramework(ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws ClassNotFoundException, NoSuchMethodException {
        final ExtensionContext.Store store = OSGiSupportFrameworkHandler.getStore(extensionContext);
        final Bundle bundle = store.get(Bundle.class, Bundle.class);
        final Method method = invocationContext.getExecutable();

        final Class<?>[] parameterTypes = method.getParameterTypes();
        final Class<?>[] parameterTypesInOsgi = new Class<?>[parameterTypes.length];
        for (int i = 0; i < parameterTypesInOsgi.length; i++) {
            parameterTypesInOsgi[i] = bundle.loadClass(parameterTypes[i].getName());
        }

        final Class<?> targetClassInOsgi = bundle.loadClass(invocationContext.getTargetClass().getName());
        final Method methodInOsgi = targetClassInOsgi.getDeclaredMethod(method.getName(), parameterTypesInOsgi);

        final Service[] serviceAnnotations = store.get(Service.class, Service[].class);
        final Object[] arguments = new Object[parameterTypesInOsgi.length];
        final Parameter[] parameters = methodInOsgi.getParameters();
        for (int i = 0; i < arguments.length; i++) {
            if (serviceAnnotations[i] != null) {
                arguments[i] = resolveServiceObject(serviceAnnotations[i], store, parameters[i]);
            } else {
                arguments[i] = invocationContext.getArguments().get(i);
            }
        }

        final Object instanceInOSGi = ReflectionSupport.newInstance(targetClassInOsgi);
        ReflectionSupport.invokeMethod(methodInOsgi, instanceInOSGi, arguments);
    }

    @Nullable
    private static Object resolveServiceObject(Service service, ExtensionContext.Store store, Parameter parameter) {
        final String filter = Optional.ofNullable(service.filter()).filter(not(String::isBlank)).orElse(null);
        final Class<?> parameterClass = parameter.getType();

        if (parameterClass.isArray()) {
            final Type componentType;
            final Class<?> castType;
            if (parameter.getParameterizedType() instanceof GenericArrayType) {
                final GenericArrayType genericArrayType = (GenericArrayType) parameter.getParameterizedType();
                componentType = genericArrayType.getGenericComponentType();
                castType = (Class<?>) ((ParameterizedType) componentType).getRawType();
            } else {
                componentType = parameterClass.getComponentType();
                castType = parameterClass.getComponentType();
            }

            return resolveServices(store, componentType, filter)
                    .toArray(i -> (Object[]) Array.newInstance(castType, i));
        } else if (parameterClass.isAssignableFrom(List.class)) {
            final Type genericType = getParameterizedType(parameter).getActualTypeArguments()[0];
            return resolveServices(store, genericType, filter).collect(Collectors.toUnmodifiableList());
        } else {
            return resolveServices(store, parameter.getParameterizedType(), filter).findFirst().orElse(null);
        }
    }

    private static Stream<?> resolveServices(ExtensionContext.Store store, Type wrapperType, String filter) {
        final BundleContext bc = store.get(BundleContext.class, BundleContext.class);

        final Type serviceType;
        final Function<ServiceReference<?>, ?> transformer;
        final Function<ServiceReference<?>, Closeable> closeableFactory;

        if (wrapperType instanceof ParameterizedType) {
            final ParameterizedType type = (ParameterizedType) wrapperType;
            final Class<?> wrapperClass = (Class<?>) type.getRawType();
            if (ServiceReference.class.isAssignableFrom(wrapperClass)) {
                serviceType = type.getActualTypeArguments()[0];
                transformer = Function.identity();
                closeableFactory = ref -> () -> {};
            } else if (ServiceObjects.class.isAssignableFrom(wrapperClass)) {
                serviceType = type.getActualTypeArguments()[0];
                transformer = bc::getServiceObjects;
                closeableFactory = ref -> () -> {};
            } else {
                throw new UnsupportedOperationException("Unsupported wrapper type " + wrapperType);
            }
        } else {
            serviceType = wrapperType;
            transformer = bc::getService;
            closeableFactory = ref -> () -> bc.ungetService(ref);
        }

        final String objectClass = serviceType.getTypeName();
        try {
            final ServiceReference<?>[] serviceReferences = bc.getAllServiceReferences(objectClass, filter);
            return Optional.ofNullable(serviceReferences).stream()
                    .flatMap(Stream::of)
                    .map(ref -> {
                        store.getOrComputeIfAbsent(Closeables.class, k -> new Closeables(), Closeables.class)
                                .add(closeableFactory.apply(ref));
                        return transformer.apply(ref);
                    });
        } catch (InvalidSyntaxException e) {
            throw new ParameterResolutionException(
                    String.format("Failed to retrieve service of type %s%s",
                            objectClass, filter == null ? "" : "with filter " + filter), e);
        }
    }

    private static ParameterizedType getParameterizedType(Parameter parameter) {
        return (ParameterizedType) parameter.getParameterizedType();
    }
}
