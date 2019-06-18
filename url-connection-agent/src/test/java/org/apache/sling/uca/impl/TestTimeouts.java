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

import java.time.Duration;
import java.util.Objects;

/**
 * Data class for holding information about various timeouts set in the tests
 */
class TestTimeouts {

    Duration executionTimeout = Duration.ofSeconds(AgentIT.EXECUTION_TIMEOUT_SECONDS);
    Duration agentConnectTimeout = Duration.ofSeconds(AgentIT.CONNECT_TIMEOUT_SECONDS);
    Duration agentReadTimeout = Duration.ofSeconds(AgentIT.READ_TIMEOUT_SECONDS);
    Duration clientConnectTimeout = Duration.ZERO;
    Duration clientReadTimeout = Duration.ZERO;
    
    public static TestTimeouts DEFAULT = new TestTimeouts();
    
    static class Builder {
        private TestTimeouts timeouts = new TestTimeouts();
        
        public TestTimeouts.Builder executionTimeout(Duration duration) {
            timeouts.executionTimeout = Objects.requireNonNull(duration);
            return this;
        }

        public TestTimeouts.Builder agentTimeouts(Duration connectTimeout, Duration readTimeout) {
            timeouts.agentConnectTimeout = Objects.requireNonNull(connectTimeout);
            timeouts.agentReadTimeout = Objects.requireNonNull(readTimeout);
            return this;
        }
        
        public TestTimeouts.Builder clientTimeouts(Duration connectTimeout, Duration readTimeout) {
            timeouts.clientConnectTimeout = Objects.requireNonNull(connectTimeout);
            timeouts.clientReadTimeout = Objects.requireNonNull(readTimeout);
            return this;
        }
        
        public TestTimeouts build() {
            return timeouts;
        }
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + ": execution " + executionTimeout + ", agent: " + agentConnectTimeout + "/" + agentReadTimeout + ", client : " + clientConnectTimeout + "/" + clientReadTimeout;
    }
}