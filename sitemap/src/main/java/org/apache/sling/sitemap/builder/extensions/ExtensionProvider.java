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
package org.apache.sling.sitemap.builder.extensions;

import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * To provide an implementation for a defined sub type of {@link org.apache.sling.sitemap.builder.Extension} an
 * {@link ExtensionProvider} needs to be registered as OSGI service.
 * <p>
 * In order to hide the implementation detail of an extension from the the consumer API, the return type of
 * {@link ExtensionProvider#newInstance()} is {@link AbstractExtension}, the provider facing API of the extension.
 * However to use the returned instance, it has to also implement the extension sub type interface and be registered
 * with it's full qualified class name.
 * <p>
 * There may be multiple {@link ExtensionProvider}s using the same namespace. If so the one with the highest ranking
 * according to the OSGI specification will define the namespace's prefix, which means that lower ranking services
 * may use another prefix then they were registered with.
 */
@ConsumerType
public interface ExtensionProvider {

    /**
     * The mandatory property to set to the {@link org.apache.sling.sitemap.builder.Extension} sub-type.
     */
    String PROPERTY_INTERFACE = "extension.interface";
    /**
     * The xml namespace prefix to use for the extension.
     */
    String PROPERTY_PREFIX = "extension.prefix";
    /**
     * The xml namespace to use for the extension.
     */
    String PROPERTY_NAMESPACE = "extension.namespace";
    /**
     * The local tag name to use when adding the extension to the sitemap xml.
     */
    String PROPERTY_LOCAL_NAME = "extension.localName";
    /**
     * An optional property to be set for extensions that only write attributes to the added xml tag and want to make
     * use of an empty-tag. If not set an open and close tag will be written.
     */
    String PROPERTY_EMPTY_TAG = "extension.emptyTag";

    /**
     * Returns a new instance of the extension provided by the {@link ExtensionProvider}.
     *
     * @return
     */
    @NotNull
    AbstractExtension newInstance();
}
