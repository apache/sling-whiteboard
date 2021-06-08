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

package org.apache.sling.apiplanes.prototype;

import java.util.Arrays;

import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;

// TODO unused - but kept around in case we need it in another context (see README)
public class ExtensionOverrideRequestPathInfo implements RequestPathInfo {

    private RequestPathInfo wrapped;
    private final String replacementExtension;
    private final String [] selectors;
    private final String selectorString;

    ExtensionOverrideRequestPathInfo(RequestPathInfo wrapped, String replacementExtension) {
        this.wrapped = wrapped;
        this.replacementExtension = replacementExtension;
        final String [] originalSelectors = wrapped.getSelectors();
        selectors = new String[originalSelectors.length + 1];
        System.arraycopy(originalSelectors, 0, selectors, 0, originalSelectors.length);
        selectors[selectors.length - 1] = wrapped.getExtension();
        selectorString = String.join(".", Arrays.asList(selectors));
    }

    @Override
    public String getResourcePath() {
        return wrapped.getResourcePath();
    }

    @Override
    public String getExtension() {
        return replacementExtension;
    }

    @Override
    public String getSelectorString() {
        return selectorString;
    }

    @Override
    public String[] getSelectors() {
        return selectors;
    }

    @Override
    public String getSuffix() {
        return wrapped.getSuffix();
    }

    @Override
    public Resource getSuffixResource() {
        return wrapped.getSuffixResource();
    }

}