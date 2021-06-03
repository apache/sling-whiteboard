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
package org.apache.sling.sitemap.impl.builder.extensions;

import org.apache.sling.sitemap.builder.extensions.AbstractExtension;
import org.apache.sling.sitemap.builder.extensions.ExtensionProvider;
import org.jetbrains.annotations.NotNull;

public class ExtensionFactory {

    private final ExtensionProvider provider;
    private final String namespace;
    private final String prefix;
    private final String localName;
    private final boolean emptyTag;

    ExtensionFactory(ExtensionProvider provider,
                     String namespace, String prefix, String localName, boolean emptyTag) {
        this.provider = provider;
        this.namespace = namespace;
        this.prefix = prefix;
        this.localName = localName;
        this.emptyTag = emptyTag;
    }

    @NotNull
    public AbstractExtension newExtension() {
        return provider.newInstance();
    }

    public String getNamespace() {
        return namespace;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getLocalName() {
        return localName;
    }

    public boolean isEmptyTag() {
        return emptyTag;
    }
}
