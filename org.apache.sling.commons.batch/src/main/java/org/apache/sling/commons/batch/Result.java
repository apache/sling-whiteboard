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
 * Represents a result of an operation of an atomic operation.
 */
public class Result {

    public enum STATUS {
        SUCCEEDED, FAILED;
    }

    private final Throwable cause;
    private final String message;
    private final STATUS status;

    private Result(STATUS status, String message, Throwable cause) {
        this.cause = cause;
        this.message = message;
        this.status = status;
    }

    public static Result succeeded() {
        return new Result(STATUS.SUCCEEDED, null, null);
    }

    public static Result succeeded(String message) {
        return new Result(STATUS.SUCCEEDED, message, null);
    }

    public static Result failed(String message) {
        return new Result(STATUS.FAILED, message, null);
    }

    public static Result failed(String message, Throwable cause) {
        return new Result(STATUS.FAILED, message, cause);
    }

    public Throwable getCause() {
        return this.cause;
    }

    public String getMessage() {
        return this.message;
    }

    public STATUS getStatus() {
        return this.status;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Result [cause=" + cause + ", message=" + message + ", status=" + status + "]";
    }

}