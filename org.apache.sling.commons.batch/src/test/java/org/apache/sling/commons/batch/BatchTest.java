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
package org.apache.sling.commons.batch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.commons.batch.Batch.Builder;
import org.apache.sling.commons.batch.provided.FinalizeOnceStrategy;
import org.apache.sling.commons.batch.provided.FinalizePerOperationStrategy;
import org.apache.sling.commons.batch.provided.FunctionalOperation;
import org.junit.Test;

public class BatchTest {

    @Test
    public void testBuilder() {

        List<String> messages = new ArrayList<>();
        Builder bob = Batch.Builder.getInstance();
        bob.addOperation(new FunctionalOperation<String>("Hello World", (msg) -> {
            messages.add(msg);
            return Result.succeeded();
        }));

        bob.addStrategy(new FinalizeOnceStrategy(() -> {
            assertEquals(1, messages.size());
            assertEquals("Hello World", messages.get(0));
            return Result.succeeded();
        }, () -> {
        }));

        Batch good = bob.build();

        assertEquals(1, good.getOperations().size());
        assertEquals(1, good.getStrategies().size());

        good.execute();

        assertSame(Batch.STATUS.COMPLETE, good.getStatus());

        assertTrue(good.allOperationsSucceeded());
    }

    @Test
    public void testFallback() {

        List<String> messages = new ArrayList<>();
        Builder bob = Batch.Builder.getInstance();
        bob.addOperation(new FunctionalOperation<String>("Hello World", (msg) -> {
            messages.add(msg);
            return Result.succeeded();
        }));
        bob.addOperation(new FunctionalOperation<String>(null, (msg) -> {
            return Result.failed("Because I feel like it!");
        }));
        bob.addOperation(new FunctionalOperation<String>("Goodbye World", (msg) -> {
            messages.add(msg);
            return Result.succeeded();
        }));

        // first we'll add a "SAVE ALL THE THINGS!!!! Strategy"
        bob.addStrategy(new FinalizeOnceStrategy(() -> {
            return Result.succeeded();
        }, () -> {
            messages.clear();
        }));

        // if that fails, we'll fall back to processing each item individually
        bob.addStrategy(new FinalizePerOperationStrategy(() -> {
            return Result.succeeded();
        }, () -> {
        }));

        Batch bad = bob.build();

        assertEquals(3, bad.getOperations().size());
        assertEquals(2, bad.getStrategies().size());

        bad.execute();

        assertSame(Batch.STATUS.COMPLETE, bad.getStatus());

        assertFalse(bad.allOperationsSucceeded());

        assertEquals(2, messages.size());
        assertEquals(2, bad.getSucceededOperations().size());
        assertEquals(1, bad.getFailedOperations().size());
    }

}
