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
package org.apache.sling.mdresource.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.testing.mock.osgi.MapUtil;
import org.apache.sling.testing.mock.osgi.context.AbstractContextPlugin;
import org.apache.sling.testing.mock.sling.context.SlingContextImpl;

public class RegisterMarkdownResourcePlugin extends AbstractContextPlugin<SlingContextImpl> {
    private final Map<String, Object> props;

    public RegisterMarkdownResourcePlugin(Object... props) {
        this.props = MapUtil.toMap(props);
    }

    @Override
    public void beforeSetUp(SlingContextImpl context) throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("provider.file", "src/test/resources");
        config.put("provider.root", "/md-test");
        config.putAll(props);
        context.registerInjectActivateService(new MarkdownResourceProvider(), config);
    }
};