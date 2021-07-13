/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.thumbnails.internal.models;

import java.util.Map;

import javax.inject.Inject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.thumbnails.TransformationHandlerConfig;

@Model(adaptables = Resource.class, adapters = TransformationHandlerConfig.class)
public class TransformationHandlerConfigImpl implements TransformationHandlerConfig {

    private final String handlerType;
    private final ValueMap properties;

    @Inject
    public TransformationHandlerConfigImpl(@Self Resource resource) {
        this.handlerType = resource.getResourceType();
        this.properties = resource.getValueMap();
    }

    @JsonCreator
    public TransformationHandlerConfigImpl(@JsonProperty("handlerType") String handerType,
            @JsonProperty("properties") Map<String, Object> properties) {
        this.handlerType = handerType;
        this.properties = new ValueMapDecorator(properties);
    }

    @Override
    public String getHandlerType() {
        return handlerType;
    }

    @Override
    public ValueMap getProperties() {
        return this.properties;
    }

}
