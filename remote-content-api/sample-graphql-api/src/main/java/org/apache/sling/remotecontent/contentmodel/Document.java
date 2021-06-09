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

package org.apache.sling.remotecontent.contentmodel;

import org.apache.sling.api.resource.Resource;

/** Base class for folders and documents */
public class Document extends ContentItem {
    private Backstage backstage;

    public Document(Resource r) {
        super(r);
    }

    public Object getBody() {
        /*
        @Reference(target="(" + DocumentTree.TARGET_TYPE + "=map)")
        private DocumentTree mappingTarget;

        @Reference
        private DocumentAggregator documentAggregator;
        
        // Use the aggregator to build the body
        if(mappingTarget != null) {
            final DocumentTree.DocumentNode body = mappingTarget.newDocumentNode();
            final DocumentAggregator.Options opt = new DocumentAggregator.Options(false, new UrlBuilderStub());
            documentAggregator.aggregate(r, body, opt);
            body.close();
            data.put("body", body.adaptTo(Map.class));
        }
        */
        return "This will be the document body, created using the document aggregator or document-speficic services";
    }

    public Backstage getBackstage() {
        if(backstage == null) {
            backstage = new Backstage();
        }
        return backstage;
    }
}