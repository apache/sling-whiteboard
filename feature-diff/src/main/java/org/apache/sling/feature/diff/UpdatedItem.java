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

import java.util.Objects;

public final class UpdatedItem<T> {

    private final String id;

    private final T previous;

    private final T current;

    protected UpdatedItem(String id, T previous, T current) {
        this.id = requireNonNull(id);
        this.previous = previous;
        this.current = current;
    }

    public String getId() {
        return id;
    }

    public T getPrevious() {
        return previous;
    }

    public T getCurrent() {
        return current;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        UpdatedItem<?> other = (UpdatedItem<?>) obj;
        return Objects.equals(id, other.getId())
                && Objects.equals(previous, other.getPrevious())
                && Objects.equals(current, other.getCurrent());
    }

    @Override
    public String toString() {
        return "UpdatedItem [id=" + id + ", previous=" + previous + ", current=" + current + "]";
    }

}
