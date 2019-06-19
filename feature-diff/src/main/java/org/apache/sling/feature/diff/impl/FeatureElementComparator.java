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
package org.apache.sling.feature.diff.impl;

import org.apache.sling.feature.Feature;

/**
 * A simple service to compare a specific Feature section.
 */
public interface FeatureElementComparator {

    /**
     * Returns a short id which identifies the Feature section for which differences will be computed.
     *
     * @return a short id which identifies the Feature section for which differences will be computed.
     */
    String getId();

    /**
     * Compares a specific Feature sections between the previous and the current,
     * reporting additions/updates/removals in the target.
     *
     * @param previous
     * @param current
     * @param target
     */
    public void computeDiff(Feature previous, Feature current, Feature target);

}
