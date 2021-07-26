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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.apache.sling.thumbnails.Transformation;
import org.apache.sling.thumbnails.TransformationHandlerConfig;

@Model(adaptables = Resource.class, adapters = Transformation.class)
public class TransformationImpl implements Transformation {

    private final List<?> handlers;
    private final String name;
    private final String path;

    @JsonCreator
    public TransformationImpl(@JsonProperty("handlers") List<?> handlers) {
        this.handlers = (List<?>) handlers;
        this.name = null;
        this.path = null;
    }

    @Inject
    public TransformationImpl(@ChildResource @Named("handlers") List<TransformationHandlerConfig> handlers,
            @ValueMapValue @Named("name") String name, @Self Resource resource) {
        this.handlers = handlers;
        this.name = name;
        this.path = resource.getPath();
    }

    @Override
    public List<TransformationHandlerConfig> getHandlers() {
        return (List<TransformationHandlerConfig>) handlers;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getPath() {
        return this.path;
    }

}
