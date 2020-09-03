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

package org.apache.sling.remotecontentapi.hardcodedfirstshot;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.remotecontentapi.resourceconverter.ResourceConverter;

class ContentProcessor implements JsonProcessor {
    static class ConverterContext implements ResourceConverter.Context {
        private final PipelineContext pc;

        ConverterContext(PipelineContext pc) {
            this.pc = pc;
        }

        @Override
        public String getUrlForPath(String path, boolean includeApiSelectorsAndExtension) {
            if(includeApiSelectorsAndExtension) {
                return pc.pathToUrl(path);
            } else {
                return pc.pathToUrlNoJsonExtension(path);
            }
        }

        @Override
        public String getRelativePath(Resource r) {
            return r.getPath().substring(pc.resource.getPath().length());
        }
    }

    @Override
    public void process(PipelineContext pc) {
        pc.content.addAll(new ResourceConverter(pc.resource, new ConverterContext(pc)).getJson());
    }
}