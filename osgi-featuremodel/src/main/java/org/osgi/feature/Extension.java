/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.osgi.feature;

import java.util.List;

/**
 * A Feature Model Extension. Extensions can contain either Text, JSON or
 * a list of Artifacts. <p>
 * Extensions are of one of the following kinds:
 * <ul>
 * <li> Mandatory: this extension must be processed by the runtime
 * <li> Optional: this extension does not have to be processed by the runtime
 * <li> Transient: this extension contains transient information such as caching
 * data that is for optimization purposes. It may be changed or removed and is
 * not part of the feature's identity.
 * </ul>
 * @ThreadSafe
 */
public interface Extension {
    enum Kind { MANDATORY, OPTIONAL, TRANSIENT };
    enum Type { JSON, TEXT, ARTIFACTS };

    /**
     * Get the extension name.
     * @return The name.
     */
    String getName();

    /**
     * Get the extension type.
     * @return The type.
     */
    Type getType();

    /**
     * Get the extension kind.
     * @return The kind.
     */
    Kind getKind();

    /**
     * Get the JSON from this extension.
     * @return The JSON, or {@code null} if this is not a JSON extension.
     */
    String getJSON();

    /**
     * Get the Text from this extension.
     * @return The Text, or {@code null} if this is not a Text extension.
     */
    String getText();

    /**
     * Get the Artifacts from this extension.
     * @return The Artifacts, or {@code null} if this is not an Artifacts extension.
     */
    List<ArtifactID> getArtifacts();
}
