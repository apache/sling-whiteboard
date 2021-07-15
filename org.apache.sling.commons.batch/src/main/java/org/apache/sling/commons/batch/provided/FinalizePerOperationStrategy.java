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

public class FinalizePerOperationStrategy implements ExecutionStrategy {

    private static final Logger log = LoggerFactory.getLogger(FinalizePerOperationStrategy.class);

    private final Supplier<Result> finalizer;
    private final Runnable resetter;

    public FinalizePerOperationStrategy(Supplier<Result> finalizer, Runnable resetter) {
        this.finalizer = finalizer;
        this.resetter = resetter;
    }

    @Override
    public Result execute(Batch batch) {
        batch.getOperations().stream().filter(op -> op.getStatus() == Operation.STATUS.NOT_STARTED).forEach(op -> {
            try {
                executeOperation(op);
            } catch (Exception e) {
                log.error("Uncaught exception executing operation: {}", op, e);
                op.setStatus(Operation.STATUS.FAILED);
                this.resetter.run();
            }
        });
        return Result.succeeded();
    }

    private void executeOperation(Operation op) {
        Result opResult = op.execute();
        log.debug("Retrived result {} from operation: {}", opResult, op);
        if (opResult.getStatus() == Result.STATUS.SUCCEEDED) {
            log.debug("Finalizing operation");
            Result result = finalizer.get();
            if (result.getStatus() != Result.STATUS.SUCCEEDED) {
                log.warn("Finalizing operation failed: {}", result);
                op.setStatus(Operation.STATUS.FAILED);
                this.resetter.run();
            } else {
                log.debug("Finalizing operation succeeded: {}", result);
            }
        } else {
            log.warn("Executing operation {} failed: {}", op, opResult);
            this.resetter.run();
        }
    }

    @Override
    public void reset(Batch batch) {
        resetter.run();
    }

}
