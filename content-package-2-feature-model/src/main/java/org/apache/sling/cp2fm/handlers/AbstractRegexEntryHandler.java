/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.cp2fm.handlers;

import java.util.regex.Pattern;

import org.apache.sling.cp2fm.spi.EntryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractRegexEntryHandler implements EntryHandler {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final Pattern pattern;

    public AbstractRegexEntryHandler(String regex) {
        pattern = Pattern.compile(regex);
    }

    @Override
    public final boolean matches(String path) {
        return pattern.matcher(path).matches();
    }

    protected final Pattern getPattern() {
        return pattern;
    }

}
