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
package org.apache.sling.commons.batch.provided;

import java.util.function.Supplier;

import org.apache.sling.commons.batch.Batch;
import org.apache.sling.commons.batch.ExecutionStrategy;
import org.apache.sling.commons.batch.Operation;
import org.apache.sling.commons.batch.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FinalizeOnceStrategy implements ExecutionStrategy {

    private static final Logger log = LoggerFactory.getLogger(FinalizeOnceStrategy.class);

    private final Supplier<Result> finalizer;
    private final Runnable resetter;

    public FinalizeOnceStrategy(Supplier<Result> finalizer, Runnable resetter) {
        this.finalizer = finalizer;
        this.resetter = resetter;
    }

    @Override
    public Result execute(Batch batch) {
        boolean succeeded = batch.getOperations().stream().filter(op -> op.getStatus() == Operation.STATUS.NOT_STARTED)
                .map(op -> {
                    try {
                        return op.execute();
                    } catch (Exception e) {
                        log.error("Uncaught exception executing operation: {}", op, e);
                        return Result.failed("Uncaught exception executing operation: " + op, e);
                    }
                }).allMatch(r -> r.getStatus() == Result.STATUS.SUCCEEDED);
        if (succeeded) {
            try {
                return finalizer.get();
            } catch (Exception e) {
                log.error("Uncaught exception calling finalizer", e);
                return Result.failed("Uncaught exception calling finalizer", e);
            }
        } else {
            log.warn("Not all operations succeeded, strategy failed");
            return Result.failed("Not all operations succeeded");
        }
    }

    @Override
    public void reset(Batch batch) {
        batch.getOperations().stream().forEach(Operation::reset);
        resetter.run();
    }

}
