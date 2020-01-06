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
 * A builder for {@link MergeContext} objects.
 * @NotThreadSafe
 */
public interface MergeContextBuilder {
    /**
     * Set the Bundle Conflict Resolver.
     * @param bh The Conflict Resolver.
     * @return This builder.
     */
    MergeContextBuilder bundleConflictHandler(ConflictResolver<Bundle, List<Bundle>> bh);

    /**
     * Set the Configuration Conflict Resolver.
     * @param ch The Conflict Resolver.
     * @return This builder.
     */
    MergeContextBuilder configConflictHandler(ConflictResolver<Configuration, Configuration> ch);

    /**
     * Set the Extension Conflict Resolver.
     * @param eh The Conflict Resolver.
     * @return This builder.
     */
    MergeContextBuilder extensionConflictHandler(ConflictResolver<Extension, Extension> eh);

    /**
     * Build the Merge Context. Can only be called once on a builder. After
     * calling this method the current builder instance cannot be used any more.
     * @return The Merge Context.
     */
    MergeContext build();

}
