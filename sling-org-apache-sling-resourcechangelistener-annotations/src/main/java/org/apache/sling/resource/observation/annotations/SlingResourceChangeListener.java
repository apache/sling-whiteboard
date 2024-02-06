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
package org.apache.sling.resource.observation.annotations;

import org.apache.sling.api.resource.observation.ExternalResourceChangeListener;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.osgi.service.component.annotations.ComponentPropertyType;

/**
 * <a href="https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.component.html#service.component-component.property.types">
 * Component Property Type (as defined by OSGi DS 1.4)</a> for Sling (External)ResourceChangeListener services.
 * Takes care of writing the relevant service properties as being used by the Sling Observation
 * to register the annotated {@link ResourceChangeListener} or {@link ExternalResourceChangeListener} component
 * with the correct restrictions.
 *
 * @see ResourceChangeListener
 * @see ExternalResourceChangeListener
 */
@ComponentPropertyType
public @interface SlingResourceChangeListener {

    /**
     * Prefix for every property being generated from the annotations elements (as defined in OSGi 7 Compendium, 112.8.2.1)
     */
    static final String PREFIX_ = "resource.";

    /**
     * <p>Array of paths or glob patterns - required.</p>
     *
     * <p>A path is either absolute or relative. If it's a relative path, the relative path will be appended to all search paths of the
     * resource resolver.</p>
     *
     * <p>If the whole tree of all search paths should be observed, the special value {@code .} should be used.</p>
     *
     * <p>A glob pattern must start with the {@code glob:} prefix (e.g. <code>glob:**&#47;*.html</code>). The following rules are used
     * to interpret glob patterns:</p>
     * <ul>
     *     <li>The {@code *} character matches zero or more characters of a name component without crossing directory boundaries.</li>
     *     <li>The {@code **} characters match zero or more characters crossing directory boundaries.</li>
     *     <li>If the pattern is relative (does not start with a slash), the relative path will be appended to all search paths of
     *        the resource resolver.
     * </ul>
     *
     * <p>
     * In general, it can't be guaranteed that the underlying implementation of the resources will send a remove
     * event for each removed resource. For example if the root of a tree, like {@code /foo} is removed, the underlying
     * implementation might only send a single remove event for {@code /foo} but not for any child resources.
     * Therefore if a listener is interested in resource remove events, it might get remove events for resources
     * that not directly match the specified pattern/filters. For example if a listener is registered for {@code /foo/bar}
     * and {@code /foo} is removed, the listener will get a remove event for {@code /foo}. The same is true if any pattern is used
     * and any parent of a matching resource is removed. If a listener is interested in
     * remove events, it will get a remove of any parent resource from the specified paths or patterns. The listener
     * must handle these events accordingly.
     *
     * <p>If one of the paths is a sub resource of another specified path, the sub path is ignored.</p>
     *
     * <p>If this property is missing or invalid, the listener is ignored. The type of the property must either be String, or a String
     * array.</p>
     *
     * @see ResourceChangeListener#PATHS
     */
    String[] paths();

    /**
     * Array of change types - optional.
     * If this property is missing, added, removed and changed events for resources
     * and added and removed events for resource providers are reported.
     *
     * @see ResourceChangeListener#CHANGES
     * @see ResourceChangeType
     */
    ResourceChange.ChangeType[] change_types() default {};

    /**
     * An optional hint indicating to the underlying implementation that for
     * changes regarding properties (added/removed/changed) the listener
     * is only interested in those property names listed inhere.
     * If the underlying implementation supports this, events for property names that
     * are not enlisted here will not be delivered, however events
     * concerning resources are not affected by this hint.
     * This is only a hint, a change listener registering with this property
     * must be prepared that the underlying implementation is not able to filter
     * based on this property. In this case the listener gets all events
     * as defined with the other properties.
     *
     * @see ResourceChangeListener#PROPERTY_NAMES_HINT
     */
    String[] property_names_hint() default {};
}
