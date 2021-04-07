/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.ddr.core;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceDecorator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import javax.servlet.http.HttpServletRequest;

//AS TODO: We might not need it

//@Component(
//    service = ResourceDecorator.class,
//    immediate = true,
//    name="DDR Decorator"
//)
//@Designate(ocd = DeclarativeDynamicDecoratorService.Configuration.class, factory = false)
public class DeclarativeDynamicDecoratorService
    implements ResourceDecorator
{
    @ObjectClassDefinition(
        name = "Declarative Dynamic Component Resource Manager",
        description = "Configuration of the Dynamic Component Resource Manager")
    public @interface Configuration {
        @AttributeDefinition(
            name = "Path Mappings",
            description = "")
        String[] path_mapping() default "/apps/ddr-dynamic";
    }

    @Override
    public @Nullable Resource decorate(@NotNull Resource resource) {
        return resource;
    }

    @Override
    public @Nullable Resource decorate(@NotNull Resource resource, @NotNull HttpServletRequest request) {
        return this.decorate(resource);
    }
}
