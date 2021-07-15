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

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.sling.commons.batch.Operation;
import org.apache.sling.commons.batch.Result;

/**
 * An Operation implementation which executes the provided function with the
 * provided value.
 */
public class FunctionalOperation<T> implements Operation {

    private final Function<T, Result> function;
    private final T value;
    private Operation.STATUS status;

    public FunctionalOperation(T value, Function<T, Result> function) {
        this.value = value;
        this.function = function;
        this.status = Operation.STATUS.NOT_STARTED;
    }

    public static <T> List<FunctionalOperation<T>> newOperations(List<T> value, Function<T, Result> function) {
        return value.stream().map(v -> new FunctionalOperation<>(v, function)).collect(Collectors.toList());
    }

    @Override
    public void reset() {
        this.setStatus(Operation.STATUS.NOT_STARTED);
    }

    @Override
    public STATUS getStatus() {
        return this.status;
    }

    @Override
    public Result execute() {
        this.status = STATUS.IN_PROGRESS;
        Result result = function.apply(value);
        this.status = result.getStatus() == Result.STATUS.SUCCEEDED ? STATUS.SUCCEEDED : STATUS.FAILED;
        return result;
    }

    @Override
    public void setStatus(STATUS status) {
        this.status = status;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */

    @Override
    public String toString() {
        return "FunctionalOperation [status=" + status + ", value=" + value + "]";
    }

}
