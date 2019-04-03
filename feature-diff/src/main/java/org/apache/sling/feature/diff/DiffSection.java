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

import java.util.LinkedList;
import java.util.List;

public final class DiffSection {

    private final List<String> added = new LinkedList<>();

    private final List<String> removed = new LinkedList<>();

    private final List<UpdatedItem<?>> updatedItems = new LinkedList<>();

    private final List<DiffSection> updates = new LinkedList<>();

    private final String id;

    protected DiffSection(String id) {
        this.id = requireNonNull(id, "A Diff section can not be declared with a null id.");
    }

    public String getId() {
        return id;
    }

    protected void markAdded(String item) {
        String checkedItem = requireNonNull(item, "Null item can not be added in the 'added' section");
        added.add(checkedItem);
    }

    public Iterable<String> getAdded() {
        return added;
    }

    protected void markRemoved(String item) {
        String checkedItem = requireNonNull(item, "Null item can not be added in the 'removed' section");
        removed.add(checkedItem);
    }

    public Iterable<String> getRemoved() {
        return removed;
    }

    protected <T> void markItemUpdated(String id, T previous, T current) {
        T checkedPrevious = previous;
        T checkedCurrent = current;
        updatedItems.add(new UpdatedItem<T>(id, checkedPrevious, checkedCurrent));
    }

    public Iterable<UpdatedItem<?>> getUpdatedItems() {
        return updatedItems;
    }

    protected void markUpdated(DiffSection diffSection) {
        DiffSection checkedSection = requireNonNull(diffSection);
        updates.add(checkedSection);
    }

    public Iterable<DiffSection> getUpdates() {
        return updates;
    }

    public boolean isEmpty() {
        return added.isEmpty()
                && removed.isEmpty()
                && updatedItems.isEmpty()
                && updates.isEmpty();
    }

}
