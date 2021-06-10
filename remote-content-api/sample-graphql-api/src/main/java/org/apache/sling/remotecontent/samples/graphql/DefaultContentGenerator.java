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

package org.apache.sling.remotecontent.samples.graphql;

import java.util.Collections;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.documentaggregator.api.DocumentAggregator;
import org.apache.sling.documentaggregator.api.DocumentTree;
import org.apache.sling.remotecontent.contentmodel.ContentGenerator;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = ContentGenerator.class)
public class DefaultContentGenerator implements ContentGenerator {

    public static final String BODY = "body";

    @Reference(target="(" + DocumentTree.TARGET_TYPE + "=map)")
    private DocumentTree mappingTarget;

    @Reference
    private DocumentAggregator documentAggregator;

    @Override
    public String getSourceInfo(Resource r, String name) {
        return String.format("%s for '%s'", getClass().getName(), name);
    }

    @Override
    public Object getContent(Resource r, String name) {
        if(!BODY.equals(name)) {
            return Collections.singletonMap("nocontent", String.format("No content available for '%s'", name));
        }

        final DocumentTree.DocumentNode body = mappingTarget.newDocumentNode();
        final DocumentAggregator.Options opt = new DocumentAggregator.Options(false, new UrlBuilderStub());
        documentAggregator.aggregate(r, body, opt);
        body.close();
        return body.adaptTo(Map.class);
    }

}