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
package org.apache.sling.uca.impl;

import javassist.bytecode.Descriptor;

/**
 * Sets timeouts for HTTP calls done using <em>Apache HttpComponents Client 4.x</em>
 * 
 * <p>It inserts two calls to <tt>org.apache.http.client.config.RequestConfig$Builder</tt> that set default
 * values for <tt>connectTimeout</tt> and <tt>socketTimeout</tt>.</p>
 */
public class HttpClient4TimeoutTransformer extends UpdateFieldsInConstructorTimeoutTransformer {

    private static final String REQUEST_CONFIG_BUILDER_CLASS_NAME = Descriptor.toJvmName("org.apache.http.client.config.RequestConfig$Builder");
    
    public HttpClient4TimeoutTransformer(long connectTimeoutMillis, long readTimeoutMillis, AgentInfo agentInfoMBean) {
        super(REQUEST_CONFIG_BUILDER_CLASS_NAME, "connectTimeout", "socketTimeout",
            connectTimeoutMillis, readTimeoutMillis, agentInfoMBean);
    }
}
