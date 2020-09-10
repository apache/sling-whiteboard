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

package org.apache.sling.remotecontentapi.take5;

import java.util.function.Predicate;

import org.apache.sling.api.resource.Resource;

class ResourceRules {
    final Predicate<Resource> matcher;
    final ResourceProcessor navigationProcessor;
    final ResourceProcessor contentProcessor;

    private ResourceRules(Predicate<Resource> matcher, ResourceProcessor navigationProcessor, ResourceProcessor contentProcessor) {
        this.matcher = matcher;
        this.navigationProcessor = navigationProcessor;
        this.contentProcessor = contentProcessor;      
    }

    static class Builder {
        private final Predicate<Resource> matcher;
        private ResourceProcessor navigationProcessor;
        private ResourceProcessor contentProcessor;

        Builder(Predicate<Resource> matcher) {
            this.matcher = matcher;
        }

        Builder withNavigationProcessor(ResourceProcessor rp) {
            navigationProcessor = rp;
            return this;
        }

        Builder withContentProcessor(ResourceProcessor rp) {
            contentProcessor = rp;
            return this;
        }

        ResourceRules build() {
            return new ResourceRules(matcher, navigationProcessor, contentProcessor);
        }

    }

    static Builder builder(Predicate<Resource> matcher) {
        return new Builder(matcher);
    }
}