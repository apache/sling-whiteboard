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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Batch {

    private Logger log = LoggerFactory.getLogger(Batch.class);

    public static class Builder {

        private List<Operation> operations;

        private List<ExecutionStrategy> strategies;

        private Builder() {
        }

        public static Builder getInstance() {
            return new Builder();
        }

        public Builder addOperation(Operation operation) {
            if (this.operations == null) {
                this.operations = new ArrayList<>();
            }
            this.operations.add(operation);
            return this;
        }

        public Builder setOperations(List<Operation> operations) {
            this.operations = operations;
            return this;
        }

        public Builder addStrategy(ExecutionStrategy strategy) {
            if (this.strategies == null) {
                this.strategies = new ArrayList<>();
            }
            this.strategies.add(strategy);
            return this;
        }

        public Builder setStrategies(List<ExecutionStrategy> strategies) {
            this.strategies = strategies;
            return this;
        }

        public Batch build() {
            return new Batch(operations, strategies);
        }

    }

    enum STATUS {
        COMPLETE, IN_PROGRESS, NOT_STARTED, FAILED;
    }

    private final List<Operation> operations;

    private final List<ExecutionStrategy> strategies;

    private STATUS status;

    private Batch(List<Operation> operations, List<ExecutionStrategy> strategies) {
        this.operations = operations;
        this.strategies = strategies;
        this.status = STATUS.NOT_STARTED;
    }

    public void execute() {
        this.status = STATUS.IN_PROGRESS;
        for (ExecutionStrategy strategy : strategies) {
            try {
                log.debug("Executing strategy: {}", strategy);
                Result result = strategy.execute(this);
                log.debug("Recieved result: {}", result);
                if (result.getStatus() == Result.STATUS.SUCCEEDED) {
                    log.info("Strategy {} executed successfully!", strategy);
                    this.status = STATUS.COMPLETE;
                    return;
                } else {
                    log.warn("Recieved failed response {} from strategy {}", result, strategy);
                    strategy.reset(this);
                }
            } catch (Exception e) {
                log.error("Uncaught exception from strategy: {}", strategy, e);
                strategy.reset(this);
            }
        }

        log.info("No more strategies to try, batch failed");
        this.status = STATUS.FAILED;
    }

    public boolean allOperationsSucceeded() {
        return operations.stream().allMatch(op -> op.getStatus() == Operation.STATUS.SUCCEEDED);
    }

    public List<Operation> getFailedOperations() {
        return operations.stream().filter(op -> op.getStatus() != Operation.STATUS.SUCCEEDED)
                .collect(Collectors.toList());
    }

    public List<Operation> getSucceededOperations() {
        return operations.stream().filter(op -> op.getStatus() == Operation.STATUS.SUCCEEDED)
                .collect(Collectors.toList());
    }

    public STATUS getStatus() {
        return status;
    }

    public List<Operation> getOperations() {
        return this.operations;
    }

    public List<ExecutionStrategy> getStrategies() {
        return this.strategies;
    }

}
