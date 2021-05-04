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

package org.apache.sling.documentmapper.impl;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.documentmapper.api.DocumentMapper;
import org.apache.sling.documentmapper.api.MappingTarget;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = DocumentMapper.class, property = { DocumentMapper.ROLE + "=api" })
public class ApiDocumentMapper implements DocumentMapper {

    @Reference(target="(" + org.apache.sling.documentmapper.api.DocumentMapper.ROLE + "=navigation)")
    private DocumentMapper navMapper;

    @Reference(target="(" + org.apache.sling.documentmapper.api.DocumentMapper.ROLE + "=content)")
    private DocumentMapper DocumentMapper;

    @Override
    public void map(@NotNull Resource r, @NotNull MappingTarget.TargetNode dest, UrlBuilder urlb) {
        final MappingTarget.TargetNode n = dest.addChild("navigation");
        navMapper.map(r, n, urlb);

        final MappingTarget.TargetNode c = dest.addChild("content");
        DocumentMapper.map(r, c, urlb);
    }
}