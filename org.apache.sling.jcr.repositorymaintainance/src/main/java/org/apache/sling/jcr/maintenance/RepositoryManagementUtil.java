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
package org.apache.sling.jcr.maintenance;

import java.util.Arrays;

import javax.management.openmbean.CompositeData;

import org.apache.jackrabbit.oak.api.jmx.RepositoryManagementMBean.StatusCode;

/**
 * Utilities for interacting with the RepositoryManagementMBean
 * 
 * @see org.apache.jackrabbit.oak.api.jmx.RepositoryManagementMBean
 */
public class RepositoryManagementUtil {

    private RepositoryManagementUtil() {
    }

    public static boolean isRunning(CompositeData status) {
        return StatusCode.RUNNING == getStatusCode(status);
    }

    public static boolean isValid(CompositeData status) {
        StatusCode statusCode = getStatusCode(status);
        return statusCode != StatusCode.UNAVAILABLE && statusCode != StatusCode.FAILED;
    }

    public static StatusCode getStatusCode(CompositeData status) {
        int c = ((Integer) status.get("code"));
        return Arrays.stream(StatusCode.values()).filter(sc -> sc.ordinal() == c).findFirst().orElse(StatusCode.NONE);
    }

    public static String getMessage(CompositeData status) {
        return ((String) status.get("message"));
    }

}
