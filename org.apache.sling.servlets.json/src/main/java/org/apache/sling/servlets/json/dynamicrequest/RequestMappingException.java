/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.servlets.json.dynamicrequest;

import org.apache.sling.api.SlingException;

public class RequestMappingException extends SlingException {

    /**
     * Constructs a new RequestMappingException
     */
    protected RequestMappingException() {
        super();
    }

    /**
     * Constructs a new RequestMappingException with the given text
     *
     * @param text the exception text
     */
    protected RequestMappingException(String text) {
        super(text);
    }

    /**
     * Constructs a new RequestMappingException with a cause
     *
     * @param text  the exception text
     * @param cause the root cause
     */
    public RequestMappingException(String text, Throwable cause) {
        super(text, cause);
    }

    /**
     * Constructs a new RequestMappingException with only a cause
     *
     * @param cause the root cause
     */
    protected RequestMappingException(Throwable cause) {
        super(cause);
    }
}
