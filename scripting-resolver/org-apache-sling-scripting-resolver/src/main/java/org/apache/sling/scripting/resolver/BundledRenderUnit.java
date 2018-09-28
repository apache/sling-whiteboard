/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Licensed to the Apache Software Foundation (ASF) under one
 ~ or more contributor license agreements.  See the NOTICE file
 ~ distributed with this work for additional information
 ~ regarding copyright ownership.  The ASF licenses this file
 ~ to you under the Apache License, Version 2.0 (the
 ~ "License"); you may not use this file except in compliance
 ~ with the License.  You may obtain a copy of the License at
 ~
 ~   http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package org.apache.sling.scripting.resolver;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.Bundle;

/**
 * <p>
 * A {@code BundledRenderUnit} represents a pre-packaged script or precompiled script that will be executed in order to render a
 * {@link org.apache.sling.api.SlingHttpServletRequest}.
 * </p>
 * <p>
 * If the current {@link org.apache.sling.api.SlingHttpServletRequest} is served by a {@code BundledRenderUnit}, the
 * {@code org.apache.sling.scripting.resolver} will set the {@code BundledRenderUnit} in the {@link javax.script.Bindings} map associated to the request,
 * under the {@link #VARIABLE} key.
 * </p>
 */
@ProviderType
public interface BundledRenderUnit {

    /**
     * The variable available in the {@link javax.script.Bindings} associated to a {@link org.apache.sling.api.SlingHttpServletRequest}
     * if that request is served by a {@code BundledRenderUnit}.
     */
    String VARIABLE = BundledRenderUnit.class.getName();

    /**
     * In case this {@code BundledRenderUnit} wraps a precompiled script, this method will return an instance of that object.
     *
     * @return a precompiled unit, if {@code this} unit wraps a precompiled script; {@code null} otherwise
     */
    @Nullable
    default Object getUnit() {
        return null;
    }

    /**
     * Returns the name of {@code this BundledRenderUnit}. This can be the name of the wrapped script or precompiled script.
     *
     * @return the name {@code this BundledRenderUnit}
     */
    @NotNull String getName();

    /**
     * Returns an instance of the {@link ScriptEngine} that can execute the wrapped script or precompiled script, if the latter needs a
     * specific runtime.
     *
     * @return an instance of the script's or precompiled script's associated {@link ScriptEngine}
     */
    @NotNull ScriptEngine getScriptEngine();

    /**
     * Returns the {@link Bundle} in which the script or precompiled script is packaged. This method can be useful for getting an
     * instance of the bundle's classloader, when needed to load dependencies at run time. To do so the following code example can help:
     *
     * <pre>
     * Bundle bundle = bundledRenderUnit.getBundle();
     * Classloader bundleClassloader = bundle.adapt(BundleWiring.class).getClassLoader();
     * </pre>
     */
    @NotNull Bundle getBundle();

    /**
     * Provided a {@link ScriptContext}, this method will execute / evaluate the wrapped script or precompiled script.
     *
     * @param context the {@link ScriptContext}
     * @throws ScriptException if the execution leads to an error
     */
    void eval(@NotNull ScriptContext context) throws ScriptException;

    /**
     * Retrieves an OSGi runtime dependency of the wrapped script identified by the passed {@code className} parameter.
     *
     * @param className     the fully qualified class name
     * @param <ServiceType> the expected service type
     * @return an instance of the {@link ServiceType} or {@code null}
     */
    @Nullable <ServiceType> ServiceType getService(@NotNull String className);

    /**
     * Retrieves multiple instances of an OSGi runtime dependency of the wrapped script identified by the passed {@code className}
     * parameter, filtered according to the passed {@code filter}.
     *
     * @param className     the fully qualified class name
     * @param filter        a filter expression or {@code null} if all the instances should be returned; for more details about the {@code
     *                      filter}'s syntax check {@link org.osgi.framework.BundleContext#getServiceReferences(String, String)}
     * @param <ServiceType> the expected service type
     * @return an instance of the {@link ServiceType} or {@code null}
     */
    @Nullable <ServiceType> ServiceType[] getServices(@NotNull String className, @Nullable String filter);
}
