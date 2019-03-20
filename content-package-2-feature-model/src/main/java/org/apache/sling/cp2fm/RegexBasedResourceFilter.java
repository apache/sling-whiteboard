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
package org.apache.sling.cp2fm;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class RegexBasedResourceFilter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final List<Pattern> patterns = new LinkedList<>();

    public void addFilteringPattern(String filteringPattern) {
        patterns.add(Pattern.compile(filteringPattern));
    }

    public boolean isFilteredOut(String path) {
        for (Pattern pattern : patterns) {
            logger.debug("Checking if path '{}' matches against '{}' pattern...", path, pattern);

            if (pattern.matcher(path).matches()) {
                logger.debug("Path '{}' matches against '{}' pattern.", path, pattern);

                return true;
            } else {
                logger.debug("Path '{}' does not matches against '{}' pattern.", path, pattern);
            }
        }

        logger.debug("Path '{}' does not match against any configured pattern.", path);

        return false;
    }

}
