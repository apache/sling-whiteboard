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

/**
 * Execution strategies define how a batch gets executed.
 */
public interface ExecutionStrategy {

    /**
     * Executes the batch of operations and returns a result indicating the status
     * of the batch
     * 
     * @param batch the batch of operations to execute
     * @return the result of executing the batch
     */
    Result execute(Batch batch);

    /**
     * Reset the batch and / or underlying system to it's initial state. Note the
     * meaning of resetting may vary depending on the strategy being executed.
     * 
     * @param batch the batch to reset
     */
    void reset(Batch batch);
}
