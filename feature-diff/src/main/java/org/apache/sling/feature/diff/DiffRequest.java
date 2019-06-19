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
package org.apache.sling.feature.diff;

import static java.util.Objects.requireNonNull;

import java.util.HashSet;
import java.util.Set;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;

public final class DiffRequest {

    private final Set<String> includeComparators = new HashSet<>();

    private final Set<String> excludeComparators = new HashSet<>();

    private Feature previous;

    private Feature current;

    private ArtifactId resultId;

    public Feature getPrevious() {
        return previous;
    }

    public DiffRequest setPrevious(Feature previous) {
        this.previous = requireNonNull(previous, "Impossible to compare null previous feature.");
        return this;
    }

    public Feature getCurrent() {
        return current;
    }

    public DiffRequest setCurrent(Feature current) {
        this.current = requireNonNull(current, "Impossible to compare null current feature.");
        return this;
    }

    public ArtifactId getResultId() {
        return resultId;
    }

    public DiffRequest setResultId(String resultId) {
        resultId = requireNonNull(resultId, "Impossible to create the Feature diff with a null id");
        return setResultId(ArtifactId.parse(resultId));
    }

    public DiffRequest setResultId(ArtifactId resultId) {
        this.resultId = requireNonNull(resultId, "Impossible to create the Feature diff with a null id");
        return this;
    }

    public DiffRequest addIncludeComparator(String includeComparator) {
        includeComparators.add(requireNonNull(includeComparator, "A null include comparator id is not valid"));
        return this;
    }

    public Set<String> getIncludeComparators() {
        return includeComparators;
    }

    public DiffRequest addExcludeComparator(String excludeComparator) {
        excludeComparators.add(requireNonNull(excludeComparator, "A null exclude comparator id is not valid"));
        return this;
    }

    public Set<String> getExcludeComparators() {
        return excludeComparators;
    }

}
