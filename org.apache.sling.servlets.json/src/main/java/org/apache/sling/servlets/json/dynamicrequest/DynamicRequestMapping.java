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

import java.lang.reflect.Method;

import org.apache.sling.servlets.json.annotations.RequestHandler;

import com.hrakaroo.glob.GlobPattern;
import com.hrakaroo.glob.MatchingEngine;

public class DynamicRequestMapping {

    private final RequestHandler handler;
    private final Method method;
    private final MatchingEngine matcher;

    /**
     * @param handler
     * @param method
     */
    public DynamicRequestMapping(RequestHandler handler, Method method) {
        this.handler = handler;
        this.method = method;
        matcher = GlobPattern.compile(handler.path());
    }

    /**
     * @return the handler
     */
    public RequestHandler getHandler() {
        return handler;
    }

    /**
     * @return the method
     */
    public Method getMethod() {
        return method;
    }

    /**
     * If true, this DynamicRequestMapping instance matches the request path
     *
     * @param requestPath the request path for this request
     * @return whether or not the request path matches
     */
    public boolean matches(String requestPath) {
        return matcher.matches(requestPath);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "DynamicRequestMapping [handler=" + handler + ", method=" + method + "]";
    }

}
