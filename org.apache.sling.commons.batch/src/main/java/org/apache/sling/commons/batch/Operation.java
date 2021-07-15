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
 * An atomic operation to be executed as a part of the batch.
 */
public interface Operation {
    enum STATUS {
        SUCCEEDED, FAILED, IN_PROGRESS, NOT_STARTED
    }

    /**
     * Resets the operation
     */
    void reset();

    /**
     * Gets the status of this operation.
     * 
     * @return the status
     */
    STATUS getStatus();

    /**
     * Executes the operation and returns the result
     */
    Result execute();

    /**
     * Sets the status of this operation.
     * 
     * @return the status
     */
    void setStatus(STATUS status);
}
