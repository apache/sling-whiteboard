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
package org.apache.sling.testing.osgi.unit;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.List;

@Target({ ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(OSGiSupportImpl.class)
public @interface OSGiSupport {

    Enablement verbose() default Enablement.INHERIT;

    LogService logService() default LogService.INHERIT;

    Class<?>[] additionalClasses() default {};

    String[] additionalBundles() default {};

    enum Enablement {
        ENABLED(true),
        DISABLED(false),
        INHERIT(false);

        private final boolean internalBoolean;

        Enablement(boolean internalBoolean) {
            this.internalBoolean = internalBoolean;
        }

        public boolean isEnabled() {
            return internalBoolean;
        }
    }

    enum LogService {

        /**
         * {@code INHERIT} is the default value. If no other value is inherited,
         * then {@code NONE} is assumed.
         */
        INHERIT(),

        NONE(),

        FELIX_LOGBACK(
                "slf4j.api",
                "ch.qos.logback.classic",
                "ch.qos.logback.core",
                "org.apache.felix.logback"),
        SLING(
                "org.apache.felix.log",
                "org.apache.sling.commons.log",
                "org.apache.sling.commons.logservice");

        private final Collection<String> symbolicNames;

        LogService(String... symbolicNames) {
            this.symbolicNames = List.of(symbolicNames);
        }

        public Collection<String> getSymbolicNames() {
            return symbolicNames;
        }
    }
}

