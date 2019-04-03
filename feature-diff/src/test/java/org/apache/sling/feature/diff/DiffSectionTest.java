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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DiffSectionTest {

    @Test(expected = NullPointerException.class)
    public void requiresValidId() {
        new DiffSection(null);
    }

    @Test
    public void emptyCheck() {
        DiffSection emptyDiff = new DiffSection("empty");
        assertTrue(emptyDiff.isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void unacceptedNullAdded() {
        new DiffSection("added").markAdded(null);
    }

    @Test
    public void validAddedField() {
        String expectedAddedValue = "sample";
        DiffSection addedDiff = new DiffSection("added");
        addedDiff.markAdded(expectedAddedValue);

        assertFalse(addedDiff.isEmpty());
        assertEquals(addedDiff.getAdded().iterator().next(), expectedAddedValue);
    }

    @Test(expected = NullPointerException.class)
    public void unacceptedNullRemoved() {
        new DiffSection("removed").markRemoved(null);
    }

    @Test
    public void validRemovedField() {
        String expectedRemovedValue = "removed";
        DiffSection removedDiff = new DiffSection("removed");
        removedDiff.markRemoved(expectedRemovedValue);

        assertFalse(removedDiff.isEmpty());
        assertEquals(removedDiff.getRemoved().iterator().next(), expectedRemovedValue);
    }

    @Test(expected = NullPointerException.class)
    public void unacceptedNullUpdatedId() {
        new DiffSection("updated").markItemUpdated(null, null, null);
    }

    @Test
    public void validItemUpdated() {
        UpdatedItem<String> expectedUpdatedItem = new UpdatedItem<String>("expected", "previous", "current");

        DiffSection updatedDiff = new DiffSection("updated");
        updatedDiff.markItemUpdated(expectedUpdatedItem.getId(), expectedUpdatedItem.getPrevious(), expectedUpdatedItem.getCurrent());

        assertFalse(updatedDiff.isEmpty());
        assertEquals(updatedDiff.getUpdatedItems().iterator().next(), expectedUpdatedItem);
    }

    @Test(expected = NullPointerException.class)
    public void unacceptedNullUpdated() {
        new DiffSection("updated").markUpdated(null);
    }

    @Test
    public void validUpdatedSubDiff() {
        DiffSection mainDiff = new DiffSection("main");
        DiffSection childDiff = new DiffSection("child");
        mainDiff.markUpdated(childDiff);

        assertFalse(mainDiff.isEmpty());
        assertEquals(mainDiff.getUpdates().iterator().next(), childDiff);
    }

}
