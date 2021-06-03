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

import org.apache.sling.sitemap.builder.Extension;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ConsumerType;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * The producer API of an extension.
 */
@ConsumerType
public abstract class AbstractExtension implements Extension {

    /**
     * Implementations must write their content to the given {@link XMLStreamWriter}.
     * <p>
     * The extension must not open/close its own surrounding tag. This is done by the caller in order to guarantee
     * proper isolation between the core implementation and the extensions. Furthermore, when an extension fails and
     * throws an {@link XMLStreamException} the extensions output will simply be discarded but the sitemap generation
     * will not fail.
     *
     * @param writer
     * @throws XMLStreamException
     */
    public abstract void writeTo(@NotNull XMLStreamWriter writer) throws XMLStreamException;

}
