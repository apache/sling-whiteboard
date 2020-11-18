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

package org.apache.sling.contentmapper.impl;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.contentmapper.ContentMapper;
import org.apache.sling.contentmapper.MappingTarget;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = ContentMapper.class, property = { ContentMapper.ROLE + "=api" })
public class ApiContentMapper implements ContentMapper {

    @Reference(target="(" + ContentMapper.ROLE + "=navigation)")
    private ContentMapper navMapper;

    @Reference(target="(" + ContentMapper.ROLE + "=content)")
    private ContentMapper contentMapper;

    @Override
    public void map(@NotNull Resource r, @NotNull MappingTarget.TargetNode dest, UrlBuilder urlb) {
        final MappingTarget.TargetNode n = dest.addChild("navigation");
        navMapper.map(r, n, urlb);

        final MappingTarget.TargetNode c = dest.addChild("content");
        contentMapper.map(r, c, urlb);
    }
}