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

/**
 * Basic information about a {@link Throwable} that was recorded in a file
 */
class RecordedThrowable {
    
    static RecordedThrowable fromLine(String line) {
        line = line.replace(AgentIT.EXCEPTION_MARKER, "");

        String className = line.substring(0, line.indexOf(':'));
        String message = line.substring(line.indexOf(':') + 2); // ignore ':' and leading ' '

        return new RecordedThrowable(className, message);
    }
    
    String className;
    String message;

    public RecordedThrowable(String className, String message) {
        this.className = className;
        this.message = message;
    }
}